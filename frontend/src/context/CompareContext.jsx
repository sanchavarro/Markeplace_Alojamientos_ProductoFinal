import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { MAX_COMPARAR } from '../utils/constants';

/**
 * Selección de alojamientos para comparar (HU-22: hasta 3).
 * Se guarda en sessionStorage para conservarla al navegar entre el catálogo y el detalle.
 */
const CompareContext = createContext(null);
const CLAVE = 'mkt.comparar';

const leer = () => {
  try {
    const lista = JSON.parse(window.sessionStorage.getItem(CLAVE) || '[]');
    return Array.isArray(lista) ? lista.slice(0, MAX_COMPARAR) : [];
  } catch {
    return [];
  }
};

export function CompareProvider({ children }) {
  const [seleccion, setSeleccion] = useState(leer);

  useEffect(() => {
    try {
      window.sessionStorage.setItem(CLAVE, JSON.stringify(seleccion));
    } catch {
      /* sin almacenamiento */
    }
  }, [seleccion]);

  const estaSeleccionado = useCallback((id) => seleccion.some((a) => a.id === id), [seleccion]);

  /** Devuelve false si ya hay 3 seleccionados. */
  const alternar = useCallback(
    (alojamiento) => {
      if (seleccion.some((a) => a.id === alojamiento.id)) {
        setSeleccion((l) => l.filter((a) => a.id !== alojamiento.id));
        return true;
      }
      if (seleccion.length >= MAX_COMPARAR) return false;
      const resumen = {
        id: alojamiento.id,
        titulo: alojamiento.titulo,
        ciudad: alojamiento.ciudad,
        imagenPrincipal: alojamiento.imagenPrincipal || alojamiento.imagenes?.[0]?.url || null,
      };
      setSeleccion((l) => [...l, resumen]);
      return true;
    },
    [seleccion],
  );

  const quitar = useCallback((id) => setSeleccion((l) => l.filter((a) => a.id !== id)), []);
  const vaciar = useCallback(() => setSeleccion([]), []);

  const valor = useMemo(
    () => ({ seleccion, estaSeleccionado, alternar, quitar, vaciar, lleno: seleccion.length >= MAX_COMPARAR }),
    [seleccion, estaSeleccionado, alternar, quitar, vaciar],
  );

  return <CompareContext.Provider value={valor}>{children}</CompareContext.Provider>;
}

export const useCompare = () => {
  const ctx = useContext(CompareContext);
  if (!ctx) throw new Error('useCompare debe usarse dentro de <CompareProvider>.');
  return ctx;
};
