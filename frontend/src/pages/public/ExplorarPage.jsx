import { useEffect, useMemo, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { SearchX } from 'lucide-react';
import { catalogoApi } from '../../api';
import { useCatalogos } from '../../hooks/useCatalogos';
import SearchBar from '../../components/listings/SearchBar';
import FiltersModal from '../../components/listings/FiltersModal';
import ListingCard, { ListingCardSkeleton } from '../../components/listings/ListingCard';
import CompareBar from '../../components/listings/CompareBar';
import { GRID_CENTRADO } from '../../components/home/Secciones';
import { EmptyState, ErrorState, Paginacion, Button } from '../../components/ui';
import { PAGINA_TAMANO, TIPOS_ALOJAMIENTO, TIPO_LABEL } from '../../utils/constants';
import { numero, plural } from '../../utils/format';

/**
 * Catálogo y búsqueda (HU-19, HU-20, HU-21, HU-22).
 * Los filtros viven en la URL (?ciudad=&llegada=...) para que la búsqueda se pueda compartir y recargar.
 * Paginación de 20 en 20 según ADR-08.
 */
const leerFiltros = (sp) => ({
  ciudad: sp.get('ciudad') || '',
  llegada: sp.get('llegada') || '',
  salida: sp.get('salida') || '',
  capacidad: sp.get('capacidad') || '',
  tipo: sp.get('tipo') || '',
  precioMin: sp.get('precioMin') || '',
  precioMax: sp.get('precioMax') || '',
  servicios: sp.getAll('servicios').map(Number).filter(Boolean),
  pagina: Number(sp.get('pagina') || 0),
});

export default function ExplorarPage() {
  const [sp, setSp] = useSearchParams();
  const filtros = useMemo(() => leerFiltros(sp), [sp]);
  const { servicios } = useCatalogos();
  const [resultado, setResultado] = useState(null);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState(null);
  const [filtrosAbiertos, setFiltrosAbiertos] = useState(false);
  const [intento, setIntento] = useState(0);

  useEffect(() => {
    const control = new AbortController();
    setCargando(true);
    setError(null);
    catalogoApi
      .buscar({ ...filtros, tamano: PAGINA_TAMANO }, { signal: control.signal })
      .then(setResultado)
      .catch((e) => e.codigo !== 'CANCELADA' && setError(e))
      .finally(() => !control.signal.aborted && setCargando(false));
    return () => control.abort();
  }, [filtros, intento]);

  const actualizar = (cambios) => {
    const nuevo = { ...filtros, ...cambios };
    const p = new URLSearchParams();
    Object.entries(nuevo).forEach(([k, v]) => {
      if (k === 'pagina' && !v) return;
      if (Array.isArray(v)) v.forEach((x) => p.append(k, x));
      else if (v !== '' && v !== null && v !== undefined) p.set(k, v);
    });
    setSp(p);
  };

  const cambiarPagina = (p) => {
    actualizar({ pagina: p });
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const activos = [filtros.tipo, filtros.precioMin, filtros.precioMax].filter(Boolean).length + filtros.servicios.length;
  const lista = resultado?.contenido || [];
  const hayBusqueda = filtros.ciudad || filtros.llegada || filtros.capacidad || activos > 0;

  return (
    <div>
      <section className="border-b border-black/[0.04] bg-gradient-to-b from-canvas to-white">
        <div className="container-app pb-8 pt-10 text-center sm:pt-12">
          <h1 className="mx-auto max-w-3xl text-[34px] font-semibold leading-[1.05] tracking-tight text-ink sm:text-[46px]">
            {filtros.ciudad ? `Alojamientos en ${filtros.ciudad}` : 'Explora todos los alojamientos'}
          </h1>
          <p className="mx-auto mt-3 max-w-xl text-[16px] text-ink-muted sm:text-[17px]">Filtra por fechas, huéspedes, tipo, precio y servicios. Marca hasta 3 para compararlos.</p>
          <div className="mt-8">
            <SearchBar
              key={`${filtros.ciudad}|${filtros.llegada}|${filtros.salida}|${filtros.capacidad}`}
              valores={filtros}
              filtrosActivos={activos}
              alAbrirFiltros={() => setFiltrosAbiertos(true)}
              alBuscar={(v) => actualizar({ ...v, pagina: 0 })}
            />
          </div>
        </div>
        <div className="container-app no-scrollbar flex gap-2 overflow-x-auto pb-5">
          <Chip activo={!filtros.tipo} onClick={() => actualizar({ tipo: '', pagina: 0 })}>
            Todos
          </Chip>
          {TIPOS_ALOJAMIENTO.map((t) => (
            <Chip key={t} activo={filtros.tipo === t} onClick={() => actualizar({ tipo: t, pagina: 0 })}>
              {TIPO_LABEL[t]}
            </Chip>
          ))}
        </div>
      </section>

      <section className="container-app pt-8">
        <div className="mx-auto mb-6 flex max-w-[1240px] flex-wrap items-center justify-between gap-3">
          <p className="text-[15px] text-ink-muted">
            {cargando ? 'Buscando alojamientos…' : resultado ? `${plural(resultado.totalElementos, 'alojamiento')}${filtros.ciudad ? ` en ${filtros.ciudad}` : ''}` : ''}
          </p>
          {hayBusqueda && (
            <Button variante="link" tamano="sm" onClick={() => setSp(new URLSearchParams())}>
              Limpiar búsqueda
            </Button>
          )}
        </div>

        {error ? (
          <ErrorState error={error} alReintentar={() => setIntento((x) => x + 1)} />
        ) : cargando ? (
          <div className={GRID_CENTRADO}>
            {Array.from({ length: 8 }).map((_, i) => (
              <ListingCardSkeleton key={i} />
            ))}
          </div>
        ) : lista.length === 0 ? (
          <EmptyState
            icono={SearchX}
            titulo="No encontramos alojamientos"
            descripcion="Solo mostramos publicaciones activas y libres en tus fechas. Prueba con otra ciudad, otras fechas o menos filtros."
            accion={
              <Button variante="outline" onClick={() => setSp(new URLSearchParams())}>
                Ver todo el catálogo
              </Button>
            }
          />
        ) : (
          <>
            <div className={GRID_CENTRADO}>
              {lista.map((a) => (
                <ListingCard key={a.id} alojamiento={a} llegada={filtros.llegada} salida={filtros.salida} numHuespedes={filtros.capacidad} />
              ))}
            </div>
            <Paginacion pagina={resultado.pagina} totalPaginas={resultado.totalPaginas} alCambiar={cambiarPagina} />
            {resultado.totalElementos > PAGINA_TAMANO && (
              <p className="mt-3 text-center text-xs text-ink-faint">
                Mostrando {numero(lista.length)} de {numero(resultado.totalElementos)}
              </p>
            )}
          </>
        )}
      </section>

      <FiltersModal
        abierto={filtrosAbiertos}
        alCerrar={() => setFiltrosAbiertos(false)}
        valores={filtros}
        servicios={servicios}
        alAplicar={(f) => {
          actualizar({ tipo: f.tipo, precioMin: f.precioMin, precioMax: f.precioMax, servicios: f.servicios, pagina: 0 });
          setFiltrosAbiertos(false);
        }}
      />
      <CompareBar />
    </div>
  );
}

function Chip({ activo, children, onClick }) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={`shrink-0 rounded-full border px-4 py-2 text-[13.5px] font-medium transition ${
        activo ? 'border-ink bg-ink text-white' : 'border-line bg-white text-ink-soft hover:border-ink/40 hover:text-ink'
      }`}
    >
      {children}
    </button>
  );
}
