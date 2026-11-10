import { CircleCheck, Clock, CircleX, CloudOff } from 'lucide-react';
import { Button, Modal } from '../ui';
import { dinero, rangoFechas } from '../../utils/format';

/**
 * Resultado de POST /reservas o POST /reservas/{id}/pagar según el código HTTP del backend:
 * 201 confirmada · 202 enviada al anfitrión · 402 pago rechazado · 503 pasarela no disponible.
 */
export default function ResultadoReservaModal({ resultado, alCerrar }) {
  if (!resultado) return null;
  const { _status: status, estado } = resultado;

  let vista;
  if (estado === 'CONFIRMADA') {
    vista = { icono: CircleCheck, color: 'text-emerald-600 bg-emerald-50', titulo: '¡Reserva confirmada!', texto: 'El pago fue aprobado y las fechas quedaron bloqueadas para ti.' };
  } else if (estado === 'EN_REVISION' || status === 202) {
    vista = { icono: Clock, color: 'text-amber-600 bg-amber-50', titulo: 'Solicitud enviada', texto: 'El anfitrión debe aprobarla. Te avisaremos para que completes el pago.' };
  } else if (status === 503 || resultado.estadoPago === 'NO_DISPONIBLE') {
    vista = { icono: CloudOff, color: 'text-ink-muted bg-canvas', titulo: 'La pasarela no respondió', texto: 'No se realizó ningún cobro y las fechas se liberaron. Intenta de nuevo en unos minutos.' };
  } else {
    vista = { icono: CircleX, color: 'text-brand bg-brand-soft', titulo: 'El pago fue rechazado', texto: 'No se realizó ningún cobro y las fechas se liberaron. Puedes intentarlo con otro método de pago.' };
  }
  const Icono = vista.icono;

  return (
    <Modal
      abierto
      alCerrar={alCerrar}
      ancho="max-w-md"
      pie={
        <>
          <Button variante="ghost" onClick={alCerrar}>
            Cerrar
          </Button>
          <Button to={`/reservas/${resultado.id}`}>Ver reserva</Button>
        </>
      }
    >
      <div className="flex flex-col items-center pb-2 text-center">
        <span className={`mb-4 flex h-16 w-16 items-center justify-center rounded-full ${vista.color}`}>
          <Icono className="h-8 w-8" strokeWidth={1.7} />
        </span>
        <h2 className="text-2xl font-semibold">{vista.titulo}</h2>
        <p className="mt-2 text-[15px] text-ink-muted">{vista.texto}</p>
        {resultado.mensaje && <p className="mt-3 rounded-xl bg-canvas px-3 py-2 text-[13px] text-ink-soft">{resultado.mensaje}</p>}
        <dl className="mt-5 grid w-full grid-cols-2 gap-3 text-left text-sm">
          <div className="rounded-2xl bg-canvas p-3">
            <dt className="text-xs text-ink-muted">Reserva</dt>
            <dd className="font-semibold">#{resultado.id}</dd>
          </div>
          <div className="rounded-2xl bg-canvas p-3">
            <dt className="text-xs text-ink-muted">Total</dt>
            <dd className="font-semibold">{dinero(resultado.total)}</dd>
          </div>
          <div className="col-span-2 rounded-2xl bg-canvas p-3">
            <dt className="text-xs text-ink-muted">{resultado.tituloAlojamiento || 'Fechas'}</dt>
            <dd className="font-medium">{rangoFechas(resultado.fechaLlegada, resultado.fechaSalida)}</dd>
          </div>
        </dl>
      </div>
    </Modal>
  );
}
