import { useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { CalendarX2, CreditCard, Star, CircleX, ChevronRight } from 'lucide-react';
import { reservasApi } from '../../api';
import { useAsync } from '../../hooks/useAsync';
import { useMiniaturas } from '../../hooks/useMiniaturas';
import { Button, EmptyState, ErrorState, EstadoBadge, Paginacion, SafeImage, SectionTitle, Skeleton } from '../../components/ui';
import { CancelacionModal, PagoModal, ResenaModal } from '../../components/reservas/AccionesReserva';
import { ESTADO_RESERVA, ESTADOS_CANCELABLES as CANCELABLES } from '../../utils/constants';
import { dinero, hoyIso, plural, rangoFechas } from '../../utils/format';

const PESTANAS = [
  { id: 'proximas', label: 'Próximas' },
  { id: 'pendientes', label: 'Pendientes' },
  { id: 'pasadas', label: 'Pasadas y canceladas' },
];

const clasificar = (r) => {
  if (['EN_REVISION', 'PENDIENTE_PAGO'].includes(r.estado)) return 'pendientes';
  if (r.estado === 'CONFIRMADA' && r.fechaSalida >= hoyIso()) return 'proximas';
  return 'pasadas';
};

/** Historial del huésped (HU-29): actuales y pasadas, con miniatura, fechas y estado. */
export default function MisReservasPage() {
  const [pagina, setPagina] = useState(0);
  const [pestana, setPestana] = useState('proximas');
  const [cancelar, setCancelar] = useState(null);
  const [pagar, setPagar] = useState(null);
  const [resenar, setResenar] = useState(null);
  const [resenadas, setResenadas] = useState(() => new Set());

  const { data, error, cargando, recargar } = useAsync(() => reservasApi.mias({ pagina, tamano: 50 }), [pagina]);
  const reservas = useMemo(() => data?.contenido || [], [data]);
  const miniaturas = useMiniaturas(reservas.map((r) => r.alojamientoId));
  const visibles = reservas.filter((r) => clasificar(r) === pestana);
  const conteo = (id) => reservas.filter((r) => clasificar(r) === id).length;

  return (
    <div className="container-app py-10">
      <SectionTitle titulo="Mis viajes" subtitulo="Todas tus reservas, su estado y las acciones disponibles." />

      <div className="no-scrollbar mb-8 flex gap-2 overflow-x-auto">
        {PESTANAS.map((p) => (
          <button
            key={p.id}
            type="button"
            onClick={() => setPestana(p.id)}
            className={`shrink-0 rounded-full px-4 py-2 text-[14px] font-medium transition ${pestana === p.id ? 'bg-ink text-white' : 'bg-canvas text-ink-soft hover:bg-[#EBEBEF]'}`}
          >
            {p.label}
            {data && <span className={`ml-1.5 ${pestana === p.id ? 'text-white/70' : 'text-ink-faint'}`}>{conteo(p.id)}</span>}
          </button>
        ))}
      </div>

      {error ? (
        <ErrorState error={error} alReintentar={recargar} />
      ) : cargando ? (
        <div className="space-y-4">
          {[0, 1, 2].map((i) => (
            <Skeleton key={i} className="h-36 w-full rounded-3xl" />
          ))}
        </div>
      ) : visibles.length === 0 ? (
        <EmptyState
          icono={CalendarX2}
          titulo={pestana === 'proximas' ? 'Aún no tienes viajes próximos' : 'Nada por aquí'}
          descripcion="Cuando reserves un alojamiento lo verás en esta sección."
          accion={<Button to="/">Explorar alojamientos</Button>}
        />
      ) : (
        <div className="space-y-4">
          {visibles.map((r) => {
            const mini = miniaturas[r.alojamientoId];
            return (
              <article key={r.id} className="flex flex-col gap-5 rounded-3xl border border-line/50 bg-white p-4 shadow-soft transition hover:shadow-lift sm:flex-row sm:items-center">
                <Link to={`/reservas/${r.id}`} className="shrink-0">
                  <SafeImage src={mini?.foto} semilla={r.alojamientoId} alt={r.tituloAlojamiento} className="h-40 w-full rounded-2xl sm:h-28 sm:w-40" />
                </Link>
                <div className="min-w-0 flex-1">
                  <div className="flex flex-wrap items-center gap-2">
                    <EstadoBadge estado={r.estado} mapa={ESTADO_RESERVA} />
                    {r.requiereRevision && <span className="text-xs text-amber-700">En revisión administrativa</span>}
                  </div>
                  <h3 className="mt-2 line-clamp-1 text-[17px] font-semibold">{r.tituloAlojamiento}</h3>
                  <p className="text-[14px] text-ink-muted">
                    {mini?.ciudad ? `${mini.ciudad} · ` : ''}
                    {rangoFechas(r.fechaLlegada, r.fechaSalida)} · {plural(r.noches, 'noche')} · {plural(r.numHuespedes, 'huésped', 'huéspedes')}
                  </p>
                  <p className="mt-1 text-[15px] font-semibold">{dinero(r.total)}</p>
                </div>
                <div className="flex flex-wrap gap-2 sm:flex-col sm:items-end">
                  {r.estado === 'PENDIENTE_PAGO' && (
                    <Button tamano="sm" variante="brand" icono={CreditCard} onClick={() => setPagar(r)}>
                      Pagar
                    </Button>
                  )}
                  {CANCELABLES.includes(r.estado) && r.fechaLlegada > hoyIso() && (
                    <Button tamano="sm" variante="danger" icono={CircleX} onClick={() => setCancelar(r)}>
                      Cancelar
                    </Button>
                  )}
                  {r.estado === 'COMPLETADA' && !resenadas.has(r.id) && (
                    <Button tamano="sm" variante="outline" icono={Star} onClick={() => setResenar(r)}>
                      Calificar
                    </Button>
                  )}
                  <Button tamano="sm" variante="ghost" to={`/reservas/${r.id}`}>
                    Detalle <ChevronRight className="h-4 w-4" />
                  </Button>
                </div>
              </article>
            );
          })}
        </div>
      )}

      <Paginacion pagina={data?.pagina || 0} totalPaginas={data?.totalPaginas || 0} alCambiar={setPagina} />

      <CancelacionModal reserva={cancelar} alCerrar={() => setCancelar(null)} alCancelar={recargar} />
      {pagar && <PagoModal reserva={pagar} alCerrar={() => setPagar(null)} alPagar={recargar} />}
      <ResenaModal
        reserva={resenar}
        alCerrar={() => setResenar(null)}
        alPublicar={() => setResenadas((s) => new Set(s).add(resenar.id))}
      />
    </div>
  );
}
