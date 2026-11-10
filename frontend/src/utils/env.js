/**
 * Configuración leída de las variables de entorno de Vite (.env).
 * Se normaliza la URL base para que nunca termine en "/".
 */
const limpiarUrl = (url) => (url || '').trim().replace(/\/+$/, '');

const numero = (valor, porDefecto) => {
  const n = Number(valor);
  return Number.isFinite(n) && n > 0 ? n : porDefecto;
};

export const API_BASE_URL = limpiarUrl(import.meta.env.VITE_API_BASE_URL) || 'http://localhost:8080';

export const NOTIFICACIONES_INTERVALO_MS = numero(import.meta.env.VITE_NOTIFICACIONES_INTERVALO_MS, 30000);

export const EXPORTACIONES_INTERVALO_MS = numero(import.meta.env.VITE_EXPORTACIONES_INTERVALO_MS, 4000);

export const METODO_PAGO_DEFECTO = (import.meta.env.VITE_METODO_PAGO_DEFECTO || 'pm_card_visa').trim();

/** Datos de contacto de la marca (footer, ayuda). */
export const CORREO_CONTACTO = (import.meta.env.VITE_CORREO_CONTACTO || 'contacto@marketplace.com').trim();
export const TELEFONO_CONTACTO = (import.meta.env.VITE_TELEFONO_CONTACTO || '+57 601 555 0100').trim();

/** Redes sociales de la marca. Si una variable queda vacía, el ícono no se muestra. */
export const REDES_SOCIALES = [
  { red: 'instagram', etiqueta: 'Instagram', url: import.meta.env.VITE_URL_INSTAGRAM ?? 'https://www.instagram.com/' },
  { red: 'facebook', etiqueta: 'Facebook', url: import.meta.env.VITE_URL_FACEBOOK ?? 'https://www.facebook.com/' },
  { red: 'x', etiqueta: 'X', url: import.meta.env.VITE_URL_X ?? 'https://x.com/' },
  { red: 'tiktok', etiqueta: 'TikTok', url: import.meta.env.VITE_URL_TIKTOK ?? 'https://www.tiktok.com/' },
  { red: 'youtube', etiqueta: 'YouTube', url: import.meta.env.VITE_URL_YOUTUBE ?? 'https://www.youtube.com/' },
].filter((r) => r.url);
