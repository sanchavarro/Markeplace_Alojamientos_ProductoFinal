import { API_BASE_URL } from './env';

/**
 * Imágenes de respaldo (arquitectura e interiores) para alojamientos de prueba sin fotos.
 * Se elige una de forma determinista según el id, para que cada tarjeta luzca distinta pero estable.
 */
const u = (id, w = 1400) => `https://images.unsplash.com/photo-${id}?auto=format&fit=crop&w=${w}&q=80`;
export const unsplash = u;

export const IMAGENES_RESPALDO = [
  '1600585154340-be6161a56a0c',
  '1613490493576-7fde63acd811',
  '1600607687939-ce8a6c25118c',
  '1564013799919-ab600027ffc6',
  '1502672260266-1c1ef2d93688',
  '1449158743715-0a90ebb6d2d8',
  '1600566753190-17f0baa2a6c3',
  '1570129477492-45c003edd2be',
  '1505693416388-ac5ce068fe85',
  '1580587771525-78b9dba3b914',
  '1493809842364-78817add7ffb',
  '1613977257363-707ba9348227',
  '1484154218962-a197022b5858',
  '1568605114967-8130f3a36994',
  '1600210492486-724fe5c67fb0',
  '1510798831971-661eb04b3739',
  '1522708323590-d24dbb6b0267',
  '1600596542815-ffad4c1539a9',
  '1540518614846-7eded433c457',
  '1499793983690-e29da59ef1c2',
].map((id) => u(id));

/** Último recurso si incluso la imagen de respaldo falla (sin red): un SVG neutro embebido. */
export const IMAGEN_VACIA =
  'data:image/svg+xml;utf8,' +
  encodeURIComponent(
    '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 400 300"><rect width="400" height="300" fill="#ECECEF"/><path d="M150 170l50-45 50 45v45a8 8 0 0 1-8 8h-22v-36h-40v36h-22a8 8 0 0 1-8-8z" fill="#C7C7CC"/></svg>',
  );

export const respaldoPara = (semilla = 0, desplazamiento = 0) => {
  const n = Math.abs(Number(semilla) || 0) + desplazamiento;
  return IMAGENES_RESPALDO[n % IMAGENES_RESPALDO.length];
};

/**
 * Resuelve la URL final de una imagen del backend:
 *  - URL absoluta (http/https/data/blob) → se usa tal cual (URLs externas o las que el adaptador
 *    de almacenamiento ya devuelve como http://localhost:8080/imagenes/{nombre}).
 *  - "/imagenes/x.jpg" → se concatena con API_BASE_URL.
 *  - "x.jpg" (solo el nombre) → API_BASE_URL + "/imagenes/" + nombre.
 */
export const resolverImagen = (valor) => {
  const url = typeof valor === 'string' ? valor : valor?.url;
  if (!url) return null;
  if (/^(https?:|data:|blob:)/i.test(url)) return url;
  if (url.startsWith('/')) return `${API_BASE_URL}${url}`;
  return `${API_BASE_URL}/imagenes/${encodeURIComponent(url)}`;
};

/** Lista de URLs de un AlojamientoDTO, ordenada por "orden", con respaldo si no hay fotos. */
export const fotosDeAlojamiento = (alojamiento, minimo = 1) => {
  const propias = [...(alojamiento?.imagenes || [])]
    .sort((a, b) => (a.orden ?? 0) - (b.orden ?? 0) || (a.id ?? 0) - (b.id ?? 0))
    .map(resolverImagen)
    .filter(Boolean);
  if (alojamiento?.imagenPrincipal && propias.length === 0) {
    propias.push(resolverImagen(alojamiento.imagenPrincipal));
  }
  if (propias.length >= minimo) return propias;
  const id = alojamiento?.id ?? 0;
  const relleno = [];
  for (let i = propias.length; i < minimo; i += 1) relleno.push(respaldoPara(id, i));
  return [...propias, ...relleno];
};

/** Manejador onError reutilizable: cambia a la imagen de respaldo y, si también falla, al SVG neutro. */
export const alFallarImagen = (semilla) => (evento) => {
  const img = evento.currentTarget;
  if (img.dataset.fallback === '2') return;
  if (img.dataset.fallback === '1') {
    img.dataset.fallback = '2';
    img.src = IMAGEN_VACIA;
    return;
  }
  img.dataset.fallback = '1';
  img.src = respaldoPara(semilla);
};
