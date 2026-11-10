import { useCallback, useEffect, useRef, useState } from 'react';

/**
 * Ejecuta una función asíncrona al montar y cuando cambian sus dependencias.
 * Devuelve { data, error, cargando, recargar, setData }.
 * Evita actualizaciones de estado tras desmontar y descarta respuestas viejas (condiciones de carrera).
 */
export function useAsync(fn, deps = [], { inmediato = true, inicial = null } = {}) {
  const [data, setData] = useState(inicial);
  const [error, setError] = useState(null);
  const [cargando, setCargando] = useState(inmediato);
  const ultimo = useRef(0);
  const montado = useRef(true);

  useEffect(() => {
    montado.current = true;
    return () => {
      montado.current = false;
    };
  }, []);

  // eslint-disable-next-line react-hooks/exhaustive-deps
  const ejecutar = useCallback(fn, deps);

  const recargar = useCallback(async () => {
    const id = ++ultimo.current;
    setCargando(true);
    setError(null);
    try {
      const r = await ejecutar();
      if (montado.current && id === ultimo.current) setData(r);
      return r;
    } catch (e) {
      if (montado.current && id === ultimo.current) setError(e);
      return undefined;
    } finally {
      if (montado.current && id === ultimo.current) setCargando(false);
    }
  }, [ejecutar]);

  useEffect(() => {
    if (inmediato) recargar();
  }, [recargar, inmediato]);

  return { data, error, cargando, recargar, setData };
}
