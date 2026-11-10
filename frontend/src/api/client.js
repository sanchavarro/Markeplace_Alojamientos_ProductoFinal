import axios from 'axios';
import { API_BASE_URL } from '../utils/env';
import { leerSesion, limpiarSesion, notificarSesionExpirada } from '../utils/session';

/**
 * Cliente HTTP único de la aplicación.
 *
 * Interceptor de solicitud: agrega "Authorization: Bearer <token>" si hay una sesión vigente.
 * Interceptor de respuesta: normaliza los errores del backend (GlobalExceptionHandler responde
 * {"mensaje": "..."} y, en validaciones, {"mensaje": "...", "errores": {campo: mensaje}}) y gestiona 401/403.
 */
export const http = axios.create({
  baseURL: API_BASE_URL,
  timeout: 20000,
  headers: { Accept: 'application/json' },
});

/** Rutas en las que un 401 significa "credenciales incorrectas", no "sesión vencida". */
const RUTAS_CREDENCIALES = ['/auth/login', '/auth/reactivar', '/auth/restablecer', '/auth/logout', '/auth/verificar-correo'];

const esRutaCredenciales = (url = '') => RUTAS_CREDENCIALES.some((r) => url.startsWith(r));

http.interceptors.request.use((config) => {
  const sesion = leerSesion();
  if (sesion?.token && !config.sinToken) {
    config.headers = config.headers || {};
    config.headers.Authorization = `${sesion.tipo || 'Bearer'} ${sesion.token}`;
  }
  return config;
});

/** Error de la API con la información que necesitan las vistas. */
export class ApiError extends Error {
  constructor({ status, mensaje, errores, datos, codigo }) {
    super(mensaje);
    this.name = 'ApiError';
    this.status = status;
    this.errores = errores || {};
    this.datos = datos;
    this.codigo = codigo;
  }
}

const MENSAJES_POR_ESTADO = {
  400: 'Hay datos inválidos en la solicitud.',
  401: 'Debes iniciar sesión para continuar.',
  402: 'El pago fue rechazado.',
  403: 'Tu rol no tiene permiso para este recurso.',
  404: 'No encontramos lo que buscas.',
  409: 'La operación entra en conflicto con el estado actual.',
  413: 'El archivo supera el tamaño máximo de 5 MB.',
  422: 'La solicitud no cumple una regla de negocio.',
  423: 'La cuenta está bloqueada.',
  503: 'El servicio no está disponible en este momento. Intenta de nuevo.',
};

const leerCuerpoBlob = async (data) => {
  if (typeof Blob !== 'undefined' && data instanceof Blob) {
    try {
      return JSON.parse(await data.text());
    } catch {
      return null;
    }
  }
  return data;
};

export const normalizarError = async (error) => {
  if (error instanceof ApiError) return error;
  if (axios.isCancel?.(error)) return new ApiError({ status: 0, mensaje: 'Solicitud cancelada.', codigo: 'CANCELADA' });
  if (!error?.response) {
    const timeout = error?.code === 'ECONNABORTED';
    return new ApiError({
      status: 0,
      codigo: timeout ? 'TIMEOUT' : 'RED',
      mensaje: timeout
        ? 'El servidor tardó demasiado en responder. Intenta de nuevo.'
        : `No hay conexión con el servidor (${API_BASE_URL}). Verifica que el backend esté en ejecución.`,
    });
  }
  const { status } = error.response;
  const data = await leerCuerpoBlob(error.response.data);
  const mensaje = (data && typeof data === 'object' && data.mensaje) || MENSAJES_POR_ESTADO[status] || 'Ocurrió un error inesperado.';
  return new ApiError({ status, mensaje, errores: data?.errores, datos: data });
};

