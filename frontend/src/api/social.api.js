import { http, datos, params } from './client';

/** Épica E10 · ResenaRestController. ResenaRequest { reservaId, calificacion (1..5), comentario } (HU-38, HU-39) */
export const resenasApi = {
  crear: ({ reservaId, calificacion, comentario }) =>
    datos(http.post('/resenas', { reservaId, calificacion: Number(calificacion), comentario: comentario?.trim() || null })),
};

/** Épica E09 · NotificacionRestController (HU-37). Notificacion { id, usuarioId, tipo, mensaje, leida, creadaEn } */
export const notificacionesApi = {
  listar: ({ pagina, tamano } = {}) => datos(http.get('/notificaciones', { params: params({ pagina, tamano }) })),
  /** → { noLeidas: number } */
  noLeidas: () => datos(http.get('/notificaciones/no-leidas')),
  marcarLeida: (id) => datos(http.patch(`/notificaciones/${id}/leida`)),
  marcarTodas: () => datos(http.patch('/notificaciones/leidas')),
};
