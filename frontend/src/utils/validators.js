/**
 * Validaciones del lado del cliente, espejo de las anotaciones jakarta.validation de los DTO del backend.
 * El backend sigue siendo la fuente de verdad (HU-05, HU-12, HU-23: "se valida en el servidor").
 */
import { hoyIso, nochesEntre } from './format';
import { NOCHES_MAXIMAS, TIPOS_ALOJAMIENTO } from './constants';

const CORREO_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const TELEFONO_RE = /^[0-9+ ]{7,20}$/;
const URL_RE = /^https?:\/\/.+/;

export const vacio = (v) => v === null || v === undefined || String(v).trim() === '';

const limpiar = (errores) => Object.fromEntries(Object.entries(errores).filter(([, v]) => v));

/** RegistroRequest (HU-01). */
export const validarRegistro = ({ nombre, correo, clave, confirmar, rol }) =>
  limpiar({
    nombre: vacio(nombre) ? 'El nombre es obligatorio.' : nombre.trim().length > 120 && 'El nombre admite máximo 120 caracteres.',
    correo: vacio(correo)
      ? 'El correo es obligatorio.'
      : !CORREO_RE.test(correo.trim())
        ? 'El correo debe tener un formato válido.'
        : correo.trim().length > 150 && 'El correo admite máximo 150 caracteres.',
    clave: vacio(clave)
      ? 'La clave es obligatoria.'
      : (clave.length < 8 || clave.length > 64) && 'La clave debe tener entre 8 y 64 caracteres.',
    confirmar: clave !== confirmar && 'Las claves no coinciden.',
    rol: !['HUESPED', 'ANFITRION'].includes(rol) && 'Elige si te registras como huésped o anfitrión.',
  });

/** LoginRequest (HU-02, HU-06). */
export const validarLogin = ({ correo, clave }) =>
  limpiar({
    correo: vacio(correo) ? 'El correo es obligatorio.' : !CORREO_RE.test(correo.trim()) && 'El correo debe tener un formato válido.',
    clave: vacio(clave) && 'La clave es obligatoria.',
  });

export const validarCorreo = (correo) =>
  vacio(correo) ? 'El correo es obligatorio.' : !CORREO_RE.test(correo.trim()) ? 'El correo debe tener un formato válido.' : null;

/** RestablecerClaveRequest (HU-04). */
export const validarRestablecer = ({ token, nuevaClave, confirmar }) =>
  limpiar({
    token: vacio(token) && 'El token de recuperación es obligatorio.',
    nuevaClave: vacio(nuevaClave)
      ? 'La nueva clave es obligatoria.'
      : (nuevaClave.length < 8 || nuevaClave.length > 64) && 'La clave debe tener entre 8 y 64 caracteres.',
    confirmar: nuevaClave !== confirmar && 'Las claves no coinciden.',
  });

/** ActualizarPerfilRequest (HU-05). */
export const validarPerfil = ({ nombre, telefono, fotoUrl, claveActual, nuevaClave, confirmar }) =>
  limpiar({
    nombre: vacio(nombre) ? 'El nombre es obligatorio.' : nombre.trim().length > 120 && 'El nombre admite máximo 120 caracteres.',
    telefono:
      !vacio(telefono) &&
      !TELEFONO_RE.test(telefono.trim()) &&
      'El teléfono solo admite números, espacios y + (7 a 20 caracteres).',
    fotoUrl: !vacio(fotoUrl) && fotoUrl.length > 500 && 'La URL de la foto admite máximo 500 caracteres.',
    claveActual: !vacio(nuevaClave) && vacio(claveActual) && 'Ingresa tu clave actual para cambiarla.',
    nuevaClave: !vacio(nuevaClave) && (nuevaClave.length < 8 || nuevaClave.length > 64) && 'La clave debe tener entre 8 y 64 caracteres.',
    confirmar: !vacio(nuevaClave) && nuevaClave !== confirmar && 'Las claves no coinciden.',
  });

