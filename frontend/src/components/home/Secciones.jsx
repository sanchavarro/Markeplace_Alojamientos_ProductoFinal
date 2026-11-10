import { useState } from 'react';
import { Link } from 'react-router-dom';
import { ArrowRight, ChevronDown, CreditCard, RotateCcw, ShieldCheck, Star, KeyRound } from 'lucide-react';
import { BENEFICIOS, DESTINOS, PREGUNTAS, TIPOS_DESTACADOS } from '../../utils/contenido';
import SafeImage from '../ui/SafeImage';
import { Button } from '../ui';

/** Grilla de tarjetas centrada: columnas de 260–300 px que se reparten desde el centro. */
export const GRID_CENTRADO =
  'mx-auto grid max-w-[1240px] justify-center gap-x-6 gap-y-10 [grid-template-columns:repeat(auto-fit,minmax(min(100%,260px),300px))]';

const ICONOS = { ShieldCheck, CreditCard, RotateCcw, Star };

/** Encabezado de sección centrado, con antetítulo, título y bajada. */
export function EncabezadoSeccion({ antetitulo, titulo, texto, accion, alineado = 'centro' }) {
  const centro = alineado === 'centro';
  return (
    <div className={`mb-10 ${centro ? 'mx-auto max-w-2xl text-center' : 'flex flex-wrap items-end justify-between gap-4'}`}>
      <div>
        {antetitulo && <p className="text-[13px] font-semibold uppercase tracking-[0.14em] text-brand">{antetitulo}</p>}
        <h2 className="mt-2 text-[30px] font-semibold leading-tight tracking-tight sm:text-[38px]">{titulo}</h2>
        {texto && <p className={`mt-3 text-[16.5px] leading-relaxed text-ink-muted ${centro ? '' : 'max-w-xl'}`}>{texto}</p>}
      </div>
      {accion && <div className={centro ? 'mt-6' : ''}>{accion}</div>}
    </div>
  );
}

/** "Descubre nuevos espacios": destinos que llevan a una búsqueda real por ciudad. */
export function SeccionDestinos({ limite = DESTINOS.length, conEncabezado = true }) {
  return (
    <section className="container-app py-16">
      {conEncabezado && (
        <EncabezadoSeccion
          antetitulo="Descubre nuevos espacios"
          titulo="Destinos que enamoran"
          texto="Del Caribe al Eje Cafetero: elige un destino y te mostramos los alojamientos disponibles."
        />
      )}
      <div className="mx-auto grid max-w-[1240px] gap-5 sm:grid-cols-2 lg:grid-cols-4">
        {DESTINOS.slice(0, limite).map((d, i) => (
          <Link
            key={d.ciudad}
            to={`/explorar?ciudad=${encodeURIComponent(d.ciudad)}`}
            className={`group relative overflow-hidden rounded-[28px] ${i === 0 && limite > 4 ? 'sm:col-span-2 sm:row-span-2' : ''}`}
          >
            <SafeImage src={d.foto} semilla={i} alt={d.ciudad} className={`w-full ${i === 0 && limite > 4 ? 'aspect-square sm:h-full' : 'aspect-[4/5]'}`} imgClassName="transition duration-700 group-hover:scale-[1.05]" />
            <div className="absolute inset-0 bg-gradient-to-t from-black/65 via-black/10 to-transparent" />
            <div className="absolute inset-x-0 bottom-0 p-5 text-white">
              <p className="text-[12px] font-semibold uppercase tracking-[0.14em] text-white/75">{d.region}</p>
              <p className="mt-1 text-[24px] font-semibold tracking-tight">{d.ciudad}</p>
              <p className="mt-0.5 text-[14px] text-white/85">{d.frase}</p>
              <span className="mt-3 inline-flex items-center gap-1 text-[13px] font-semibold opacity-0 transition group-hover:opacity-100">
                Ver alojamientos <ArrowRight className="h-3.5 w-3.5" />
              </span>
            </div>
          </Link>
        ))}
      </div>
    </section>
  );
}

/** Explora por tipo de alojamiento. */
export function SeccionTipos() {
  return (
    <section className="bg-canvas py-16">
      <div className="container-app">
        <EncabezadoSeccion antetitulo="A tu estilo" titulo="¿Qué tipo de lugar buscas?" texto="Cada viaje pide un espacio distinto. Empieza por el que más se parece a tu plan." />
        <div className="mx-auto grid max-w-[1240px] grid-cols-2 gap-4 md:grid-cols-3 lg:grid-cols-6">
          {TIPOS_DESTACADOS.map((t, i) => (
            <Link key={t.tipo} to={`/explorar?tipo=${t.tipo}`} className="group rounded-3xl bg-white p-2.5 shadow-soft transition hover:-translate-y-1 hover:shadow-lift">
              <SafeImage src={t.foto} semilla={i + 3} alt={t.titulo} className="aspect-square w-full rounded-2xl" imgClassName="transition duration-700 group-hover:scale-[1.06]" />
              <p className="mt-3 px-1.5 text-[15px] font-semibold">{t.titulo}</p>
              <p className="px-1.5 pb-1.5 text-[13px] text-ink-muted">{t.texto}</p>
            </Link>
          ))}
        </div>
      </div>
    </section>
  );
}

