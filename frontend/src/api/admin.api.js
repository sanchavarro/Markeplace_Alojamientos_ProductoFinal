import { http, datos, params, nombreDesdeDisposition } from './client';

/**
 * Épicas E11 a E14 · AdminRestController, ReporteRestController, TarifaRestController, SaludRestController.
 * Todas exigen rol ADMINISTRADOR salvo GET /salud (público).
 */
export const adminApi = {
  // ── Moderación (HU-42, HU-43) ──
  pendientes: ({ pagina, tamano } = {}) => datos(http.get('/admin/publicaciones/pendientes', { params: params({ pagina, tamano }) })),
  /** accion: APROBAR | PAUSAR | BLOQUEAR | DESBLOQUEAR */
  gestionarPublicacion: (id, accion, motivo) => datos(http.patch(`/admin/publicaciones/${id}`, { accion, motivo: motivo?.trim() || null })),

  // ── Usuarios (HU-44) ──
  usuarios: ({ estado, pagina, tamano } = {}) => datos(http.get('/admin/usuarios', { params: params({ estado, pagina, tamano }) })),
  /** accion: RESTRINGIR | REACTIVAR */
  gestionarUsuario: (id, accion, motivo) => datos(http.patch(`/admin/usuarios/${id}`, { accion, motivo: motivo?.trim() || null })),

  // ── Reseñas (HU-41) ──
  ocultarResena: (id, motivo) => datos(http.patch(`/admin/resenas/${id}/ocultar`, { motivo: motivo?.trim() || null })),
  mostrarResena: (id, motivo) => datos(http.patch(`/admin/resenas/${id}/mostrar`, { motivo: motivo?.trim() || null })),

  // ── Auditoría (HU-45) ──
  auditoria: ({ entidad, usuarioId, accion, desde, hasta, pagina, tamano } = {}) =>
    datos(http.get('/admin/auditoria', { params: params({ entidad, usuarioId, accion, desde, hasta, pagina, tamano }) })),

  // ── Reservas en revisión manual (HU-42) ──
  reservasRevision: ({ pagina, tamano } = {}) => datos(http.get('/admin/reservas/revision', { params: params({ pagina, tamano }) })),
  /** accion: MANTENER | CANCELAR */
  resolverRevision: (id, accion, motivo) => datos(http.patch(`/admin/reservas/${id}/revision`, { accion, motivo: motivo?.trim() || null })),

  /** POST /admin/tareas → { reservasCompletadas, pagosPendientesVencidos, clavesIdempotenciaEliminadas, reembolsosReintentados } */
  ejecutarTareas: () => datos(http.post('/admin/tareas')),

  // ── Tarifas (HU-24) ──
  actualizarTarifa: (clave, valor) => datos(http.put(`/tarifas/${encodeURIComponent(clave)}`, { valor: Number(valor) })),
};

export const reportesApi = {
  /** HU-46 → { usuarios, publicaciones, reservas, ingresosAprobadosCOP, ciudadesConMasDemanda[] } */
  dashboard: () => datos(http.get('/reportes/dashboard')),

  /** HU-47 descarga inmediata: GET /reportes/reservas?formato=csv|pdf|excel&desde&hasta → binario */
  descargarReservas: async ({ formato, desde, hasta }) => {
    const r = await http.get('/reportes/reservas', { params: params({ formato, desde, hasta }), responseType: 'blob', timeout: 60000 });
    return { blob: r.data, nombre: nombreDesdeDisposition(r.headers['content-disposition'], `reservas.${formato}`) };
  },

  /** HU-47 en segundo plano: ExportacionRequest { formato, desde, hasta } → 202 ReporteExportacion */
  solicitarExportacion: ({ formato, desde, hasta }) =>
    datos(http.post('/reportes/exportaciones', { formato, desde: desde || null, hasta: hasta || null })),
  exportaciones: ({ pagina, tamano } = {}) => datos(http.get('/reportes/exportaciones', { params: params({ pagina, tamano }) })),
  exportacion: (id) => datos(http.get(`/reportes/exportaciones/${id}`)),
  descargarExportacion: async (id, porDefecto) => {
    const r = await http.get(`/reportes/exportaciones/${id}/archivo`, { responseType: 'blob', timeout: 60000 });
    return { blob: r.data, nombre: nombreDesdeDisposition(r.headers['content-disposition'], porDefecto || `reporte-${id}`) };
  },
};

export const saludApi = {
  /** GET /salud (público). 503 con el mismo cuerpo si la base de datos está caída. */
  estado: () =>
    http
      .get('/salud', { validateStatus: (s) => s === 200 || s === 503, timeout: 8000 })
      .then((r) => ({ ...r.data, _status: r.status })),
  metricas: () => datos(http.get('/salud/metricas')),
};
