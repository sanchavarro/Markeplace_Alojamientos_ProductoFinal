import { useEffect, useMemo, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { Star, ShieldCheck, Zap, UserCheck, Minus, Plus } from 'lucide-react';
import { catalogoApi, reservasApi } from '../../api';
import { useAuth } from '../../context/AuthContext';
import { useToast } from '../../context/ToastContext';
import { useNotificaciones } from '../../context/NotificationsContext';
import { useDebounce } from '../../hooks/useDebounce';
import { useIdempotencia } from '../../hooks/useIdempotencia';
import { dinero, hoyIso, sumarDias, calificacion, plural } from '../../utils/format';
import { validarFechasEstadia } from '../../utils/validators';
import { MODO_LABEL, ROLES } from '../../utils/constants';
import { fotosDeAlojamiento } from '../../utils/images';
import { Button, Spinner } from '../ui';
import { CheckoutModal } from '../reservas/Pasarela';
import ResultadoReservaModal from '../reservas/ResultadoReservaModal';

/**
 * Tarjeta sticky de cotización y reserva (estilo panel derecho de Airbnb, acabado glass).
 *  - Cotiza en tiempo real con POST /cotizaciones (HU-23): el total siempre lo calcula el servidor.
 *  - Valida fechas (HU-25) y capacidad antes de cotizar.
 *  - Reserva con POST /reservas + Idempotency-Key (HU-26, HU-28, HU-31).
 */
export default function BookingCard({ alojamiento, llegada, salida, huespedes, alCambiar, rangoOk }) {
  const { autenticado, rol } = useAuth();
  const toast = useToast();
  const { invalidar } = useNotificaciones();
  const navegar = useNavigate();
  const ubicacion = useLocation();
  const idem = useIdempotencia();

  const [extras, setExtras] = useState([]);
  const [checkout, setCheckout] = useState(false);
  const [cotizacion, setCotizacion] = useState(null);
  const [cotizando, setCotizando] = useState(false);
  const [errorCotizacion, setErrorCotizacion] = useState(null);
  const [reservando, setReservando] = useState(false);
  const [resultado, setResultado] = useState(null);

  const errorFechas = llegada || salida ? validarFechasEstadia(llegada, salida) : null;
  const errorHuespedes = huespedes > alojamiento.capacidad ? `Este alojamiento admite máximo ${alojamiento.capacidad} huéspedes.` : null;
  const listo = Boolean(llegada && salida && !errorFechas && !errorHuespedes);

  const clave = useDebounce(useMemo(() => JSON.stringify({ llegada, salida, huespedes, extras }), [llegada, salida, huespedes, extras]), 300);

  useEffect(() => {
    // Una cotización nueva = un intento nuevo: se descarta la clave de idempotencia previa.
    idem.reiniciar();
    if (!listo) {
      setCotizacion(null);
      setErrorCotizacion(null);
      return undefined;
    }
    const control = new AbortController();
    setCotizando(true);
    setErrorCotizacion(null);
    catalogoApi
      .cotizar(
        { alojamientoId: alojamiento.id, fechaLlegada: llegada, fechaSalida: salida, numHuespedes: huespedes, serviciosAdicionales: extras },
        { signal: control.signal },
      )
      .then(setCotizacion)
      .catch((e) => {
        if (e.codigo === 'CANCELADA') return;
        setCotizacion(null);
        setErrorCotizacion(e.message);
      })
      .finally(() => !control.signal.aborted && setCotizando(false));
    return () => control.abort();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [clave]);

  const alternarExtra = (id) => setExtras((l) => (l.includes(id) ? l.filter((x) => x !== id) : [...l, id]));

  /** Abre el checkout (revisión del viaje + pasarela de pago). */
  const abrirCheckout = () => {
    if (!autenticado) {
      navegar('/login', { state: { desde: `${ubicacion.pathname}${ubicacion.search}` } });
      return;
    }
    if (!listo || !cotizacion) return;
    if (!rangoOk) {
      toast.aviso('Algunas noches de ese rango no están disponibles. Revisa el calendario.');
      return;
    }
    setCheckout(true);
  };

  /** metodoPago = token de prueba generado por la pasarela (null en reservas con aprobación). */
  const reservar = async (metodoPago) => {
    if (!listo || reservando) return;
    setReservando(true);
    const claveIdem = idem.obtener();
    try {
      const r = await reservasApi.crear(
        { alojamientoId: alojamiento.id, fechaLlegada: llegada, fechaSalida: salida, numHuespedes: huespedes, serviciosAdicionales: extras, metodoPago },
        claveIdem,
      );
      idem.reiniciar();
      setCheckout(false);
      setResultado(r);
      invalidar();
    } catch (e) {
      // 402 (pago rechazado) y 503 (pasarela caída) traen un ReservaResponse en el cuerpo.
      if (e.datos?.id) {
        idem.reiniciar();
        setCheckout(false);
        setResultado({ ...e.datos, _status: e.status });
        invalidar();
      } else {
        if (!idem.esIncierto(e)) idem.reiniciar();
        toast.error(idem.esIncierto(e) ? `${e.message} Puedes reintentar con seguridad: no se cobrará dos veces.` : e.message);
      }
    } finally {
      setReservando(false);
    }
  };

  const esHuesped = rol === ROLES.HUESPED;
  const puedeReservar = !autenticado || esHuesped;
  const nota = calificacion(alojamiento.promedioCalificacion);
  const adicionales = alojamiento.serviciosAdicionales || [];

  return (
    <>
      <div className="glass rounded-3xl border border-line/60 p-6 shadow-lift">
        <div className="flex items-baseline justify-between gap-3">
          <p className="text-[22px] font-semibold tracking-tight">
            {dinero(alojamiento.precioNoche)} <span className="text-[15px] font-normal text-ink-muted">noche</span>
          </p>
          {nota && (
            <span className="flex items-center gap-1 text-[13.5px]">
              <Star className="h-3.5 w-3.5 fill-ink" /> {nota} <span className="text-ink-muted">· {plural(alojamiento.totalResenas, 'reseña')}</span>
            </span>
          )}
        </div>

        <div className="mt-5 overflow-hidden rounded-2xl border border-line">
          <div className="grid grid-cols-2 divide-x divide-line">
            <label className="block px-3.5 py-2.5">
              <span className="block text-[10.5px] font-bold uppercase tracking-wide">Llegada</span>
              <input
                type="date"
                min={hoyIso()}
                value={llegada}
                onChange={(e) => {
                  const v = e.target.value;
                  alCambiar({ llegada: v, salida: salida && salida > v ? salida : v ? sumarDias(v, 1) : '' });
                }}
                className="w-full bg-transparent text-[14px] outline-none"
              />
            </label>
            <label className="block px-3.5 py-2.5">
              <span className="block text-[10.5px] font-bold uppercase tracking-wide">Salida</span>
              <input type="date" min={llegada ? sumarDias(llegada, 1) : sumarDias(hoyIso(), 1)} value={salida} onChange={(e) => alCambiar({ salida: e.target.value })} className="w-full bg-transparent text-[14px] outline-none" />
            </label>
          </div>
          <div className="flex items-center justify-between border-t border-line px-3.5 py-2.5">
            <div>
              <span className="block text-[10.5px] font-bold uppercase tracking-wide">Huéspedes</span>
              <span className="text-[14px]">{plural(huespedes, 'huésped', 'huéspedes')}</span>
            </div>
            <div className="flex items-center gap-2">
              <button type="button" disabled={huespedes <= 1} onClick={() => alCambiar({ huespedes: huespedes - 1 })} className="flex h-8 w-8 items-center justify-center rounded-full border border-line disabled:opacity-30" aria-label="Menos huéspedes">
                <Minus className="h-3.5 w-3.5" />
              </button>
              <button type="button" disabled={huespedes >= alojamiento.capacidad} onClick={() => alCambiar({ huespedes: huespedes + 1 })} className="flex h-8 w-8 items-center justify-center rounded-full border border-line disabled:opacity-30" aria-label="Más huéspedes">
                <Plus className="h-3.5 w-3.5" />
              </button>
            </div>
          </div>
        </div>

        {adicionales.length > 0 && (
          <div className="mt-4">
            <p className="mb-2 text-[13px] font-semibold">Servicios adicionales</p>
            <div className="space-y-1.5">
              {adicionales.map((s) => (
                <label key={s.id} className="flex cursor-pointer items-center justify-between gap-3 rounded-xl px-1 py-1 text-[14px] hover:bg-black/[0.03]">
                  <span className="flex items-center gap-2.5">
                    <input type="checkbox" checked={extras.includes(s.id)} onChange={() => alternarExtra(s.id)} className="h-4 w-4 rounded accent-ink" />
                    {s.nombre}
                  </span>
                  <span className="text-ink-muted">{dinero(s.precio)}</span>
                </label>
              ))}
            </div>
          </div>
        )}

        <Button
          completo
          tamano="lg"
          variante="brand"
          className="mt-5"
          onClick={abrirCheckout}
          cargando={reservando}
          disabled={autenticado && (!puedeReservar || !listo || cotizando || !cotizacion)}
        >
          {!autenticado ? 'Inicia sesión para reservar' : alojamiento.modoReserva === 'APROBACION' ? 'Solicitar reserva' : 'Reservar'}
        </Button>

        <p className="mt-3 flex items-center justify-center gap-1.5 text-center text-[12.5px] text-ink-muted">
          {alojamiento.modoReserva === 'INMEDIATA' ? <Zap className="h-3.5 w-3.5" /> : <UserCheck className="h-3.5 w-3.5" />}
          {alojamiento.modoReserva === 'INMEDIATA' ? 'Aún no se te cobrará: primero revisas y pagas en la pasarela.' : 'No se cobra nada hasta que el anfitrión apruebe.'}
        </p>
        {autenticado && !esHuesped && <p className="mt-2 text-center text-[12.5px] text-amber-700">Solo las cuentas de huésped pueden reservar.</p>}

        {(errorFechas || errorHuespedes || errorCotizacion) && (llegada || salida) && (
          <p className="mt-4 rounded-xl bg-brand-soft px-3 py-2 text-[13px] text-[#A1192C]" role="alert">
            {errorFechas || errorHuespedes || errorCotizacion}
          </p>
        )}
        {listo && !rangoOk && !errorCotizacion && (
          <p className="mt-4 rounded-xl bg-amber-50 px-3 py-2 text-[13px] text-amber-800">Algunas noches de este rango no aparecen disponibles en el calendario.</p>
        )}

        {cotizando && !cotizacion && (
          <div className="mt-5 flex items-center justify-center gap-2 text-sm text-ink-muted">
            <Spinner className="h-4 w-4" /> Calculando el precio…
          </div>
        )}

        {cotizacion && (
          <div className={`mt-5 space-y-2.5 text-[14.5px] transition-opacity ${cotizando ? 'opacity-50' : ''}`}>
            <Linea texto={`${dinero(cotizacion.precioNoche)} x ${plural(cotizacion.noches, 'noche')}`} valor={cotizacion.precioBase} />
            {Number(cotizacion.tarifaLimpieza) > 0 && <Linea texto="Tarifa de limpieza" valor={cotizacion.tarifaLimpieza} />}
            {(cotizacion.detalleServicios || []).map((s) => (
              <Linea key={s.id} texto={s.nombre} valor={s.precio} />
            ))}
            <Linea texto="Tarifa de servicio" valor={cotizacion.tarifaServicio} />
            {Number(cotizacion.descuento) > 0 && <Linea texto="Descuento" valor={-Number(cotizacion.descuento)} destacado />}
            <div className="flex justify-between border-t border-line pt-3 text-[16px] font-semibold">
              <span>Total ({cotizacion.moneda || 'COP'})</span>
              <span>{dinero(cotizacion.total, cotizacion.moneda)}</span>
            </div>
            <p className="flex items-center gap-1.5 pt-1 text-[12px] text-ink-faint">
              <ShieldCheck className="h-3.5 w-3.5" /> Total calculado por el servidor · {MODO_LABEL[alojamiento.modoReserva]}
            </p>
          </div>
        )}
      </div>

      <CheckoutModal
        abierto={checkout}
        alCerrar={() => !reservando && setCheckout(false)}
        alojamiento={alojamiento}
        foto={fotosDeAlojamiento(alojamiento)[0]}
        llegada={llegada}
        salida={salida}
        huespedes={huespedes}
        cotizacion={cotizacion}
        extras={extras}
        procesando={reservando}
        alConfirmar={reservar}
      />
      <ResultadoReservaModal resultado={resultado} alCerrar={() => setResultado(null)} />
    </>
  );
}

function Linea({ texto, valor, destacado }) {
  return (
    <div className="flex justify-between gap-4">
      <span className="text-ink-soft underline decoration-line underline-offset-4">{texto}</span>
      <span className={destacado ? 'text-emerald-700' : ''}>{dinero(valor)}</span>
    </div>
  );
}
