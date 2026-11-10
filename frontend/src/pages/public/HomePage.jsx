import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ArrowRight, Sparkles } from 'lucide-react';
import { catalogoApi } from '../../api';
import SearchBar from '../../components/listings/SearchBar';
import ListingCard, { ListingCardSkeleton } from '../../components/listings/ListingCard';
import CompareBar from '../../components/listings/CompareBar';
import { Button, ErrorState } from '../../components/ui';
import {
  CtaAnfitrion,
  EncabezadoSeccion,
  GRID_CENTRADO,
  Preguntas,
  SeccionBeneficios,
  SeccionDestinos,
  SeccionPasos,
  SeccionTipos,
} from '../../components/home/Secciones';
import { PASOS_HUESPED } from '../../utils/contenido';
import { unsplash } from '../../utils/images';

/**
 * Página de inicio: buscador, alojamientos destacados (centrados) y secciones para descubrir
 * destinos, tipos de espacio, cómo funciona, beneficios, anfitriones y preguntas frecuentes.
 */
export default function HomePage() {
  const navegar = useNavigate();
  const [destacados, setDestacados] = useState(null);
  const [error, setError] = useState(null);
  const [intento, setIntento] = useState(0);

  useEffect(() => {
    const control = new AbortController();
    setError(null);
    catalogoApi
      .buscar({ pagina: 0, tamano: 8 }, { signal: control.signal })
      .then(setDestacados)
      .catch((e) => e.codigo !== 'CANCELADA' && setError(e));
    return () => control.abort();
  }, [intento]);

  const buscar = (v) => {
    const p = new URLSearchParams();
    Object.entries(v).forEach(([k, val]) => val && p.set(k, val));
    navegar(`/explorar${p.toString() ? `?${p}` : ''}`);
  };

  const lista = destacados?.contenido || [];

  return (
    <div>
      {/* Hero */}
      <section className="relative overflow-hidden bg-gradient-to-b from-canvas via-canvas to-white">
        <div className="container-app pb-14 pt-14 text-center sm:pt-20">
          <span className="inline-flex items-center gap-1.5 rounded-full bg-white px-3.5 py-1.5 text-[13px] font-medium text-ink-soft shadow-pill">
            <Sparkles className="h-3.5 w-3.5 text-brand" /> {destacados ? `${destacados.totalElementos} alojamientos disponibles` : 'Alojamientos verificados en toda Colombia'}
          </span>
          <h1 className="mx-auto mt-6 max-w-3xl text-[42px] font-semibold leading-[1.03] tracking-tight text-ink sm:text-[64px]">Descubre tu mejor espacio.</h1>
          <p className="mx-auto mt-5 max-w-xl text-[17px] text-ink-muted sm:text-[20px]">Casas, cabañas y apartamentos con precios claros desde el primer clic y pago seguro.</p>
          <div className="mt-10">
            <SearchBar valores={{}} alBuscar={buscar} alAbrirFiltros={() => navegar('/explorar')} />
          </div>
        </div>
      </section>

      {/* Destacados centrados */}
      <section className="container-app pb-6 pt-14">
        <EncabezadoSeccion
          antetitulo="Recomendados"
          titulo="Alojamientos destacados"
          texto="Una selección de espacios activos y listos para reservar. Toca «Comparar» para verlos lado a lado."
        />
        {error ? (
          <ErrorState error={error} alReintentar={() => setIntento((x) => x + 1)} />
        ) : (
          <div className={GRID_CENTRADO}>
            {destacados === null
              ? Array.from({ length: 8 }).map((_, i) => <ListingCardSkeleton key={i} />)
              : lista.map((a) => <ListingCard key={a.id} alojamiento={a} />)}
          </div>
        )}
        {destacados && lista.length === 0 && !error && (
          <p className="mx-auto max-w-md text-center text-[15px] text-ink-muted">Aún no hay alojamientos publicados. ¡Vuelve pronto o publica el tuyo!</p>
        )}
        <div className="mt-12 flex justify-center">
          <Button variante="outline" tamano="lg" to="/explorar">
            Ver todos los alojamientos <ArrowRight className="h-4 w-4" />
          </Button>
        </div>
      </section>

      <SeccionDestinos limite={5} />
      <SeccionTipos />
      <SeccionPasos antetitulo="Así de fácil" titulo="Reserva en cuatro pasos" texto="Sin sorpresas: sabes cuánto pagas, qué incluye y cómo cancelar antes de confirmar." pasos={PASOS_HUESPED} />
      <SeccionBeneficios />
      <CtaAnfitrion foto={unsplash('1600210492486-724fe5c67fb0', 1800)} />

      <section className="container-app py-16">
        <EncabezadoSeccion antetitulo="Resolvemos tus dudas" titulo="Preguntas frecuentes" />
        <Preguntas limite={4} />
        <div className="mt-8 flex justify-center">
          <Button variante="link" to="/ayuda">
            Ver el centro de ayuda <ArrowRight className="h-4 w-4" />
          </Button>
        </div>
      </section>

      <CompareBar />
    </div>
  );
}
