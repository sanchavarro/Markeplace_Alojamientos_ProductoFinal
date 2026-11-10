import { useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import { ChevronLeft, ChevronRight, Star, Plus, Check } from 'lucide-react';
import { useCompare } from '../../context/CompareContext';
import { useToast } from '../../context/ToastContext';
import { TIPO_LABEL, MODO_LABEL } from '../../utils/constants';
import { dinero, calificacion, rangoFechas } from '../../utils/format';
import { fotosDeAlojamiento } from '../../utils/images';
import SafeImage from '../ui/SafeImage';
import { Skeleton } from '../ui';

/**
 * Mini carrusel estilo Airbnb: flechas suaves al hacer hover, puntos indicadores y deslizamiento táctil.
 * El resumen de búsqueda (AlojamientoResumenDTO) solo trae imagenPrincipal; se complementa con imágenes
 * de respaldo para que el carrusel siempre tenga recorrido visual.
 */
export function ImageCarousel({ fotos, semilla, alt, aspecto = 'aspect-[20/19]', className = '' }) {
  const [indice, setIndice] = useState(0);
  const inicioX = useRef(null);
  const total = fotos.length;

  const ir = (e, delta) => {
    e?.preventDefault();
    e?.stopPropagation();
    setIndice((i) => (i + delta + total) % total);
  };

  return (
    <div
      className={`group/carrusel relative overflow-hidden rounded-2xl ${aspecto} ${className}`}
      onTouchStart={(e) => (inicioX.current = e.touches[0].clientX)}
      onTouchEnd={(e) => {
        if (inicioX.current === null) return;
        const dx = e.changedTouches[0].clientX - inicioX.current;
        if (Math.abs(dx) > 40) setIndice((i) => (i + (dx < 0 ? 1 : -1) + total) % total);
        inicioX.current = null;
      }}
    >
      <div className="flex h-full transition-transform duration-500 ease-[cubic-bezier(0.2,0.8,0.2,1)]" style={{ transform: `translateX(-${indice * 100}%)` }}>
        {fotos.map((f, i) => (
          <SafeImage key={`${f}-${i}`} src={f} semilla={semilla + i} alt={i === 0 ? alt : ''} className="h-full w-full shrink-0" eager={i === 0} />
        ))}
      </div>

      {total > 1 && (
        <>
          <button
            type="button"
            onClick={(e) => ir(e, -1)}
            aria-label="Foto anterior"
            className="absolute left-3 top-1/2 flex h-8 w-8 -translate-y-1/2 items-center justify-center rounded-full bg-white/90 text-ink opacity-0 shadow-pill backdrop-blur transition hover:scale-105 hover:bg-white focus-visible:opacity-100 group-hover/carrusel:opacity-100"
          >
            <ChevronLeft className="h-4 w-4" strokeWidth={2.4} />
          </button>
          <button
            type="button"
            onClick={(e) => ir(e, 1)}
            aria-label="Foto siguiente"
            className="absolute right-3 top-1/2 flex h-8 w-8 -translate-y-1/2 items-center justify-center rounded-full bg-white/90 text-ink opacity-0 shadow-pill backdrop-blur transition hover:scale-105 hover:bg-white focus-visible:opacity-100 group-hover/carrusel:opacity-100"
          >
            <ChevronRight className="h-4 w-4" strokeWidth={2.4} />
          </button>
          <div className="pointer-events-none absolute inset-x-0 bottom-3 flex justify-center gap-1.5">
            {fotos.slice(0, 5).map((_, i) => (
              <span key={i} className={`h-1.5 rounded-full bg-white shadow transition-all duration-300 ${i === indice % 5 ? 'w-1.5 opacity-100' : 'w-1.5 opacity-55'}`} />
            ))}
          </div>
        </>
      )}
    </div>
  );
}

export default function ListingCard({ alojamiento, llegada, salida, numHuespedes, comparable = true }) {
  const { estaSeleccionado, alternar } = useCompare();
  const toast = useToast();
  const fotos = fotosDeAlojamiento(alojamiento, 4);
  const seleccionado = estaSeleccionado(alojamiento.id);
  const nota = calificacion(alojamiento.promedioCalificacion);

  const consulta = new URLSearchParams();
  if (llegada) consulta.set('llegada', llegada);
  if (salida) consulta.set('salida', salida);
  if (numHuespedes) consulta.set('huespedes', numHuespedes);
  const enlace = `/alojamientos/${alojamiento.id}${consulta.toString() ? `?${consulta}` : ''}`;

  const comparar = (e) => {
    e.preventDefault();
    e.stopPropagation();
    if (!alternar(alojamiento)) toast.aviso('Puedes comparar hasta 3 alojamientos a la vez.');
  };

  return (
    <article className="group relative">
      <Link to={enlace} className="block rounded-2xl focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-accent/25">
        <div className="relative">
          <ImageCarousel fotos={fotos} semilla={alojamiento.id} alt={alojamiento.titulo} />
          {alojamiento.modoReserva === 'INMEDIATA' && (
            <span className="glass absolute left-3 top-3 rounded-full px-2.5 py-1 text-[11.5px] font-semibold text-ink shadow-pill">
              {MODO_LABEL.INMEDIATA}
            </span>
          )}
        </div>

        <div className="mt-3 flex items-start justify-between gap-3">
          <h3 className="line-clamp-1 text-[15px] font-semibold text-ink">
            {TIPO_LABEL[alojamiento.tipo] || alojamiento.tipo} en {alojamiento.ciudad}
          </h3>
          <span className="flex shrink-0 items-center gap-1 text-[14px] text-ink">
            <Star className="h-3.5 w-3.5 fill-ink text-ink" />
            {nota ? (
              <>
                {nota}
                <span className="text-ink-muted">({alojamiento.totalResenas})</span>
              </>
            ) : (
              <span className="text-ink-muted">Nuevo</span>
            )}
          </span>
        </div>
        <p className="line-clamp-1 text-[14px] text-ink-muted">{alojamiento.titulo}</p>
        <p className="text-[14px] text-ink-muted">
          {llegada && salida ? rangoFechas(llegada, salida) : `Hasta ${alojamiento.capacidad} huéspedes`}
        </p>
        <p className="mt-1.5 text-[15px] text-ink">
          <span className="font-semibold">{dinero(alojamiento.precioNoche)}</span> <span className="text-ink-soft">noche</span>
        </p>
      </Link>

      {comparable && (
        <button
          type="button"
          onClick={comparar}
          aria-pressed={seleccionado}
          className={`absolute right-3 top-3 flex h-8 items-center gap-1 rounded-full px-2.5 text-[11.5px] font-semibold shadow-pill backdrop-blur transition ${
            seleccionado ? 'bg-ink text-white' : 'bg-white/85 text-ink opacity-100 sm:opacity-0 sm:group-hover:opacity-100'
          }`}
        >
          {seleccionado ? <Check className="h-3.5 w-3.5" /> : <Plus className="h-3.5 w-3.5" />}
          Comparar
        </button>
      )}
    </article>
  );
}

/** Skeleton idéntico a la estructura de la tarjeta. */
export function ListingCardSkeleton() {
  return (
    <div aria-hidden="true">
      <Skeleton className="aspect-[20/19] w-full rounded-2xl" />
      <div className="mt-3 flex justify-between gap-4">
        <Skeleton className="h-4 w-2/3" />
        <Skeleton className="h-4 w-10" />
      </div>
      <Skeleton className="mt-2 h-3.5 w-1/2" />
      <Skeleton className="mt-2 h-3.5 w-1/3" />
      <Skeleton className="mt-3 h-4 w-2/5" />
    </div>
  );
}