/** Pasos numerados para entender el servicio. */
export function SeccionPasos({ antetitulo, titulo, texto, pasos, fondo = '' }) {
  return (
    <section className={`py-16 ${fondo}`}>
      <div className="container-app">
        <EncabezadoSeccion antetitulo={antetitulo} titulo={titulo} texto={texto} />
        <ol className="mx-auto grid max-w-[1240px] gap-5 sm:grid-cols-2 lg:grid-cols-4">
          {pasos.map((p, i) => (
            <li key={p.titulo} className="relative rounded-3xl border border-line/60 bg-white p-6 shadow-soft">
              <span className="flex h-10 w-10 items-center justify-center rounded-full bg-ink text-[15px] font-semibold text-white">{i + 1}</span>
              <h3 className="mt-5 text-[18px] font-semibold">{p.titulo}</h3>
              <p className="mt-2 text-[14.5px] leading-relaxed text-ink-muted">{p.texto}</p>
            </li>
          ))}
        </ol>
      </div>
    </section>
  );
}

export function SeccionBeneficios() {
  return (
    <section className="container-app py-16">
      <EncabezadoSeccion antetitulo="Reserva con confianza" titulo="Pensado para que viajes tranquilo" />
      <div className="mx-auto grid max-w-[1240px] gap-5 sm:grid-cols-2 lg:grid-cols-4">
        {BENEFICIOS.map((b) => {
          const Icono = ICONOS[b.icono] || ShieldCheck;
          return (
            <div key={b.titulo} className="rounded-3xl bg-canvas p-6 text-center">
              <span className="mx-auto flex h-12 w-12 items-center justify-center rounded-2xl bg-white shadow-soft">
                <Icono className="h-5 w-5" strokeWidth={1.7} />
              </span>
              <h3 className="mt-4 text-[17px] font-semibold">{b.titulo}</h3>
              <p className="mt-1.5 text-[14px] leading-relaxed text-ink-muted">{b.texto}</p>
            </div>
          );
        })}
      </div>
    </section>
  );
}

/** Banner para atraer anfitriones. */
export function CtaAnfitrion({ foto }) {
  return (
    <section className="container-app py-10">
      <div className="relative mx-auto max-w-[1240px] overflow-hidden rounded-[32px] bg-ink text-white">
        <SafeImage src={foto} semilla={9} className="absolute inset-0 h-full w-full opacity-40" />
        <div className="absolute inset-0 bg-gradient-to-r from-black/80 via-black/50 to-transparent" />
        <div className="relative max-w-xl px-8 py-14 sm:px-12 sm:py-20">
          <span className="inline-flex items-center gap-2 rounded-full bg-white/15 px-3 py-1 text-[12.5px] font-semibold backdrop-blur">
            <KeyRound className="h-3.5 w-3.5" /> Para anfitriones
          </span>
          <h2 className="mt-4 text-[32px] font-semibold leading-tight tracking-tight sm:text-[42px]">Tu espacio puede ser el próximo destino de alguien.</h2>
          <p className="mt-3 text-[16.5px] text-white/80">Publica gratis, define tus precios y tu calendario, y elige a quién recibir.</p>
          <div className="mt-7 flex flex-wrap gap-3">
            <Button variante="brand" tamano="lg" to="/anfitriones">
              Hazte anfitrión
            </Button>
            <Link to="/como-funciona" className="inline-flex h-[52px] items-center rounded-full border border-white/40 px-6 text-[15px] font-medium text-white transition hover:bg-white/10">
              Cómo funciona
            </Link>
          </div>
        </div>
      </div>
    </section>
  );
}

/** Preguntas frecuentes en acordeón accesible. */
export function Preguntas({ limite = PREGUNTAS.length, id }) {
  const [abierta, setAbierta] = useState(0);
  return (
    <div id={id} className="mx-auto max-w-3xl scroll-mt-24 divide-y divide-line/70 overflow-hidden rounded-3xl border border-line/60 bg-white shadow-soft">
      {PREGUNTAS.slice(0, limite).map((q, i) => {
        const activa = abierta === i;
        return (
          <div key={q.p}>
            <button
              type="button"
              onClick={() => setAbierta(activa ? -1 : i)}
              aria-expanded={activa}
              className="flex w-full items-center justify-between gap-4 px-6 py-5 text-left text-[16px] font-semibold transition hover:bg-canvas/60"
            >
              {q.p}
              <ChevronDown className={`h-5 w-5 shrink-0 text-ink-muted transition-transform duration-300 ${activa ? 'rotate-180' : ''}`} />
            </button>
            <div className={`grid transition-all duration-300 ${activa ? 'grid-rows-[1fr] opacity-100' : 'grid-rows-[0fr] opacity-0'}`}>
              <p className="overflow-hidden px-6 text-[15px] leading-relaxed text-ink-muted">
                <span className="block pb-5">{q.r}</span>
              </p>
            </div>
          </div>
        );
      })}
    </div>
  );
}
