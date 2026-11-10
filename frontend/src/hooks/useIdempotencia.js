import { useCallback, useMemo, useRef } from 'react';
import { nuevaClaveIdempotencia } from '../api';

/**
 * Clave de idempotencia por intento del usuario (HU-28, ADR-03).
 *
 *  - obtener(): devuelve la clave del intento actual; si no hay, genera una con crypto.randomUUID().
 *  - Si la solicitud falla por red o timeout (no sabemos si el servidor la procesó), la clave SE CONSERVA:
 *    el siguiente clic reenvía la misma clave y el backend devuelve el resultado original sin cobrar dos veces.
 *  - Cuando el servidor da una respuesta definitiva (éxito, 402, 409, 422...) se llama a reiniciar() para que
 *    un nuevo intento consciente del usuario use una clave nueva.
 */
export function useIdempotencia() {
  const clave = useRef(null);

  const obtener = useCallback(() => {
    if (!clave.current) clave.current = nuevaClaveIdempotencia();
    return clave.current;
  }, []);

  const reiniciar = useCallback(() => {
    clave.current = null;
  }, []);

  /** ¿El error deja la clave "abierta" (resultado incierto)? */
  const esIncierto = useCallback((error) => !error?.status || error.status === 0 || error.status >= 500, []);

  // Objeto estable entre renders: puede usarse como dependencia de efectos sin reiniciar la clave.
  return useMemo(() => ({ obtener, reiniciar, esIncierto }), [obtener, reiniciar, esIncierto]);
}
