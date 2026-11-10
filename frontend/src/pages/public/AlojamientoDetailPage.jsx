import { useEffect, useMemo } from 'react';
import { Link, useParams, useSearchParams } from 'react-router-dom';
import { MapPin, Users, House, Zap, UserCheck, ShieldCheck, Check, ScrollText, Star, ChevronLeft, Pencil } from 'lucide-react';
import { alojamientosApi } from '../../api';
import { useAuth } from '../../context/AuthContext';
import { useAsync } from '../../hooks/useAsync';
import { BentoGallery } from '../../components/listings/Gallery';
import BookingCard from '../../components/listings/BookingCard';
import AvailabilityCalendar, { rangoDisponible } from '../../components/listings/AvailabilityCalendar';
import ReviewsSection from '../../components/listings/ReviewsSection';
import { Badge, Button, ErrorState, EstadoBadge, Skeleton } from '../../components/ui';
import { ESTADO_ALOJAMIENTO, MODO_LABEL, ROLES, TIPO_LABEL } from '../../utils/constants';
import { calificacion, hoyIso, plural, sumarDias, iniciales, nochesEntre } from '../../utils/format';
import { fotosDeAlojamiento } from '../../utils/images';

/** Detalle de la publicación (HU-15, HU-14, HU-17, HU-23, HU-40). */
export default function AlojamientoDetailPage() {
  const { id } = useParams();
  const [sp, setSp] = useSearchParams();
  const { usuario, rol } = useAuth();

  const { data: a, error, cargando, recargar } = useAsync(() => alojamientosApi.detalle(id), [id, usuario?.id]);
  const { data: periodos, recargar: recargarCalendario } = useAsync(
    () => alojamientosApi.calendario(id, { desde: hoyIso(), hasta: sumarDias(hoyIso(), 365) }),
    [id],
    { inicial: [] },
  );

  const llegada = sp.get('llegada') || '';
  const salida = sp.get('salida') || '';
  const huespedes = Math.max(1, Number(sp.get('huespedes') || 1));

  const cambiar = (cambios) => {
    const p = new URLSearchParams(sp);
    const v = { llegada, salida, huespedes, ...cambios };
    ['llegada', 'salida'].forEach((k) => (v[k] ? p.set(k, v[k]) : p.delete(k)));
    p.set('huespedes', String(v.huespedes));
    setSp(p, { replace: true });
  };

  // Al volver a la pestaña se refresca el calendario: refleja de inmediato reservas y cancelaciones (HU-17).
  useEffect(() => {
    const alVolver = () => document.visibilityState === 'visible' && recargarCalendario();
    document.addEventListener('visibilitychange', alVolver);
    return () => document.removeEventListener('visibilitychange', alVolver);
  }, [recargarCalendario]);

  const fotos = useMemo(() => (a ? fotosDeAlojamiento(a, 5) : []), [a]);
  const rangoOk = rangoDisponible(llegada, salida, periodos || []);

  if (error) {
    return (
      <div className="container-app py-16">
        <ErrorState error={error.status === 404 ? { message: 'Este alojamiento no existe o no está publicado.' } : error} alReintentar={error.status === 404 ? undefined : recargar} />
        <div className="mt-6 text-center">
          <Button variante="outline" to="/">
            Volver al catálogo
          </Button>
        </div>
      </div>
    );
  }

  if (cargando || !a) return <DetalleSkeleton />;

  const esDueno = usuario?.id === a.anfitrionId;
  const nota = calificacion(a.promedioCalificacion);

  return (
    <div className="container-app pb-16 pt-6">
      <Link to="/" className="mb-4 inline-flex items-center gap-1 text-sm text-ink-muted hover:text-ink">
        <ChevronLeft className="h-4 w-4" /> Volver
      </Link>

      <div className="mb-5 flex flex-wrap items-end justify-between gap-3">
        <div>
          <h1 className="text-[28px] font-semibold leading-tight tracking-tight sm:text-[32px]">{a.titulo}</h1>
          <p className="mt-1.5 flex flex-wrap items-center gap-x-2 gap-y-1 text-[15px] text-ink-soft">
            {nota && (
              <a href="#resenas" className="flex items-center gap-1 font-medium text-ink underline-offset-4 hover:underline">
                <Star className="h-4 w-4 fill-ink" /> {nota} · {plural(a.totalResenas, 'reseña')}
              </a>
            )}
            {nota && <span>·</span>}
            <span className="flex items-center gap-1">
              <MapPin className="h-4 w-4" /> {a.ciudad}
            </span>
          </p>
        </div>
        {(esDueno || rol === ROLES.ADMIN) && (
          <div className="flex items-center gap-2">
            <EstadoBadge estado={a.estado} mapa={ESTADO_ALOJAMIENTO} />
            {esDueno && (
              <Button variante="outline" tamano="sm" icono={Pencil} to={`/anfitrion/alojamientos/${a.id}`}>
                Gestionar
              </Button>
            )}
          </div>
        )}
      </div>

      <BentoGallery fotos={fotos} semilla={a.id} titulo={a.titulo} />

      <div className="mt-10 grid gap-12 lg:grid-cols-[minmax(0,1fr)_380px]">
        <div className="min-w-0">
          <div className="flex items-start justify-between gap-6 border-b border-line/70 pb-8">
            <div>
              <h2 className="text-[22px] font-semibold">
                {TIPO_LABEL[a.tipo] || a.tipo} completo en {a.ciudad}
              </h2>
              <p className="mt-1 text-[15px] text-ink-soft">
                Hasta {plural(a.capacidad, 'huésped', 'huéspedes')} · {a.direccion}
              </p>
            </div>
            <span className="flex h-14 w-14 shrink-0 items-center justify-center rounded-full bg-ink text-lg font-semibold text-white" title={a.anfitrionNombre}>
              {iniciales(a.anfitrionNombre)}
            </span>
          </div>

          <ul className="space-y-6 border-b border-line/70 py-8">
            <Destacado icono={a.modoReserva === 'INMEDIATA' ? Zap : UserCheck} titulo={MODO_LABEL[a.modoReserva]}>
              {a.modoReserva === 'INMEDIATA' ? 'Tu reserva se confirma en cuanto se aprueba el pago.' : 'El anfitrión revisa la solicitud y luego completas el pago.'}
            </Destacado>
            <Destacado icono={House} titulo={`Anfitrión: ${a.anfitrionNombre || 'Anfitrión verificado'}`}>
              Publicación revisada por el equipo de la plataforma.
            </Destacado>
            {a.politica && (
              <Destacado icono={ShieldCheck} titulo={`Cancelación ${String(a.politica.nombre || a.politica.tipo).toLowerCase()}`}>
                {a.politica.descripcion}
              </Destacado>
            )}
          </ul>

          <section className="border-b border-line/70 py-8">
            <h2 className="mb-4 text-[22px] font-semibold">Acerca de este espacio</h2>
            <p className="whitespace-pre-line text-[16px] leading-relaxed text-ink-soft">{a.descripcion}</p>
          </section>

          {a.servicios?.length > 0 && (
            <section className="border-b border-line/70 py-8">
              <h2 className="mb-5 text-[22px] font-semibold">Lo que este lugar ofrece</h2>
              <div className="grid gap-x-8 gap-y-4 sm:grid-cols-2">
                {a.servicios.map((s) => (
                  <p key={s.id} className="flex items-center gap-3 text-[15.5px]">
                    <Check className="h-5 w-5 text-ink-soft" strokeWidth={1.6} /> {s.nombre}
                  </p>
                ))}
              </div>
            </section>
          )}

          {a.reglas && (
            <section className="border-b border-line/70 py-8">
              <h2 className="mb-4 flex items-center gap-2 text-[22px] font-semibold">
                <ScrollText className="h-5 w-5" /> Reglas de la casa
              </h2>
              <p className="whitespace-pre-line text-[15.5px] leading-relaxed text-ink-soft">{a.reglas}</p>
            </section>
          )}

          <section className="border-b border-line/70 py-8">
            <h2 className="text-[22px] font-semibold">{llegada && salida ? plural(nochesEntre(llegada, salida), 'noche') + ` en ${a.ciudad}` : 'Elige tus fechas'}</h2>
            <p className="mb-6 mt-1 text-[14px] text-ink-muted">Selecciona la llegada y luego la salida. El calendario muestra la disponibilidad real.</p>
            <AvailabilityCalendar periodos={periodos || []} llegada={llegada} salida={salida} alCambiarRango={(l, s) => cambiar({ llegada: l, salida: s })} />
          </section>

          <div className="py-8">
            <ReviewsSection alojamientoId={a.id} />
          </div>
        </div>

        <aside className="lg:relative">
          <div className="lg:sticky lg:top-24">
            {a.estado !== 'ACTIVA' ? (
              <div className="rounded-3xl border border-line/60 bg-canvas p-6 text-center">
                <Badge tono="warning">{ESTADO_ALOJAMIENTO[a.estado]?.label || a.estado}</Badge>
                <p className="mt-3 text-[14.5px] text-ink-soft">Esta publicación no está activa, por eso no acepta reservas. Solo tú y el administrador pueden verla.</p>
              </div>
            ) : (
              <BookingCard alojamiento={a} llegada={llegada} salida={salida} huespedes={huespedes} alCambiar={cambiar} rangoOk={rangoOk} />
            )}
          </div>
        </aside>
      </div>
    </div>
  );
}

