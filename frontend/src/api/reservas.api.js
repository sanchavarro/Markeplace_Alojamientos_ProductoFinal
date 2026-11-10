import { http, datos, params } from './client';

/**
 * Épicas E07 y E08 · ReservaRestController y CancelacionRestController.
 *
 * Idempotencia (ADR-03, HU-28): POST /reservas y POST /reservas/{id}/pagar exigen el encabezado
 * "Idempotency-Key" con un UUID. La clave la genera el llamador (hook useIdempotencia) UNA vez por
 * intento del usuario y se reutiliza si la misma solicitud se reintenta (doble clic, red caída).
 *
 * Códigos de respuesta de reservar/pagar: 201 confirmada · 202 enviada al anfitrión ·
 * 402 pago rechazado · 503 pasarela no disponible. En 402/503 el cuerpo sigue siendo un ReservaResponse.
 */
export const reservasApi = {
  /** ReservaRequest { alojamientoId, fechaLlegada, fechaSalida, numHuespedes, serviciosAdicionales, metodoPago } */
  crear: (reserva, claveIdempotencia) =>
    http
      .post(
        '/reservas',
        {
          alojamientoId: reserva.alojamientoId,
          fechaLlegada: reserva.fechaLlegada,
          fechaSalida: reserva.fechaSalida,
          numHuespedes: Number(reserva.numHuespedes),
          serviciosAdicionales: reserva.serviciosAdicionales || [],
          metodoPago: reserva.metodoPago || null,
        },
        { headers: { 'Idempotency-Key': claveIdempotencia }, timeout: 30000 },
      )
      .then((r) => ({ ...r.data, _status: r.status })),

  /** PagarRequest { metodoPago } — para reservas con aprobación ya aprobadas (PENDIENTE_PAGO). */
  pagar: (id, metodoPago, claveIdempotencia) =>
    http
      .post(`/reservas/${id}/pagar`, { metodoPago: metodoPago || null }, { headers: { 'Idempotency-Key': claveIdempotencia }, timeout: 30000 })
      .then((r) => ({ ...r.data, _status: r.status })),

  mias: ({ pagina, tamano } = {}) => datos(http.get('/reservas/mias', { params: params({ pagina, tamano }) })),

  anfitrion: ({ alojamientoId, pagina, tamano } = {}) =>
    datos(http.get('/reservas/anfitrion', { params: params({ alojamientoId, pagina, tamano }) })),

  aprobar: (id) => datos(http.patch(`/reservas/${id}/aprobar`)),
  rechazar: (id) => datos(http.patch(`/reservas/${id}/rechazar`)),

  detalle: (id) => datos(http.get(`/reservas/${id}`)),

  /** HU-33 → CancelacionResponse (porcentajeDevolucion, valorDevolucion, reglaPolitica...). No modifica nada. */
  simularCancelacion: (id) => datos(http.get(`/reservas/${id}/cancelacion/simulacion`)),

  /** HU-32, HU-34, HU-35 · CancelacionRequest { motivo } */
  cancelar: (id, motivo) => datos(http.post(`/reservas/${id}/cancelacion`, { motivo: motivo?.trim() || null })),
};
