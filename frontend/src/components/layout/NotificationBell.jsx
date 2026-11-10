import { useEffect, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import { Bell, CheckCheck } from 'lucide-react';
import { notificacionesApi } from '../../api';
import { useNotificaciones } from '../../context/NotificationsContext';
import { useToast } from '../../context/ToastContext';
import { haceCuanto } from '../../utils/format';
import { Spinner } from '../ui';

/** Campana del navbar con badge de no leídas y vista rápida de las últimas notificaciones (HU-37). */
export default function NotificationBell() {
  const { noLeidas, invalidar, version } = useNotificaciones();
  const toast = useToast();
  const [abierto, setAbierto] = useState(false);
  const [lista, setLista] = useState([]);
  const [cargando, setCargando] = useState(false);
  const contenedor = useRef(null);

  useEffect(() => {
    if (!abierto) return undefined;
    let activo = true;
    setCargando(true);
    notificacionesApi
      .listar({ pagina: 0, tamano: 6 })
      .then((p) => activo && setLista(p?.contenido || []))
      .catch((e) => activo && toast.error(e))
      .finally(() => activo && setCargando(false));
    const fuera = (e) => contenedor.current && !contenedor.current.contains(e.target) && setAbierto(false);
    const esc = (e) => e.key === 'Escape' && setAbierto(false);
    document.addEventListener('mousedown', fuera);
    document.addEventListener('keydown', esc);
    return () => {
      activo = false;
      document.removeEventListener('mousedown', fuera);
      document.removeEventListener('keydown', esc);
    };
  }, [abierto, version, toast]);

  const marcar = async (n) => {
    if (n.leida) return;
    try {
      await notificacionesApi.marcarLeida(n.id);
      setLista((l) => l.map((x) => (x.id === n.id ? { ...x, leida: true } : x)));
      invalidar();
    } catch (e) {
      toast.error(e);
    }
  };

  const marcarTodas = async () => {
    try {
      await notificacionesApi.marcarTodas();
      setLista((l) => l.map((x) => ({ ...x, leida: true })));
      invalidar();
    } catch (e) {
      toast.error(e);
    }
  };

  return (
    <div className="relative" ref={contenedor}>
      <button
        type="button"
        onClick={() => setAbierto((v) => !v)}
        className="relative flex h-10 w-10 items-center justify-center rounded-full text-ink-soft transition hover:bg-black/5 hover:text-ink"
        aria-label={`Notificaciones${noLeidas ? `, ${noLeidas} sin leer` : ''}`}
        aria-expanded={abierto}
      >
        <Bell className="h-[19px] w-[19px]" strokeWidth={1.8} />
        {noLeidas > 0 && (
          <span className="absolute right-1 top-1 flex h-[18px] min-w-[18px] items-center justify-center rounded-full bg-brand px-1 text-[10px] font-semibold text-white ring-2 ring-white">
            {noLeidas > 99 ? '99+' : noLeidas}
          </span>
        )}
      </button>

      {abierto && (
        <div className="glass absolute right-0 top-12 z-50 w-[min(92vw,380px)] animate-rise-in overflow-hidden rounded-3xl border border-white/70 shadow-lift">
          <div className="flex items-center justify-between px-5 pb-2 pt-4">
            <p className="text-[15px] font-semibold">Notificaciones</p>
            {noLeidas > 0 && (
              <button type="button" onClick={marcarTodas} className="flex items-center gap-1 text-xs font-medium text-accent hover:underline">
                <CheckCheck className="h-3.5 w-3.5" /> Marcar todas
              </button>
            )}
          </div>
          <div className="max-h-[360px] overflow-y-auto px-2 pb-2">
            {cargando && lista.length === 0 ? (
              <div className="flex justify-center py-8 text-ink-muted">
                <Spinner />
              </div>
            ) : lista.length === 0 ? (
              <p className="px-3 py-8 text-center text-sm text-ink-muted">No tienes notificaciones todavía.</p>
            ) : (
              lista.map((n) => (
                <button
                  key={n.id}
                  type="button"
                  onClick={() => marcar(n)}
                  className={`flex w-full gap-3 rounded-2xl px-3 py-3 text-left transition hover:bg-black/[0.04] ${n.leida ? '' : 'bg-accent-soft/60'}`}
                >
                  <span className={`mt-1.5 h-2 w-2 shrink-0 rounded-full ${n.leida ? 'bg-transparent' : 'bg-accent'}`} />
                  <span className="min-w-0">
                    <span className="line-clamp-3 block text-[13.5px] leading-snug text-ink">{n.mensaje}</span>
                    <span className="mt-1 block text-[11.5px] text-ink-faint">{haceCuanto(n.creadaEn)}</span>
                  </span>
                </button>
              ))
            )}
          </div>
          <Link
            to="/notificaciones"
            onClick={() => setAbierto(false)}
            className="block border-t border-line/50 py-3 text-center text-[13px] font-medium text-accent hover:bg-black/[0.03]"
          >
            Ver todas
          </Link>
        </div>
      )}
    </div>
  );
}
