const COP = new Intl.NumberFormat('es-CO', {
  style: 'currency',
  currency: 'COP',
  maximumFractionDigits: 0,
});

const NUM = new Intl.NumberFormat('es-CO');

/** Formatea un valor monetario (BigDecimal serializado como número o texto). */
export const dinero = (valor, moneda = 'COP') => {
  if (valor === null || valor === undefined || valor === '') return '—';
  const n = Number(valor);
  if (!Number.isFinite(n)) return '—';
  if (moneda && moneda !== 'COP') {
    return new Intl.NumberFormat('es-CO', { style: 'currency', currency: moneda, maximumFractionDigits: 0 }).format(n);
  }
  return COP.format(n);
};

export const numero = (valor) => (valor === null || valor === undefined ? '—' : NUM.format(Number(valor)));

/**
 * Convierte "2026-11-09" (LocalDate) en una fecha local sin desfase de zona horaria.
 * new Date("2026-11-09") se interpreta en UTC y en Bogotá mostraría el día anterior.
 */
export const parseFecha = (texto) => {
  if (!texto) return null;
  if (texto instanceof Date) return texto;
  const soloFecha = /^\d{4}-\d{2}-\d{2}$/.test(texto);
  if (soloFecha) {
    const [a, m, d] = texto.split('-').map(Number);
    return new Date(a, m - 1, d);
  }
  const f = new Date(texto);
  return Number.isNaN(f.getTime()) ? null : f;
};

/** Date → "YYYY-MM-DD" (formato ISO que espera @DateTimeFormat(iso = DATE)). */
export const aIsoFecha = (fecha) => {
  if (!fecha) return '';
  const f = fecha instanceof Date ? fecha : parseFecha(fecha);
  if (!f) return '';
  const m = String(f.getMonth() + 1).padStart(2, '0');
  const d = String(f.getDate()).padStart(2, '0');
  return `${f.getFullYear()}-${m}-${d}`;
};

export const hoyIso = () => aIsoFecha(new Date());

export const sumarDias = (iso, dias) => {
  const f = parseFecha(iso) || new Date();
  const r = new Date(f.getFullYear(), f.getMonth(), f.getDate() + dias);
  return aIsoFecha(r);
};

export const nochesEntre = (llegada, salida) => {
  const a = parseFecha(llegada);
  const b = parseFecha(salida);
  if (!a || !b) return 0;
  return Math.round((b - a) / 86400000);
};

export const fechaCorta = (texto) => {
  const f = parseFecha(texto);
  if (!f) return '—';
  return f.toLocaleDateString('es-CO', { day: 'numeric', month: 'short' });
};

export const fechaLarga = (texto) => {
  const f = parseFecha(texto);
  if (!f) return '—';
  return f.toLocaleDateString('es-CO', { day: 'numeric', month: 'long', year: 'numeric' });
};

export const fechaHora = (texto) => {
  const f = parseFecha(texto);
  if (!f) return '—';
  return f.toLocaleString('es-CO', {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
};

export const rangoFechas = (llegada, salida) => {
  if (!llegada || !salida) return '';
  return `${fechaCorta(llegada)} – ${fechaCorta(salida)}`;
};

export const haceCuanto = (texto) => {
  const f = parseFecha(texto);
  if (!f) return '';
  const seg = Math.round((Date.now() - f.getTime()) / 1000);
  if (seg < 60) return 'hace un momento';
  const min = Math.round(seg / 60);
  if (min < 60) return `hace ${min} min`;
  const h = Math.round(min / 60);
  if (h < 24) return `hace ${h} h`;
  const d = Math.round(h / 24);
  if (d < 30) return `hace ${d} d`;
  return fechaCorta(texto);
};

export const plural = (n, singular, pluralTexto) => `${n} ${n === 1 ? singular : pluralTexto || `${singular}s`}`;

export const iniciales = (nombre = '') =>
  nombre
    .trim()
    .split(/\s+/)
    .slice(0, 2)
    .map((p) => p[0]?.toUpperCase() || '')
    .join('') || '·';

export const calificacion = (valor) => (valor === null || valor === undefined ? null : Number(valor).toFixed(1));