http.interceptors.response.use(
  (respuesta) => respuesta,
  async (error) => {
    const config = error?.config || {};
    const status = error?.response?.status;

    // 401 con un token adjunto fuera de las rutas de credenciales: la sesión venció, se revocó
    // (logout en otra pestaña) o la cuenta fue restringida (HU-44: se verifica en cada solicitud).
    if (status === 401 && config.headers?.Authorization && !esRutaCredenciales(config.url)) {
      const cuerpo = await leerCuerpoBlob(error.response.data);
      // Algunos 401 son de negocio y no de sesión (p. ej. "La clave actual no es correcta." en PUT /usuarios/perfil).
      if (config.error401Negocio instanceof RegExp && config.error401Negocio.test(cuerpo?.mensaje || '')) {
        throw new ApiError({ status, mensaje: cuerpo.mensaje, datos: cuerpo });
      }
      limpiarSesion();
      notificarSesionExpirada(cuerpo?.mensaje || 'Tu sesión terminó. Inicia sesión de nuevo.');
      // El filtro JWT rechaza tokens inválidos incluso en rutas públicas: se reintenta una vez sin token
      // para que el catálogo, el detalle o la búsqueda sigan funcionando.
      if (!config._reintentoSinToken) {
        const reintento = { ...config, _reintentoSinToken: true, sinToken: true, headers: { ...config.headers } };
        delete reintento.headers.Authorization;
        try {
          return await http.request(reintento);
        } catch (e) {
          throw await normalizarError(e);
        }
      }
    }
    throw await normalizarError(error);
  },
);

/** Genera el valor del encabezado Idempotency-Key (ADR-03, HU-28). */
export const nuevaClaveIdempotencia = () => {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') return crypto.randomUUID();
  // Respaldo RFC 4122 v4 con crypto.getRandomValues (contextos no seguros, p. ej. http://IP).
  const bytes = new Uint8Array(16);
  crypto.getRandomValues(bytes);
  bytes[6] = (bytes[6] & 0x0f) | 0x40;
  bytes[8] = (bytes[8] & 0x3f) | 0x80;
  const hex = [...bytes].map((b) => b.toString(16).padStart(2, '0')).join('');
  return `${hex.slice(0, 8)}-${hex.slice(8, 12)}-${hex.slice(12, 16)}-${hex.slice(16, 20)}-${hex.slice(20)}`;
};

/** Quita parámetros vacíos para no enviar "?ciudad=&tipo=" al backend. */
export const params = (objeto = {}) =>
  Object.fromEntries(
    Object.entries(objeto).filter(([, v]) => v !== undefined && v !== null && v !== '' && !(Array.isArray(v) && v.length === 0)),
  );

/** Serializa listas como ?servicios=1&servicios=2 (formato que Spring enlaza a List<Long>). */
export const serializarParams = (objeto) => {
  const busqueda = new URLSearchParams();
  Object.entries(objeto || {}).forEach(([clave, valor]) => {
    if (valor === undefined || valor === null || valor === '') return;
    if (Array.isArray(valor)) valor.forEach((v) => busqueda.append(clave, v));
    else busqueda.append(clave, valor);
  });
  return busqueda.toString();
};

http.defaults.paramsSerializer = { serialize: serializarParams };

export const datos = (promesa) => promesa.then((r) => r.data);

/** Extrae el nombre del archivo de Content-Disposition (expuesto por CORS en SecurityConfig). */
export const nombreDesdeDisposition = (encabezado, porDefecto) => {
  if (!encabezado) return porDefecto;
  const utf = /filename\*=UTF-8''([^;]+)/i.exec(encabezado);
  if (utf) return decodeURIComponent(utf[1]);
  const simple = /filename="?([^";]+)"?/i.exec(encabezado);
  return simple ? simple[1] : porDefecto;
};

/** Descarga un Blob en el navegador. */
export const descargarBlob = (blob, nombre) => {
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = nombre;
  document.body.appendChild(a);
  a.click();
  a.remove();
  setTimeout(() => URL.revokeObjectURL(url), 1500);
};
