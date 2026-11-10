/**
 * Valores de dominio tomados de co.edu.unbosque.utils.Constantes (backend).
 * Mantenerlos sincronizados con el backend.
 */

export const ROLES = Object.freeze({
  HUESPED: 'HUESPED',
  ANFITRION: 'ANFITRION',
  ADMIN: 'ADMINISTRADOR',
});

export const ROL_LABEL = {
  HUESPED: 'Huésped',
  ANFITRION: 'Anfitrión',
  ADMINISTRADOR: 'Administrador',
};

/** Ruta de inicio de cada rol después de iniciar sesión. */
export const HOME_POR_ROL = {
  HUESPED: '/',
  ANFITRION: '/anfitrion',
  ADMINISTRADOR: '/admin',
};

export const TIPOS_ALOJAMIENTO = ['CASA', 'APARTAMENTO', 'FINCA', 'CABANA', 'HABITACION', 'GLAMPING'];

export const TIPO_LABEL = {
  CASA: 'Casa',
  APARTAMENTO: 'Apartamento',
  FINCA: 'Finca',
  CABANA: 'Cabaña',
  HABITACION: 'Habitación',
  GLAMPING: 'Glamping',
};

export const MODOS_RESERVA = ['INMEDIATA', 'APROBACION'];

export const MODO_LABEL = {
  INMEDIATA: 'Reserva inmediata',
  APROBACION: 'Con aprobación del anfitrión',
};

export const ESTADO_ALOJAMIENTO = {
  BORRADOR: { label: 'Borrador', tone: 'neutral' },
  PENDIENTE_REVISION: { label: 'En revisión', tone: 'warning' },
  ACTIVA: { label: 'Activa', tone: 'success' },
  PAUSADA: { label: 'Pausada', tone: 'info' },
  BLOQUEADA: { label: 'Bloqueada', tone: 'danger' },
};

export const ESTADO_RESERVA = {
  EN_REVISION: { label: 'Esperando al anfitrión', tone: 'warning' },
  PENDIENTE_PAGO: { label: 'Pendiente de pago', tone: 'warning' },
  CONFIRMADA: { label: 'Confirmada', tone: 'success' },
  PAGO_FALLIDO: { label: 'Pago fallido', tone: 'danger' },
  CANCELADA: { label: 'Cancelada', tone: 'neutral' },
  COMPLETADA: { label: 'Completada', tone: 'info' },
};

/** CancelacionServiceImpl.CANCELABLES: solo antes de la fecha de llegada. */
export const ESTADOS_CANCELABLES = ['CONFIRMADA', 'PENDIENTE_PAGO', 'EN_REVISION'];

export const ESTADO_USUARIO = {
  ACTIVO: { label: 'Activo', tone: 'success' },
  INACTIVO: { label: 'Inactivo', tone: 'neutral' },
  RESTRINGIDO: { label: 'Restringido', tone: 'danger' },
};

export const ESTADO_PERIODO = {
  DISPONIBLE: { label: 'Disponible', tone: 'success' },
  BLOQUEADO: { label: 'No disponible', tone: 'neutral' },
  RESERVADO: { label: 'Reservado', tone: 'danger' },
};

export const ESTADO_EXPORTACION = {
  PENDIENTE: { label: 'En cola', tone: 'neutral' },
  PROCESANDO: { label: 'Generando', tone: 'warning' },
  LISTO: { label: 'Listo', tone: 'success' },
  ERROR: { label: 'Error', tone: 'danger' },
};

/** Acciones del anfitrión sobre su publicación (PATCH /alojamientos/{id}/estado). */
export const ACCIONES_ANFITRION = {
  BORRADOR: [{ accion: 'ENVIAR_REVISION', label: 'Enviar a revisión' }],
  ACTIVA: [{ accion: 'PAUSAR', label: 'Pausar publicación' }],
  PAUSADA: [{ accion: 'ACTIVAR', label: 'Reactivar publicación' }],
  PENDIENTE_REVISION: [],
  BLOQUEADA: [],
};

/** Acciones del administrador sobre publicaciones (PATCH /admin/publicaciones/{id}). */
export const ACCIONES_ADMIN_PUBLICACION = ['APROBAR', 'PAUSAR', 'BLOQUEAR', 'DESBLOQUEAR'];

/** Claves de configuración de tarifas (tabla configuracion_tarifa). */
export const TARIFA_LABEL = {
  TARIFA_SERVICIO_PORCENTAJE: { label: 'Tarifa de servicio', unidad: '%' },
  DESCUENTO_SEMANAL_PORCENTAJE: { label: 'Descuento semanal', unidad: '%' },
  NOCHES_DESCUENTO_SEMANAL: { label: 'Noches para el descuento semanal', unidad: 'noches' },
};

/** Métodos de pago de prueba aceptados por el backend (FabricaPagoSimulado / Stripe test). */
export const METODOS_PAGO_PRUEBA = [
  { valor: 'pm_card_visa', label: 'Visa de prueba · aprobada' },
  { valor: 'pm_card_mastercard', label: 'Mastercard de prueba · aprobada' },
  { valor: 'pm_card_chargeDeclined', label: 'Tarjeta rechazada (simulación)' },
];

export const NOCHES_MAXIMAS = 30;

export const MAX_COMPARAR = 3;

export const PAGINA_TAMANO = 20;
