import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from 'react';
import { notificacionesApi } from '../api';
import { useAuth } from './AuthContext';
import { NOTIFICACIONES_INTERVALO_MS } from '../utils/env';

/**
 * Contador de notificaciones no leídas (HU-37) compartido entre la campana del navbar y la página
 * de notificaciones. Se refresca periódicamente, al volver a la pestaña y tras cada acción.
 */
const NotificationsContext = createContext(null);

export function NotificationsProvider({ children }) {
  const { autenticado } = useAuth();
  const [noLeidas, setNoLeidas] = useState(0);
  const [version, setVersion] = useState(0);
  const enCurso = useRef(false);

  const refrescar = useCallback(async () => {
    if (!autenticado || enCurso.current) return;
    enCurso.current = true;
    try {
      const r = await notificacionesApi.noLeidas();
      setNoLeidas(Number(r?.noLeidas) || 0);
    } catch {
      /* el interceptor ya gestiona 401; otros errores no deben romper el navbar */
    } finally {
      enCurso.current = false;
    }
  }, [autenticado]);

  useEffect(() => {
    if (!autenticado) {
      setNoLeidas(0);
      return undefined;
    }
    refrescar();
    const intervalo = setInterval(() => {
      if (document.visibilityState === 'visible') refrescar();
    }, NOTIFICACIONES_INTERVALO_MS);
    const alVolver = () => document.visibilityState === 'visible' && refrescar();
    document.addEventListener('visibilitychange', alVolver);
    return () => {
      clearInterval(intervalo);
      document.removeEventListener('visibilitychange', alVolver);
    };
  }, [autenticado, refrescar]);

  /** Notifica a las listas abiertas que hubo cambios (marcar leída, nuevas acciones). */
  const invalidar = useCallback(() => {
    setVersion((v) => v + 1);
    refrescar();
  }, [refrescar]);

  const valor = useMemo(() => ({ noLeidas, setNoLeidas, refrescar, invalidar, version }), [noLeidas, refrescar, invalidar, version]);

  return <NotificationsContext.Provider value={valor}>{children}</NotificationsContext.Provider>;
}

export const useNotificaciones = () => {
  const ctx = useContext(NotificationsContext);
  if (!ctx) throw new Error('useNotificaciones debe usarse dentro de <NotificationsProvider>.');
  return ctx;
};