function Destacado({ icono: Icono, titulo, children }) {
  return (
    <li className="flex gap-5">
      <Icono className="mt-0.5 h-6 w-6 shrink-0 text-ink" strokeWidth={1.5} />
      <div>
        <p className="text-[15.5px] font-semibold">{titulo}</p>
        <p className="mt-0.5 text-[14.5px] text-ink-muted">{children}</p>
      </div>
    </li>
  );
}

function DetalleSkeleton() {
  return (
    <div className="container-app pb-16 pt-10" aria-busy="true">
      <Skeleton className="h-8 w-2/3" />
      <Skeleton className="mt-3 h-4 w-1/3" />
      <div className="mt-6 grid h-[min(56vh,520px)] grid-cols-4 grid-rows-2 gap-2 overflow-hidden rounded-3xl">
        <Skeleton className="col-span-4 row-span-2 rounded-none sm:col-span-2" />
        {[0, 1, 2, 3].map((i) => (
          <Skeleton key={i} className="hidden rounded-none sm:block" />
        ))}
      </div>
      <div className="mt-10 grid gap-12 lg:grid-cols-[1fr_380px]">
        <div className="space-y-3">
          <Skeleton className="h-6 w-1/2" />
          <Skeleton className="h-4 w-1/3" />
          <Skeleton className="mt-8 h-24 w-full" />
        </div>
        <Skeleton className="h-80 w-full rounded-3xl" />
      </div>
    </div>
  );
}
