/**
 * Almacenamiento de la sesión (JWT de acceso, ADR-05).
 *
 * Decisiones de seguridad:
 *  - El backend entrega el JWT en el cuerpo de POST /auth/login (no usa cookies httpOnly), así que el
 *    frontend debe guardarlo. Se usa sessionStorage (no localStorage): el token muere al cerrar la pestaña
 *    y no queda persistido en disco entre sesiones del navegador.
 *  - Se guarda junto con su fecha de expiración (expiraEnSegundos = 3600) para descartarlo localmente
 *    antes de enviarlo vencido.
 *  - Solo se persiste lo mínimo: token, tipo y expiración. Los datos del usuario se piden siempre a
 *    GET /usuarios/perfil (fuente de verdad del rol y estado).
 *  - Ningún dato sensible (clave) se guarda nunca en el navegador.
 */
const CLAVE = 'mkt.sesion';
const MARGEN_MS = 15 * 1000;

let enMemoria = null;

const leerAlmacen = () => {
  try {
    const crudo = window.sessionStorage.getItem(CLAVE);
    return crudo ? JSON.parse(crudo) : null;
  } catch {
    return null;
  }
};

export const guardarSesion = ({ token, tipo = 'Bearer', expiraEnSegundos = 3600 }) => {
  const sesion = {
    token,
    tipo,
    expiraEn: Date.now() + Number(expiraEnSegundos || 3600) * 1000,
  };
  enMemoria = sesion;
  try {
    window.sessionStorage.setItem(CLAVE, JSON.stringify(sesion));
  } catch {
    /* Navegación privada estricta: la sesión vive solo en memoria. */
  }
  return sesion;
};

export const leerSesion = () => {
  const sesion = enMemoria || leerAlmacen();
  if (!sesion?.token) return null;
  if (Date.now() >= sesion.expiraEn - MARGEN_MS) {
    limpiarSesion();
    return null;
  }
  enMemoria = sesion;
  return sesion;
};

export const tokenVigente = () => leerSesion()?.token || null;

export const msParaExpirar = () => {
  const sesion = leerSesion();
  return sesion ? Math.max(0, sesion.expiraEn - Date.now() - MARGEN_MS) : 0;
};

export const limpiarSesion = () => {
  enMemoria = null;
  try {
    window.sessionStorage.removeItem(CLAVE);
  } catch {
    /* sin almacenamiento disponible */
  }
};

/** Eventos globales de sesión, escuchados por AuthContext. */
export const EVENTO_SESION_EXPIRADA = 'mkt:sesion-expirada';

export const notificarSesionExpirada = (mensaje) => {
  window.dispatchEvent(new CustomEvent(EVENTO_SESION_EXPIRADA, { detail: { mensaje } }));
};
