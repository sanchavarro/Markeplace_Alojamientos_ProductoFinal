import { useEffect, useState } from 'react';
import { alojamientosApi } from '../api';
import { fotosDeAlojamiento, respaldoPara } from '../utils/images';

/**
 * ReservaResponse no trae la foto del alojamiento, así que se consulta GET /alojamientos/{id} una vez por
 * alojamiento (caché en memoria por pestaña). Si el alojamiento ya no es público se usa una imagen de respaldo.
 */
const cache = new Map();

const pedir = (id) => {
  if (!cache.has(id)) {
    cache.set(
      id,
      alojamientosApi
        .detalle(id)
        .then((a) => ({ foto: fotosDeAlojamiento(a)[0], ciudad: a.ciudad, titulo: a.titulo }))
        .catch(() => ({ foto: respaldoPara(id), ciudad: null, titulo: null })),
    );
  }
  return cache.get(id);
};

export function useMiniaturas(ids = []) {
  const [mapa, setMapa] = useState({});
  const clave = [...new Set(ids.filter(Boolean))].sort((a, b) => a - b).join(',');

  useEffect(() => {
    let activo = true;
    const unicos = clave ? clave.split(',').map(Number) : [];
    Promise.all(unicos.map((id) => pedir(id).then((info) => [id, info]))).then((pares) => {
      if (activo) setMapa(Object.fromEntries(pares));
    });
    return () => {
      activo = false;
    };
  }, [clave]);

  return mapa;
}
