import { http, datos, params } from './client';

/**
 * Épicas E03 y E04 · AlojamientoRestController (/alojamientos).
 * AlojamientoRequest { titulo, descripcion, reglas, ciudad, direccion, tipo, capacidad, precioNoche,
 *                      tarifaLimpieza, modoReserva, politicaId, servicios: List<Long> }
 */
const aRequest = (f) => ({
  titulo: f.titulo.trim(),
  descripcion: f.descripcion.trim(),
  reglas: f.reglas?.trim() || null,
  ciudad: f.ciudad.trim(),
  direccion: f.direccion.trim(),
  tipo: f.tipo,
  capacidad: Number(f.capacidad),
  precioNoche: Number(f.precioNoche),
  tarifaLimpieza: f.tarifaLimpieza === '' || f.tarifaLimpieza === null || f.tarifaLimpieza === undefined ? 0 : Number(f.tarifaLimpieza),
  modoReserva: f.modoReserva,
  politicaId: Number(f.politicaId),
  servicios: (f.servicios || []).map(Number),
});

export const alojamientosApi = {
  // ── Público ────────────────────────────────────────────────
  /** HU-15 → AlojamientoDTO (imagenes, servicios, serviciosAdicionales, politica, promedioCalificacion...). */
  detalle: (id) => datos(http.get(`/alojamientos/${id}`)),

  /** HU-17 → List<PeriodoDisponibilidad> { id, alojamientoId, reservaId, fechaInicio, fechaFin, estado }. */
  calendario: (id, { desde, hasta } = {}) => datos(http.get(`/alojamientos/${id}/calendario`, { params: params({ desde, hasta }) })),

  /** HU-40 → { alojamientoId, promedio, total, resenas: PaginaResponse<Resena> } */
  resenas: (id, { pagina, tamano } = {}) => datos(http.get(`/alojamientos/${id}/resenas`, { params: params({ pagina, tamano }) })),

  // ── Anfitrión ──────────────────────────────────────────────
  mios: ({ pagina, tamano } = {}) => datos(http.get('/alojamientos/mios', { params: params({ pagina, tamano }) })),
  crear: (formulario) => datos(http.post('/alojamientos', aRequest(formulario))),
  editar: (id, formulario) => datos(http.put(`/alojamientos/${id}`, aRequest(formulario))),

  /** HU-09/HU-10: CambioEstadoRequest { accion: ENVIAR_REVISION | PAUSAR | ACTIVAR, motivo? } */
  cambiarEstado: (id, accion, motivo) => datos(http.patch(`/alojamientos/${id}/estado`, { accion, motivo: motivo || null })),

  /** ImagenRequest { url, orden } */
  agregarImagenUrl: (id, { url, orden }) =>
    datos(http.post(`/alojamientos/${id}/imagenes`, { url: url.trim(), orden: orden ?? null })),

  /** multipart/form-data con el campo "archivo" (y "orden" opcional). */
  subirImagen: (id, archivo, { orden, onProgreso } = {}) => {
    const formulario = new FormData();
    formulario.append('archivo', archivo);
    if (orden !== undefined && orden !== null) formulario.append('orden', String(orden));
    return datos(
      http.post(`/alojamientos/${id}/imagenes/archivo`, formulario, {
        timeout: 60000,
        onUploadProgress: (e) => onProgreso?.(e.total ? Math.round((e.loaded * 100) / e.total) : 0),
      }),
    );
  },

  eliminarImagen: (id, imagenId) => datos(http.delete(`/alojamientos/${id}/imagenes/${imagenId}`)),

  /** ServicioAdicionalRequest { nombre, precio } (HU-13) */
  agregarServicioAdicional: (id, { nombre, precio }) =>
    datos(http.post(`/alojamientos/${id}/servicios-adicionales`, { nombre: nombre.trim(), precio: Number(precio) })),
  eliminarServicioAdicional: (id, servicioId) => datos(http.delete(`/alojamientos/${id}/servicios-adicionales/${servicioId}`)),

  /** PeriodoRequest { fechaInicio, fechaFin, estado: DISPONIBLE | BLOQUEADO } (HU-16) */
  crearPeriodo: (id, periodo) => datos(http.post(`/alojamientos/${id}/periodos`, periodo)),
  editarPeriodo: (id, periodoId, periodo) => datos(http.put(`/alojamientos/${id}/periodos/${periodoId}`, periodo)),
  eliminarPeriodo: (id, periodoId) => datos(http.delete(`/alojamientos/${id}/periodos/${periodoId}`)),
};
