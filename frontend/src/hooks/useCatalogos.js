import { useEffect, useState } from 'react';
import { catalogoApi } from '../api';

/**
 * Catálogos estáticos del backend (políticas de cancelación y servicios), cacheados en memoria
 * durante la vida de la pestaña para no repetir GET /catalogo/* en cada vista.
 */
let cache = null;
let promesa = null;

const cargar = () => {
  if (cache) return Promise.resolve(cache);
  if (!promesa) {
    promesa = Promise.all([catalogoApi.politicas(), catalogoApi.servicios()])
      .then(([politicas, servicios]) => {
        cache = { politicas: politicas || [], servicios: servicios || [] };
        return cache;
      })
      .catch((e) => {
        promesa = null;
        throw e;
      });
  }
  return promesa;
};

export function useCatalogos() {
  const [estado, setEstado] = useState({ politicas: cache?.politicas || [], servicios: cache?.servicios || [], cargando: !cache, error: null });

  useEffect(() => {
    let activo = true;
    if (cache) return undefined;
    cargar()
      .then((c) => activo && setEstado({ ...c, cargando: false, error: null }))
      .catch((error) => activo && setEstado((s) => ({ ...s, cargando: false, error })));
    return () => {
      activo = false;
    };
  }, []);

  return estado;
}