/** AlojamientoRequest (HU-07, HU-11, HU-12, HU-13, HU-14). */
export const validarAlojamiento = (f) => {
  const capacidad = Number(f.capacidad);
  const precio = Number(f.precioNoche);
  const limpieza = vacio(f.tarifaLimpieza) ? 0 : Number(f.tarifaLimpieza);
  return limpiar({
    titulo: vacio(f.titulo)
      ? 'El título es obligatorio.'
      : (f.titulo.trim().length < 5 || f.titulo.trim().length > 120) && 'El título debe tener entre 5 y 120 caracteres.',
    descripcion: vacio(f.descripcion)
      ? 'La descripción es obligatoria.'
      : f.descripcion.trim().length < 20 && 'La descripción debe tener al menos 20 caracteres.',
    reglas: !vacio(f.reglas) && f.reglas.length > 2000 && 'Las reglas admiten máximo 2000 caracteres.',
    ciudad: vacio(f.ciudad) ? 'La ciudad es obligatoria.' : f.ciudad.trim().length > 80 && 'La ciudad admite máximo 80 caracteres.',
    direccion: vacio(f.direccion)
      ? 'La dirección es obligatoria.'
      : f.direccion.trim().length > 150 && 'La dirección admite máximo 150 caracteres.',
    tipo: !TIPOS_ALOJAMIENTO.includes(f.tipo) && 'El tipo es obligatorio.',
    capacidad: vacio(f.capacidad)
      ? 'La capacidad es obligatoria.'
      : !Number.isInteger(capacidad) || capacidad <= 0
        ? 'La capacidad debe ser mayor a 0.'
        : capacidad > 50 && 'La capacidad máxima es 50 personas.',
    precioNoche: vacio(f.precioNoche)
      ? 'El precio por noche es obligatorio.'
      : (!Number.isFinite(precio) || precio <= 0) && 'El precio por noche debe ser mayor a 0.',
    tarifaLimpieza: (!Number.isFinite(limpieza) || limpieza < 0) && 'La tarifa de limpieza no puede ser negativa.',
    modoReserva: !['INMEDIATA', 'APROBACION'].includes(f.modoReserva) && 'El modo de reserva es obligatorio.',
    politicaId: vacio(f.politicaId) && 'La política de cancelación es obligatoria.',
  });
};

/** HU-25: la salida debe ser posterior a la llegada y la llegada no puede estar en el pasado. */
export const validarFechasEstadia = (llegada, salida) => {
  if (vacio(llegada)) return 'Elige la fecha de llegada.';
  if (vacio(salida)) return 'Elige la fecha de salida.';
  if (llegada < hoyIso()) return 'La fecha de llegada no puede estar en el pasado.';
  if (salida <= llegada) return 'La fecha de salida debe ser posterior a la de llegada.';
  if (nochesEntre(llegada, salida) > NOCHES_MAXIMAS) return `La estadía máxima es de ${NOCHES_MAXIMAS} noches.`;
  return null;
};

/** PeriodoRequest (HU-16). */
export const validarPeriodo = ({ fechaInicio, fechaFin, estado }) =>
  limpiar({
    fechaInicio: vacio(fechaInicio) && 'La fecha de inicio es obligatoria.',
    fechaFin: vacio(fechaFin)
      ? 'La fecha de fin es obligatoria.'
      : !vacio(fechaInicio) && fechaFin < fechaInicio && 'La fecha de fin no puede ser anterior al inicio.',
    estado: !['DISPONIBLE', 'BLOQUEADO'].includes(estado) && 'El estado es obligatorio (DISPONIBLE o BLOQUEADO).',
  });

/** ImagenRequest (HU-07). */
export const validarUrlImagen = (url) =>
  vacio(url)
    ? 'La URL de la imagen es obligatoria.'
    : url.length > 255
      ? 'La URL admite máximo 255 caracteres.'
      : !URL_RE.test(url.trim())
        ? 'La URL debe empezar por http:// o https://.'
        : null;

/** ServicioAdicionalRequest (HU-13). */
export const validarServicioAdicional = ({ nombre, precio }) =>
  limpiar({
    nombre: vacio(nombre) ? 'El nombre del servicio es obligatorio.' : nombre.trim().length > 80 && 'El nombre admite máximo 80 caracteres.',
    precio: vacio(precio) ? 'El precio es obligatorio.' : (!Number.isFinite(Number(precio)) || Number(precio) < 0) && 'El precio no puede ser negativo.',
  });

/** HU-20: el rango de precio valida que el mínimo no supere el máximo. */
export const validarRangoPrecio = (min, max) => {
  if (!vacio(min) && Number(min) < 0) return 'El precio mínimo no puede ser negativo.';
  if (!vacio(min) && !vacio(max) && Number(min) > Number(max)) return 'El precio mínimo no puede superar al máximo.';
  return null;
};

export const ARCHIVO_MAX_BYTES = 5 * 1024 * 1024;
export const TIPOS_IMAGEN = ['image/jpeg', 'image/png', 'image/webp', 'image/gif'];

export const validarArchivoImagen = (archivo) => {
  if (!archivo) return 'Selecciona una imagen.';
  if (!TIPOS_IMAGEN.includes(archivo.type)) return `"${archivo.name}" no es una imagen JPG, PNG, WEBP o GIF.`;
  if (archivo.size > ARCHIVO_MAX_BYTES) return `"${archivo.name}" supera el tamaño máximo de 5 MB.`;
  return null;
};
