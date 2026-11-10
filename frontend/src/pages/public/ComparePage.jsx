import { useMemo } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { Check, Minus, Star, X, Scale } from 'lucide-react';
import { catalogoApi } from '../../api';
import { useAsync } from '../../hooks/useAsync';
import { useCompare } from '../../context/CompareContext';
import { Button, EmptyState, ErrorState, SafeImage, Skeleton } from '../../components/ui';
import { MODO_LABEL, TIPO_LABEL } from '../../utils/constants';
import { calificacion, dinero, plural } from '../../utils/format';
import { fotosDeAlojamiento } from '../../utils/images';

/**
 * Comparación lado a lado al estilo de la página "Comparar modelos" de Apple (HU-22):
 * precio, capacidad, servicios, política y calificación de 2 a 3 alojamientos (GET /busqueda/comparar).
 */
export default function ComparePage() {
  const [sp] = useSearchParams();
  const navegar = useNavigate();
  const { quitar } = useCompare();
  const ids = useMemo(
    () => [...new Set((sp.get('ids') || '').split(',').map(Number).filter((n) => Number.isInteger(n) && n > 0))].slice(0, 3),
    [sp],
  );
  const clave = ids.join(',');

  const { data, error, cargando, recargar } = useAsync(() => (ids.length >= 2 ? catalogoApi.comparar(ids) : Promise.resolve([])), [clave]);
  const lista = data || [];

  const todosServicios = useMemo(() => {
    const mapa = new Map();
    lista.forEach((a) => (a.servicios || []).forEach((s) => mapa.set(s.id, s.nombre)));
    return [...mapa.entries()].sort((x, y) => x[1].localeCompare(y[1]));
  }, [lista]);

  const quitarDeComparacion = (id) => {
    quitar(id);
    const restantes = ids.filter((x) => x !== id);
    navegar(restantes.length ? `/comparar?ids=${restantes.join(',')}` : '/', { replace: true });
  };

  if (ids.length < 2) {
    return (
      <div className="container-app py-16">
        <EmptyState icono={Scale} titulo="Elige al menos 2 alojamientos" descripcion="En el catálogo, toca «Comparar» en hasta 3 alojamientos para verlos lado a lado." accion={<Button to="/">Ir al catálogo</Button>} />
      </div>
    );
  }

  const columnas = `repeat(${Math.max(lista.length, 2)}, minmax(220px, 1fr))`;

  return (
    <div className="container-app py-10">
      <h1 className="text-center text-[34px] font-semibold tracking-tight sm:text-[44px]">Compara alojamientos</h1>
      <p className="mt-2 text-center text-[17px] text-ink-muted">Precio, capacidad, servicios, política y calificación, lado a lado.</p>

      {error ? (
        <div className="mt-10">
          <ErrorState error={error} alReintentar={recargar} />
        </div>
      ) : cargando ? (
        <div className="mt-12 grid gap-8 sm:grid-cols-3">
          {ids.map((i) => (
            <div key={i}>
              <Skeleton className="aspect-[4/3] w-full rounded-3xl" />
              <Skeleton className="mt-4 h-5 w-2/3" />
              <Skeleton className="mt-2 h-4 w-1/2" />
            </div>
          ))}
        </div>
      ) : lista.length < 2 ? (
        <div className="mt-10">
          <EmptyState icono={Scale} titulo="No hay suficientes alojamientos activos" descripcion="Algunos de los seleccionados ya no están publicados. Elige otros desde el catálogo." accion={<Button to="/">Ir al catálogo</Button>} />
        </div>
      ) : (
        <div className="no-scrollbar mt-12 overflow-x-auto">
          <div className="min-w-[680px]">
            <div className="grid gap-8" style={{ gridTemplateColumns: columnas }}>
              {lista.map((a) => (
                <div key={a.id} className="relative text-center">
                  <button type="button" onClick={() => quitarDeComparacion(a.id)} className="glass absolute right-3 top-3 z-10 flex h-8 w-8 items-center justify-center rounded-full shadow-pill" aria-label={`Quitar ${a.titulo}`}>
                    <X className="h-4 w-4" />
                  </button>
                  <SafeImage src={fotosDeAlojamiento(a)[0]} semilla={a.id} alt={a.titulo} className="aspect-[4/3] w-full rounded-3xl" />
                  <p className="mt-5 text-xs font-semibold uppercase tracking-wider text-brand">{TIPO_LABEL[a.tipo]}</p>
                  <h2 className="mt-1 line-clamp-2 text-[21px] font-semibold leading-snug">{a.titulo}</h2>
                  <p className="mt-1 text-[14px] text-ink-muted">{a.ciudad}</p>
                  <p className="mt-4 text-[17px]">
                    <span className="font-semibold">{dinero(a.precioNoche)}</span> <span className="text-ink-muted">/ noche</span>
                  </p>
                  <Button tamano="sm" className="mt-4" to={`/alojamientos/${a.id}`}>
                    Ver y reservar
                  </Button>
                </div>
              ))}
            </div>

            <Fila titulo="Calificación" columnas={columnas}>
              {lista.map((a) => (
                <p key={a.id} className="flex items-center justify-center gap-1 text-[17px] font-semibold">
                  {calificacion(a.promedioCalificacion) ? (
                    <>
                      <Star className="h-4 w-4 fill-ink" /> {calificacion(a.promedioCalificacion)}
                      <span className="text-sm font-normal text-ink-muted">({a.totalResenas})</span>
                    </>
                  ) : (
                    <span className="text-sm font-normal text-ink-muted">Sin reseñas</span>
                  )}
                </p>
              ))}
            </Fila>
            <Fila titulo="Capacidad" columnas={columnas}>
              {lista.map((a) => (
                <p key={a.id} className="text-[17px] font-semibold">
                  {plural(a.capacidad, 'huésped', 'huéspedes')}
                </p>
              ))}
            </Fila>
            <Fila titulo="Tarifa de limpieza" columnas={columnas}>
              {lista.map((a) => (
                <p key={a.id} className="text-[17px] font-semibold">
                  {Number(a.tarifaLimpieza) > 0 ? dinero(a.tarifaLimpieza) : 'Incluida'}
                </p>
              ))}
            </Fila>
            <Fila titulo="Modalidad" columnas={columnas}>
              {lista.map((a) => (
                <p key={a.id} className="text-[15px]">
                  {MODO_LABEL[a.modoReserva]}
                </p>
              ))}
            </Fila>
            <Fila titulo="Política de cancelación" columnas={columnas}>
              {lista.map((a) => (
                <div key={a.id}>
                  <p className="text-[15px] font-semibold">{a.politica?.nombre || '—'}</p>
                  <p className="mt-1 text-[13px] text-ink-muted">{a.politica?.descripcion}</p>
                </div>
              ))}
            </Fila>
            <Fila titulo="Servicios adicionales" columnas={columnas}>
              {lista.map((a) => (
                <div key={a.id} className="space-y-1 text-[14px]">
                  {(a.serviciosAdicionales || []).length === 0 ? (
                    <p className="text-ink-muted">—</p>
                  ) : (
                    a.serviciosAdicionales.map((s) => (
                      <p key={s.id}>
                        {s.nombre} · <span className="text-ink-muted">{dinero(s.precio)}</span>
                      </p>
                    ))
                  )}
                </div>
              ))}
            </Fila>

            <h3 className="mt-14 border-b border-line pb-3 text-[22px] font-semibold">Servicios</h3>
            {todosServicios.length === 0 && <p className="py-6 text-center text-ink-muted">Ninguno de estos alojamientos registró servicios.</p>}
            {todosServicios.map(([sid, nombre]) => (
              <div key={sid} className="grid gap-8 border-b border-line/50 py-4" style={{ gridTemplateColumns: columnas }}>
                {lista.map((a) => {
                  const tiene = (a.servicios || []).some((s) => s.id === sid);
                  return (
                    <div key={a.id} className="flex flex-col items-center gap-1 text-center">
                      {tiene ? <Check className="h-5 w-5 text-ink" /> : <Minus className="h-5 w-5 text-line" />}
                      <span className={`text-[13.5px] ${tiene ? 'text-ink' : 'text-ink-faint'}`}>{nombre}</span>
                    </div>
                  );
                })}
              </div>
            ))}
          </div>
        </div>
      )}
      <p className="mt-10 text-center text-sm text-ink-muted">
        <Link to="/" className="text-accent hover:underline">
          Seguir explorando
        </Link>
      </p>
    </div>
  );
}

function Fila({ titulo, columnas, children }) {
  return (
    <div className="mt-10">
      <h3 className="border-b border-line pb-3 text-center text-[13px] font-semibold uppercase tracking-wider text-ink-muted">{titulo}</h3>
      <div className="grid gap-8 pt-5 text-center" style={{ gridTemplateColumns: columnas }}>
        {children}
      </div>
    </div>
  );
}
