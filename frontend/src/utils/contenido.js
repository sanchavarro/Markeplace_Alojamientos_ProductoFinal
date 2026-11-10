import { unsplash } from './images';

/**
 * Contenido editorial de la marca (secciones de descubrimiento, pasos, beneficios y preguntas frecuentes).
 * No son datos de negocio: los alojamientos, precios y disponibilidad siempre vienen de la API.
 * Cada destino lleva a una búsqueda real (GET /busqueda?ciudad=...).
 */
export const DESTINOS = [
  { ciudad: 'Cartagena', region: 'Caribe', frase: 'Murallas, atardeceres y mar turquesa', foto: unsplash('1613490493576-7fde63acd811', 900) },
  { ciudad: 'Medellín', region: 'Antioquia', frase: 'La ciudad de la eterna primavera', foto: unsplash('1502672260266-1c1ef2d93688', 900) },
  { ciudad: 'Bogotá', region: 'Cundinamarca', frase: 'Cultura, museos y gastronomía', foto: unsplash('1518780664697-55e3ad937233', 900) },
  { ciudad: 'Santa Marta', region: 'Magdalena', frase: 'Entre la Sierra Nevada y el mar', foto: unsplash('1613977257363-707ba9348227', 900) },
  { ciudad: 'Salento', region: 'Eje Cafetero', frase: 'Palmas de cera y café de origen', foto: unsplash('1449158743715-0a90ebb6d2d8', 900) },
  { ciudad: 'San Andrés', region: 'Insular', frase: 'El mar de los siete colores', foto: unsplash('1519046904884-53103b34b206', 900) },
  { ciudad: 'Villa de Leyva', region: 'Boyacá', frase: 'Calles empedradas y cielo abierto', foto: unsplash('1564013799919-ab600027ffc6', 900) },
  { ciudad: 'Guatapé', region: 'Antioquia', frase: 'Embalses y la Piedra del Peñol', foto: unsplash('1542718610-a1d656d1884c', 900) },
];

export const TIPOS_DESTACADOS = [
  { tipo: 'CASA', titulo: 'Casas completas', texto: 'Espacio para toda la familia', foto: unsplash('1580587771525-78b9dba3b914', 800) },
  { tipo: 'APARTAMENTO', titulo: 'Apartamentos', texto: 'Ideales para la ciudad', foto: unsplash('1522708323590-d24dbb6b0267', 800) },
  { tipo: 'CABANA', titulo: 'Cabañas', texto: 'Naturaleza y desconexión', foto: unsplash('1510798831971-661eb04b3739', 800) },
  { tipo: 'FINCA', titulo: 'Fincas', texto: 'Planes en grupo y al aire libre', foto: unsplash('1570129477492-45c003edd2be', 800) },
  { tipo: 'GLAMPING', titulo: 'Glamping', texto: 'Acampar con todas las comodidades', foto: unsplash('1504280390367-361c6d9f38f4', 800) },
  { tipo: 'HABITACION', titulo: 'Habitaciones', texto: 'La opción más económica', foto: unsplash('1631049307264-da0ec9d70304', 800) },
];

export const PASOS_HUESPED = [
  { titulo: 'Busca y compara', texto: 'Filtra por ciudad, fechas, huéspedes, precio y servicios. Compara hasta 3 opciones lado a lado.' },
  { titulo: 'Revisa el precio total', texto: 'Antes de pagar ves cada valor: noches, limpieza, tarifa de servicio, descuentos y extras.' },
  { titulo: 'Paga de forma segura', texto: 'Ingresas tu tarjeta en la pasarela de pago. Un doble clic nunca genera dos cobros.' },
  { titulo: 'Disfruta y califica', texto: 'Recibe la confirmación en tus notificaciones y, al terminar, deja tu reseña.' },
];

export const PASOS_ANFITRION = [
  { titulo: 'Crea tu publicación', texto: 'Agrega fotos, descripción, precio, servicios, reglas y tu política de cancelación.' },
  { titulo: 'Define tu disponibilidad', texto: 'Abre o bloquea fechas en el calendario cuando quieras.' },
  { titulo: 'Pasa la revisión', texto: 'Nuestro equipo revisa la publicación para que el catálogo sea confiable.' },
  { titulo: 'Recibe reservas', texto: 'Elige reserva inmediata o aprueba cada solicitud. Te avisamos de todo.' },
];

export const BENEFICIOS = [
  { icono: 'ShieldCheck', titulo: 'Alojamientos revisados', texto: 'Cada publicación pasa por moderación antes de aparecer en el catálogo.' },
  { icono: 'CreditCard', titulo: 'Pago protegido', texto: 'Pasarela con claves de idempotencia: sin cobros duplicados.' },
  { icono: 'RotateCcw', titulo: 'Cancelación clara', texto: 'Antes de cancelar ves exactamente cuánto te devolvemos.' },
  { icono: 'Star', titulo: 'Reseñas reales', texto: 'Solo opinan huéspedes que completaron su estadía.' },
];

export const PREGUNTAS = [
  {
    p: '¿Cómo sé cuánto voy a pagar?',
    r: 'En el detalle de cada alojamiento eliges fechas y huéspedes y ves el desglose completo calculado por el servidor: noches, limpieza, tarifa de servicio, descuentos y servicios adicionales.',
  },
  {
    p: '¿Qué diferencia hay entre reserva inmediata y con aprobación?',
    r: 'Con reserva inmediata pagas y queda confirmada al instante. Con aprobación envías una solicitud; cuando el anfitrión la acepta te avisamos para que completes el pago.',
  },
  {
    p: '¿Puedo cancelar?',
    r: 'Sí, antes de la fecha de llegada. Según la política del alojamiento (flexible o estricta) te mostramos cuánto se devuelve antes de confirmar.',
  },
  {
    p: '¿Por qué debo confirmar mi correo?',
    r: 'Para proteger tu cuenta y asegurarnos de que las confirmaciones de reserva y los avisos te lleguen. Te enviamos un código de 6 dígitos al registrarte.',
  },
  {
    p: '¿Cómo publico mi espacio?',
    r: 'Crea una cuenta de anfitrión, completa la publicación con fotos y disponibilidad y envíala a revisión. Cuando el equipo la aprueba aparece en el catálogo.',
  },
  {
    p: '¿Es seguro pagar con tarjeta?',
    r: 'El pago se procesa a través de la pasarela (Stripe en modo pruebas para este proyecto). Los datos de la tarjeta no se guardan en nuestra plataforma.',
  },
];
