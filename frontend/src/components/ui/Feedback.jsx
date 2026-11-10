import { Star } from 'lucide-react';
import Button from './Button';

const TONOS = {
  neutral: 'bg-canvas text-ink-soft ring-line/60',
  success: 'bg-emerald-50 text-emerald-700 ring-emerald-200/70',
  warning: 'bg-amber-50 text-amber-700 ring-amber-200/70',
  danger: 'bg-brand-soft text-[#B4233A] ring-brand/20',
  info: 'bg-accent-soft text-accent ring-accent/20',
  dark: 'bg-ink text-white ring-ink',
};

export function Badge({ tono = 'neutral', children, className = '' }) {
  return (
    <span className={`inline-flex items-center gap-1 whitespace-nowrap rounded-full px-2.5 py-0.5 text-xs font-medium ring-1 ring-inset ${TONOS[tono]} ${className}`}>
      {children}
    </span>
  );
}

/** Badge a partir de un mapa de estados de utils/constants (ESTADO_RESERVA, ESTADO_ALOJAMIENTO...). */
export function EstadoBadge({ estado, mapa }) {
  const info = mapa?.[estado] || { label: estado || '—', tone: 'neutral' };
  return <Badge tono={info.tone}>{info.label}</Badge>;
}

export function Skeleton({ className = '' }) {
  return <div className={`skeleton rounded-xl ${className}`} aria-hidden="true" />;
}

export function EmptyState({ icono: Icono, titulo, descripcion, accion }) {
  return (
    <div className="flex flex-col items-center justify-center rounded-3xl border border-dashed border-line bg-canvas/50 px-6 py-16 text-center">
      {Icono && (
        <div className="mb-4 flex h-14 w-14 items-center justify-center rounded-2xl bg-white shadow-soft">
          <Icono className="h-6 w-6 text-ink-muted" strokeWidth={1.6} />
        </div>
      )}
      <h3 className="text-lg font-semibold text-ink">{titulo}</h3>
      {descripcion && <p className="mt-1.5 max-w-md text-sm text-ink-muted">{descripcion}</p>}
      {accion && <div className="mt-6">{accion}</div>}
    </div>
  );
}

export function ErrorState({ error, alReintentar }) {
  return (
    <div className="flex flex-col items-center justify-center rounded-3xl bg-canvas px-6 py-14 text-center">
      <h3 className="text-lg font-semibold text-ink">No pudimos cargar esta información</h3>
      <p className="mt-1.5 max-w-md text-sm text-ink-muted">{error?.message || 'Ocurrió un error inesperado.'}</p>
      {alReintentar && (
        <Button variante="outline" tamano="sm" className="mt-5" onClick={alReintentar}>
          Reintentar
        </Button>
      )}
    </div>
  );
}

/** Estrellas de solo lectura o interactivas (HU-38: calificación de 1 a 5). */
export function Estrellas({ valor = 0, alCambiar, tamano = 'h-5 w-5', etiqueta = 'Calificación' }) {
  const interactivo = typeof alCambiar === 'function';
  return (
    <div className="flex items-center gap-1" role={interactivo ? 'radiogroup' : 'img'} aria-label={`${etiqueta}: ${valor} de 5`}>
      {[1, 2, 3, 4, 5].map((n) => {
        const lleno = n <= Math.round(valor);
        const estrella = <Star className={`${tamano} transition ${lleno ? 'fill-ink text-ink' : 'fill-transparent text-line'}`} strokeWidth={1.5} />;
        return interactivo ? (
          <button
            key={n}
            type="button"
            role="radio"
            aria-checked={n === valor}
            aria-label={`${n} estrella${n > 1 ? 's' : ''}`}
            onClick={() => alCambiar(n)}
            className="rounded-md p-0.5 transition hover:scale-110 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-accent/40"
          >
            {estrella}
          </button>
        ) : (
          <span key={n}>{estrella}</span>
        );
      })}
    </div>
  );
}

export function Card({ children, className = '', padding = 'p-6' }) {
  return <div className={`rounded-3xl border border-line/50 bg-white shadow-soft ${padding} ${className}`}>{children}</div>;
}

export function SectionTitle({ titulo, subtitulo, accion }) {
  return (
    <div className="mb-6 flex flex-wrap items-end justify-between gap-4">
      <div>
        <h1 className="text-3xl font-semibold tracking-tight text-ink sm:text-[34px]">{titulo}</h1>
        {subtitulo && <p className="mt-1.5 text-[15px] text-ink-muted">{subtitulo}</p>}
      </div>
      {accion}
    </div>
  );
}

export function Paginacion({ pagina = 0, totalPaginas = 0, alCambiar }) {
  if (!totalPaginas || totalPaginas <= 1) return null;
  return (
    <nav className="mt-8 flex items-center justify-center gap-3" aria-label="Paginación">
      <Button variante="outline" tamano="sm" disabled={pagina <= 0} onClick={() => alCambiar(pagina - 1)}>
        Anterior
      </Button>
      <span className="text-sm text-ink-muted">
        Página <strong className="text-ink">{pagina + 1}</strong> de {totalPaginas}
      </span>
      <Button variante="outline" tamano="sm" disabled={pagina >= totalPaginas - 1} onClick={() => alCambiar(pagina + 1)}>
        Siguiente
      </Button>
    </nav>
  );
}

export function Stat({ etiqueta, valor, detalle, icono: Icono }) {
  return (
    <div className="rounded-3xl border border-line/50 bg-white p-5 shadow-soft">
      <div className="flex items-center justify-between">
        <p className="text-[13px] font-medium text-ink-muted">{etiqueta}</p>
        {Icono && <Icono className="h-4 w-4 text-ink-faint" strokeWidth={1.8} />}
      </div>
      <p className="mt-2 text-[28px] font-semibold tracking-tight text-ink">{valor}</p>
      {detalle && <p className="mt-0.5 text-xs text-ink-faint">{detalle}</p>}
    </div>
  );
}
