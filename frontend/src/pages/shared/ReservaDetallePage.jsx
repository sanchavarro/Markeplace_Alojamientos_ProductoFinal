import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { ChevronLeft, CreditCard, Star, CircleX, Check, X } from 'lucide-react';
import { reservasApi } from '../../api';
import { useAuth } from '../../context/AuthContext';
import { useToast } from '../../context/ToastContext';
import { useAsync } from '../../hooks/useAsync';
import { useMiniaturas } from '../../hooks/useMiniaturas';
import { Button, Card, Confirmar, ErrorState, EstadoBadge, PantallaCarga, SafeImage } from '../../components/ui';
import { CancelacionModal, PagoModal, ResenaModal } from '../../components/reservas/AccionesReserva';
import { ESTADO_RESERVA, ESTADOS_CANCELABLES, ROLES } from '../../utils/constants';
import { dinero, fechaHora, fechaLarga, hoyIso, plural } from '../../utils/format';

/** Detalle de una reserva (GET /reservas/{id}), compartido por huésped, anfitrión y administrador. */
export default function ReservaDetallePage() {
  const { id } = useParams();
  const { rol } = useAuth();
  const toast = useToast();
  const { data: r, error, cargando, recargar } = useAsync(() => reservasApi.detalle(id), [id]);
  const mini = useMiniaturas(r ? [r.alojamientoId] : [])[r?.alojamientoId];
  const [modal, setModal] = useState(null);
  const [decision, setDecision] = useState(null);
  const [enviando, setEnviando] = useState(false);

  if (error) {
    return (
      <div className="container-app py-16">
        <ErrorState error={error} alReintentar={recargar} />
      </div>
    );
  }
  if (cargando || !r) return <PantallaCarga />;

  const esHuesped = rol === ROLES.HUESPED;
  const esAnfitrion = rol === ROLES.ANFITRION;

  const decidir = async () => {
    setEnviando(true);
    try {
      if (decision === 'aprobar') await reservasApi.aprobar(r.id);
      else await reservasApi.rechazar(r.id);
      toast.exito(decision === 'aprobar' ? 'Solicitud aprobada. El huésped ya puede pagar.' : 'Solicitud rechazada.');
      setDecision(null);
      recargar();
    } catch (e) {
      toast.error(e);
    } finally {
      setEnviando(false);
    }
  };

  return (
    <div className="container-app max-w-5xl py-8">
      <Link to={esAnfitrion ? '/anfitrion/reservas' : rol === ROLES.ADMIN ? '/admin/revision' : '/mis-reservas'} className="mb-5 inline-flex items-center gap-1 text-sm text-ink-muted hover:text-ink">
        <ChevronLeft className="h-4 w-4" /> Volver
      </Link>

      <div className="grid gap-8 lg:grid-cols-[1fr_340px]">
        <div>
          <SafeImage src={mini?.foto} semilla={r.alojamientoId} alt={r.tituloAlojamiento} className="aspect-[16/8] w-full rounded-3xl" />
          <div className="mt-6 flex flex-wrap items-center gap-3">
            <EstadoBadge estado={r.estado} mapa={ESTADO_RESERVA} />
            <span className="text-sm text-ink-muted">Reserva #{r.id} · creada {fechaHora(r.creadaEn)}</span>
          </div>
          <h1 className="mt-3 text-[30px] font-semibold tracking-tight">{r.tituloAlojamiento}</h1>
          <Link to={`/alojamientos/${r.alojamientoId}`} className="text-sm font-medium text-accent hover:underline">
            Ver el alojamiento
          </Link>

          <div className="mt-8 grid gap-4 sm:grid-cols-3">
            <Dato etiqueta="Llegada" valor={fechaLarga(r.fechaLlegada)} />
            <Dato etiqueta="Salida" valor={fechaLarga(r.fechaSalida)} />
            <Dato etiqueta="Huéspedes" valor={plural(r.numHuespedes, 'huésped', 'huéspedes')} />
          </div>
          {r.huespedNombre && (esAnfitrion || rol === ROLES.ADMIN) && (
            <div className="mt-4">
              <Dato etiqueta="Huésped" valor={r.huespedNombre} />
            </div>
          )}
          {r.mensaje && <p className="mt-6 rounded-2xl bg-canvas px-4 py-3 text-[14px] text-ink-soft">{r.mensaje}</p>}
          {r.requiereRevision && (
            <p className="mt-4 rounded-2xl bg-amber-50 px-4 py-3 text-[14px] text-amber-800">
              La publicación fue bloqueada por un administrador; esta reserva está en revisión manual.
            </p>
          )}
        </div>

        <aside>
          <Card className="lg:sticky lg:top-24">
            <h2 className="text-lg font-semibold">Detalle del precio</h2>
            <div className="mt-4 space-y-2.5 text-[14.5px]">
              <Fila t={`Alojamiento · ${plural(r.noches, 'noche')}`} v={r.precioBase} />
              {Number(r.tarifaLimpieza) > 0 && <Fila t="Limpieza" v={r.tarifaLimpieza} />}
              <Fila t="Tarifa de servicio" v={r.tarifaServicio} />
              {Number(r.descuento) > 0 && <Fila t="Descuento" v={-Number(r.descuento)} />}
              <div className="flex justify-between border-t border-line pt-3 text-[16px] font-semibold">
                <span>Total</span>
                <span>{dinero(r.total)}</span>
              </div>
            </div>
            {r.estadoPago && (
              <p className="mt-4 text-xs text-ink-muted">
                Pago: <strong>{r.estadoPago}</strong>
                {r.referenciaPago && <> · Ref. {r.referenciaPago}</>}
              </p>
            )}

            <div className="mt-6 space-y-2">
              {esHuesped && r.estado === 'PENDIENTE_PAGO' && (
                <Button completo variante="brand" icono={CreditCard} onClick={() => setModal('pagar')}>
                  Pagar ahora
                </Button>
              )}
              {(esHuesped || rol === ROLES.ADMIN) && ESTADOS_CANCELABLES.includes(r.estado) && r.fechaLlegada > hoyIso() && (
                <Button completo variante="danger" icono={CircleX} onClick={() => setModal('cancelar')}>
                  Cancelar reserva
                </Button>
              )}
              {esHuesped && r.estado === 'COMPLETADA' && (
                <Button completo variante="outline" icono={Star} onClick={() => setModal('resenar')}>
                  Calificar estadía
                </Button>
              )}
              {esAnfitrion && r.estado === 'EN_REVISION' && (
                <>
                  <Button completo icono={Check} onClick={() => setDecision('aprobar')}>
                    Aprobar solicitud
                  </Button>
                  <Button completo variante="danger" icono={X} onClick={() => setDecision('rechazar')}>
                    Rechazar
                  </Button>
                </>
              )}
            </div>
          </Card>
        </aside>
      </div>

      <CancelacionModal reserva={modal === 'cancelar' ? r : null} alCerrar={() => setModal(null)} alCancelar={recargar} />
      {modal === 'pagar' && <PagoModal reserva={r} alCerrar={() => setModal(null)} alPagar={recargar} />}
      <ResenaModal reserva={modal === 'resenar' ? r : null} alCerrar={() => setModal(null)} />
      <Confirmar
        abierto={Boolean(decision)}
        alCerrar={() => setDecision(null)}
        alConfirmar={decidir}
        cargando={enviando}
        titulo={decision === 'aprobar' ? 'Aprobar solicitud' : 'Rechazar solicitud'}
        mensaje={decision === 'aprobar' ? 'El huésped recibirá una notificación para completar el pago.' : 'La solicitud se cancelará y las fechas quedarán libres.'}
        textoConfirmar={decision === 'aprobar' ? 'Aprobar' : 'Rechazar'}
        variante={decision === 'aprobar' ? 'primary' : 'brand'}
      />
    </div>
  );
}

function Dato({ etiqueta, valor }) {
  return (
    <div className="rounded-2xl bg-canvas p-4">
      <p className="text-xs font-medium text-ink-muted">{etiqueta}</p>
      <p className="mt-1 text-[15px] font-semibold">{valor}</p>
    </div>
  );
}

function Fila({ t, v }) {
  return (
    <div className="flex justify-between gap-3">
      <span className="text-ink-soft">{t}</span>
      <span>{dinero(v)}</span>
    </div>
  );
}
