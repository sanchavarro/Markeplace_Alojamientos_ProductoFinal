import { useEffect, useState } from 'react';
import { TriangleAlert } from 'lucide-react';
import { reservasApi, resenasApi } from '../../api';
import { useToast } from '../../context/ToastContext';
import { useNotificaciones } from '../../context/NotificationsContext';
import { useIdempotencia } from '../../hooks/useIdempotencia';
import { Button, Modal, Textarea, Estrellas, Spinner, ErrorFormulario } from '../ui';
import { dinero } from '../../utils/format';
import { FormularioTarjeta, ProcesandoPago, TARJETA_VACIA, tokenizar, validarTarjeta } from './Pasarela';
import ResultadoReservaModal from './ResultadoReservaModal';

/**
 * Cancelación en dos pasos (HU-32, HU-33): primero GET /reservas/{id}/cancelacion/simulacion muestra
 * exactamente cuánto se devolvería según la política; luego POST /reservas/{id}/cancelacion confirma.
 */
export function CancelacionModal({ reserva, alCerrar, alCancelar }) {
  const toast = useToast();
  const { invalidar } = useNotificaciones();
  const [simulacion, setSimulacion] = useState(null);
  const [error, setError] = useState(null);
  const [motivo, setMotivo] = useState('');
  const [enviando, setEnviando] = useState(false);

  useEffect(() => {
    if (!reserva) return undefined;
    let activo = true;
    setSimulacion(null);
    setError(null);
    setMotivo('');
    reservasApi
      .simularCancelacion(reserva.id)
      .then((s) => activo && setSimulacion(s))
      .catch((e) => activo && setError(e));
    return () => {
      activo = false;
    };
  }, [reserva]);

  const confirmar = async () => {
    setEnviando(true);
    try {
      const r = await reservasApi.cancelar(reserva.id, motivo);
      toast.exito(
        Number(r.valorDevolucion) > 0 ? `Reserva cancelada. Se devolverán ${dinero(r.valorDevolucion)}.` : 'Reserva cancelada. Según la política no hay devolución.',
      );
      invalidar();
      alCancelar?.(r);
      alCerrar();
    } catch (e) {
      setError(e);
    } finally {
      setEnviando(false);
    }
  };

  return (
    <Modal
      abierto={Boolean(reserva)}
      alCerrar={enviando ? undefined : alCerrar}
      titulo="Cancelar reserva"
      descripcion={reserva?.tituloAlojamiento}
      pie={
        <>
          <Button variante="ghost" onClick={alCerrar} disabled={enviando}>
            Volver
          </Button>
          <Button variante="brand" onClick={confirmar} cargando={enviando} disabled={!simulacion}>
            Confirmar cancelación
          </Button>
        </>
      }
    >
      {!simulacion && !error && (
        <div className="flex items-center justify-center gap-2 py-10 text-sm text-ink-muted">
          <Spinner className="h-4 w-4" /> Calculando la devolución según la política…
        </div>
      )}
      {error && <ErrorFormulario error={error} />}
      {simulacion && (
        <div className="space-y-5">
          <div className="rounded-3xl bg-canvas p-5 text-center">
            <p className="text-[13px] font-medium text-ink-muted">Te devolveremos</p>
            <p className="mt-1 text-[40px] font-semibold tracking-tight">{dinero(simulacion.valorDevolucion)}</p>
            <p className="text-[13px] text-ink-muted">
              {simulacion.porcentajeDevolucion}% de {dinero(simulacion.totalPagado)} pagados
            </p>
          </div>
          <div className="flex gap-3 rounded-2xl border border-line/70 p-4 text-[13.5px]">
            <TriangleAlert className="mt-0.5 h-4 w-4 shrink-0 text-amber-500" />
            <div>
              <p className="font-semibold">Política {String(simulacion.politica || '').toLowerCase()}</p>
              <p className="mt-0.5 text-ink-muted">{simulacion.reglaPolitica}</p>
            </div>
          </div>
          <Textarea label="Motivo de la cancelación" opcional rows={3} maxLength={255} value={motivo} onChange={(e) => setMotivo(e.target.value)} placeholder="Cuéntanos qué cambió (opcional)" />
          <p className="text-xs text-ink-faint">Al cancelar, las fechas vuelven a quedar disponibles para otros huéspedes.</p>
        </div>
      )}
    </Modal>
  );
}

/** Pago de una reserva con aprobación ya aprobada (PENDIENTE_PAGO) con Idempotency-Key (HU-28, HU-31). */
export function PagoModal({ reserva, alCerrar, alPagar }) {
  const toast = useToast();
  const { invalidar } = useNotificaciones();
  const idem = useIdempotencia();
  const [tarjeta, setTarjeta] = useState(TARJETA_VACIA);
  const [errores, setErrores] = useState({});
  const [enviando, setEnviando] = useState(false);
  const [resultado, setResultado] = useState(null);

  // Una tarjeta distinta es un intento nuevo: nueva clave de idempotencia.
  useEffect(() => {
    idem.reiniciar();
  }, [reserva, tarjeta.numero, idem]);

  const pagar = async () => {
    if (enviando) return;
    const v = validarTarjeta(tarjeta);
    setErrores(v);
    if (Object.keys(v).length) return;
    setEnviando(true);
    try {
      const r = await reservasApi.pagar(reserva.id, tokenizar(tarjeta.numero), idem.obtener());
      idem.reiniciar();
      setResultado(r);
      invalidar();
      alPagar?.(r);
    } catch (e) {
      if (e.datos?.id) {
        idem.reiniciar();
        setResultado({ ...e.datos, _status: e.status });
        alPagar?.(e.datos);
      } else {
        if (!idem.esIncierto(e)) idem.reiniciar();
        toast.error(idem.esIncierto(e) ? `${e.message} Puedes reintentar: no se cobrará dos veces.` : e.message);
      }
    } finally {
      setEnviando(false);
    }
  };

  if (resultado) {
    return (
      <ResultadoReservaModal
        resultado={resultado}
        alCerrar={() => {
          setResultado(null);
          alCerrar();
        }}
      />
    );
  }

  return (
    <Modal
      abierto={Boolean(reserva)}
      alCerrar={enviando ? undefined : alCerrar}
      titulo={enviando ? undefined : 'Pagar reserva'}
      descripcion={enviando ? undefined : reserva?.tituloAlojamiento}
      ancho="max-w-lg"
      pie={
        <>
          <Button variante="ghost" onClick={alCerrar} disabled={enviando}>
            Cancelar
          </Button>
          <Button variante="brand" onClick={pagar} cargando={enviando}>
            Pagar {dinero(reserva?.total)}
          </Button>
        </>
      }
    >
      {enviando ? (
        <ProcesandoPago />
      ) : (
        <div className="space-y-4">
          <div className="flex items-center justify-between rounded-2xl bg-canvas px-4 py-3">
            <span className="text-[13px] text-ink-muted">Total a pagar</span>
            <span className="text-[22px] font-semibold tracking-tight">{dinero(reserva?.total)}</span>
          </div>
          <FormularioTarjeta tarjeta={tarjeta} setTarjeta={setTarjeta} errores={errores} />
          <p className="text-center text-xs text-ink-faint">Cada intento viaja con una clave de idempotencia única: un doble clic o un reintento nunca generan dos cobros.</p>
        </div>
      )}
    </Modal>
  );
}

/** Reseña con selector de estrellas (HU-38): solo reservas COMPLETADAS del propio huésped. */
export function ResenaModal({ reserva, alCerrar, alPublicar }) {
  const toast = useToast();
  const [calificacion, setCalificacion] = useState(0);
  const [comentario, setComentario] = useState('');
  const [error, setError] = useState(null);
  const [enviando, setEnviando] = useState(false);

  useEffect(() => {
    setCalificacion(0);
    setComentario('');
    setError(null);
  }, [reserva]);

  const enviar = async () => {
    if (calificacion < 1 || calificacion > 5) {
      setError('Elige una calificación de 1 a 5 estrellas.');
      return;
    }
    setEnviando(true);
    try {
      const r = await resenasApi.crear({ reservaId: reserva.id, calificacion, comentario });
      toast.exito('¡Gracias! Tu reseña fue publicada.');
      alPublicar?.(r);
      alCerrar();
    } catch (e) {
      setError(e);
    } finally {
      setEnviando(false);
    }
  };

  const etiquetas = ['', 'Muy mala', 'Mala', 'Regular', 'Muy buena', 'Excelente'];

  return (
    <Modal
      abierto={Boolean(reserva)}
      alCerrar={enviando ? undefined : alCerrar}
      titulo="Califica tu estadía"
      descripcion={reserva?.tituloAlojamiento}
      pie={
        <>
          <Button variante="ghost" onClick={alCerrar} disabled={enviando}>
            Cancelar
          </Button>
          <Button onClick={enviar} cargando={enviando}>
            Publicar reseña
          </Button>
        </>
      }
    >
      <div className="space-y-5">
        <div className="flex flex-col items-center gap-2 rounded-3xl bg-canvas py-6">
          <Estrellas valor={calificacion} alCambiar={setCalificacion} tamano="h-9 w-9" />
          <p className="h-5 text-sm font-medium text-ink-soft">{etiquetas[calificacion]}</p>
        </div>
        <Textarea label="Comentario" opcional rows={4} maxLength={1000} value={comentario} onChange={(e) => setComentario(e.target.value)} placeholder="¿Qué fue lo mejor de tu estadía?" />
        {error && <ErrorFormulario error={error} />}
      </div>
    </Modal>
  );
}
