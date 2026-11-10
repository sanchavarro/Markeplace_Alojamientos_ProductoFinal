import { useEffect, useMemo, useState } from 'react';
import { CalendarDays, CreditCard, Lock, ShieldCheck, Users, ChevronLeft, Info } from 'lucide-react';
import { Button, Input, Modal } from '../ui';
import SafeImage from '../ui/SafeImage';
import { dinero, plural, rangoFechas } from '../../utils/format';
import { MODO_LABEL } from '../../utils/constants';

/**
 * Pasarela de pago de pruebas.
 *
 * El usuario escribe una tarjeta como en cualquier checkout real (número, titular, vencimiento y CVC),
 * con validación Luhn, detección de la franquicia y formato automático. Los datos de la tarjeta NUNCA
 * salen del navegador: se convierten en un método de pago de prueba de Stripe (pm_card_visa,
 * pm_card_chargeDeclined…) que es lo único que recibe el backend, igual que haría Stripe Elements.
 */

export const TARJETAS_PRUEBA = [
  { numero: '4242 4242 4242 4242', resultado: 'Aprobada', marca: 'Visa' },
  { numero: '5555 5555 5555 4444', resultado: 'Aprobada', marca: 'Mastercard' },
  { numero: '4000 0000 0000 0002', resultado: 'Rechazada', marca: 'Visa' },
  { numero: '4000 0000 0000 9995', resultado: 'Fondos insuficientes', marca: 'Visa' },
];

const ESPECIALES = {
  '4000000000000002': 'pm_card_chargeDeclined',
  '4000000000009995': 'pm_card_chargeDeclinedInsufficientFunds',
  '4000000000000119': 'pm_simular_caida',
};

export const soloDigitos = (v) => (v || '').replace(/\D/g, '');

export const detectarMarca = (numero) => {
  const n = soloDigitos(numero);
  if (/^4/.test(n)) return 'visa';
  if (/^(5[1-5]|2(2[2-9]|[3-6]\d|7[01]|720))/.test(n)) return 'mastercard';
  if (/^3[47]/.test(n)) return 'amex';
  return null;
};

export const luhn = (numero) => {
  const n = soloDigitos(numero);
  if (n.length < 13) return false;
  let suma = 0;
  let doble = false;
  for (let i = n.length - 1; i >= 0; i -= 1) {
    let d = Number(n[i]);
    if (doble) {
      d *= 2;
      if (d > 9) d -= 9;
    }
    suma += d;
    doble = !doble;
  }
  return suma % 10 === 0;
};

const formatearNumero = (v) => {
  const n = soloDigitos(v).slice(0, 19);
  if (detectarMarca(n) === 'amex') return [n.slice(0, 4), n.slice(4, 10), n.slice(10, 15)].filter(Boolean).join(' ');
  return n.replace(/(.{4})/g, '$1 ').trim();
};

const formatearVencimiento = (v) => {
  const n = soloDigitos(v).slice(0, 4);
  return n.length > 2 ? `${n.slice(0, 2)}/${n.slice(2)}` : n;
};

/** Convierte la tarjeta en el método de pago de prueba que entiende el backend. */
export const tokenizar = (numero) => {
  const n = soloDigitos(numero);
  if (ESPECIALES[n]) return ESPECIALES[n];
  return { visa: 'pm_card_visa', mastercard: 'pm_card_mastercard', amex: 'pm_card_amex' }[detectarMarca(n)] || 'pm_card_visa';
};

export const validarTarjeta = ({ numero, titular, vencimiento, cvc }) => {
  const errores = {};
  const n = soloDigitos(numero);
  const marca = detectarMarca(n);
  if (!n) errores.numero = 'Ingresa el número de la tarjeta.';
  else if (!marca) errores.numero = 'Aceptamos Visa, Mastercard y American Express.';
  else if (!luhn(n) || n.length < (marca === 'amex' ? 15 : 16)) errores.numero = 'El número de la tarjeta no es válido.';
  if (!titular?.trim() || titular.trim().length < 3) errores.titular = 'Escribe el nombre como aparece en la tarjeta.';
  const [mm, aa] = (vencimiento || '').split('/');
  const mes = Number(mm);
  const anio = 2000 + Number(aa);
  const hoy = new Date();
  if (!mm || !aa || aa.length !== 2 || mes < 1 || mes > 12) errores.vencimiento = 'Usa el formato MM/AA.';
  else if (anio < hoy.getFullYear() || (anio === hoy.getFullYear() && mes < hoy.getMonth() + 1)) errores.vencimiento = 'La tarjeta está vencida.';
  const largoCvc = marca === 'amex' ? 4 : 3;
  if (soloDigitos(cvc).length !== largoCvc) errores.cvc = `El código tiene ${largoCvc} dígitos.`;
  return errores;
};

function LogoMarca({ marca, activa }) {
  const base = `flex h-7 w-11 items-center justify-center rounded-md border text-[9px] font-black tracking-tight transition ${activa ? 'border-ink bg-white opacity-100' : 'border-line bg-white opacity-40'}`;
  if (marca === 'visa') return <span className={`${base} italic text-[#1A1F71]`}>VISA</span>;
  if (marca === 'mastercard')
    return (
      <span className={base}>
        <span className="h-3.5 w-3.5 rounded-full bg-[#EB001B]" />
        <span className="-ml-1.5 h-3.5 w-3.5 rounded-full bg-[#F79E1B] mix-blend-multiply" />
      </span>
    );
  return <span className={`${base} text-[#2E77BC]`}>AMEX</span>;
}

/** Formulario de tarjeta reutilizable (checkout y pago de reservas aprobadas). */
export function FormularioTarjeta({ tarjeta, setTarjeta, errores, deshabilitado }) {
  const marca = detectarMarca(tarjeta.numero);
  const cambiar = (campo, formatear) => (e) => setTarjeta((t) => ({ ...t, [campo]: formatear ? formatear(e.target.value) : e.target.value }));

  return (
    <div className="space-y-4">
      <div className="relative overflow-hidden rounded-3xl bg-gradient-to-br from-[#1D1D1F] via-[#2C2C2E] to-[#3A3A3C] p-5 text-white shadow-lift">
        <div className="flex items-start justify-between">
          <span className="h-8 w-11 rounded-md bg-gradient-to-br from-[#E9D7A4] to-[#B8975A]" />
          <span className="text-[13px] font-bold uppercase tracking-wider text-white/80">{marca ? { visa: 'Visa', mastercard: 'Mastercard', amex: 'Amex' }[marca] : 'Tarjeta'}</span>
        </div>
        <p className="mt-6 font-mono text-[19px] tracking-[0.12em]">{tarjeta.numero || '•••• •••• •••• ••••'}</p>
        <div className="mt-4 flex justify-between text-[12px] uppercase tracking-wider text-white/70">
          <span className="truncate pr-4">{tarjeta.titular || 'Nombre del titular'}</span>
          <span>{tarjeta.vencimiento || 'MM/AA'}</span>
        </div>
      </div>

      <div>
        <div className="mb-1.5 flex items-center justify-between">
          <span className="label mb-0">Número de la tarjeta</span>
          <span className="flex gap-1">
            {['visa', 'mastercard', 'amex'].map((m) => (
              <LogoMarca key={m} marca={m} activa={!marca || marca === m} />
            ))}
          </span>
        </div>
        <Input
          inputMode="numeric"
          autoComplete="cc-number"
          placeholder="1234 5678 9012 3456"
          value={tarjeta.numero}
          onChange={cambiar('numero', formatearNumero)}
          error={errores.numero}
          disabled={deshabilitado}
          aria-label="Número de la tarjeta"
        />
      </div>
      <Input label="Nombre del titular" autoComplete="cc-name" placeholder="Como aparece en la tarjeta" value={tarjeta.titular} onChange={cambiar('titular', (v) => v.toUpperCase().slice(0, 40))} error={errores.titular} disabled={deshabilitado} />
      <div className="grid grid-cols-2 gap-3">
        <Input label="Vencimiento" inputMode="numeric" autoComplete="cc-exp" placeholder="MM/AA" value={tarjeta.vencimiento} onChange={cambiar('vencimiento', formatearVencimiento)} error={errores.vencimiento} disabled={deshabilitado} />
        <Input label="CVC" inputMode="numeric" autoComplete="cc-csc" placeholder={marca === 'amex' ? '4 dígitos' : '3 dígitos'} value={tarjeta.cvc} onChange={cambiar('cvc', (v) => soloDigitos(v).slice(0, marca === 'amex' ? 4 : 3))} error={errores.cvc} disabled={deshabilitado} />
      </div>

      <details className="rounded-2xl bg-canvas px-4 py-3 text-[13px] text-ink-soft">
        <summary className="flex cursor-pointer list-none items-center gap-2 font-medium text-ink">
          <Info className="h-4 w-4" /> Tarjetas de prueba (modo pruebas, no se cobra dinero real)
        </summary>
        <div className="mt-3 space-y-1.5">
          {TARJETAS_PRUEBA.map((t) => (
            <button
              key={t.numero}
              type="button"
              disabled={deshabilitado}
              onClick={() => setTarjeta((x) => ({ ...x, numero: t.numero, titular: x.titular || 'HUESPED DE PRUEBA', vencimiento: x.vencimiento || '12/30', cvc: x.cvc || '123' }))}
              className="flex w-full items-center justify-between rounded-xl bg-white px-3 py-2 text-left transition hover:shadow-soft"
            >
              <span className="font-mono text-[13px]">{t.numero}</span>
              <span className={`text-[12px] font-semibold ${t.resultado === 'Aprobada' ? 'text-emerald-700' : 'text-brand'}`}>{t.resultado}</span>
            </button>
          ))}
          <p className="pt-1 text-[12px] text-ink-faint">Cualquier fecha futura y cualquier CVC sirven.</p>
        </div>
      </details>
    </div>
  );
}

export const TARJETA_VACIA = { numero: '', titular: '', vencimiento: '', cvc: '' };

/** Animación mientras la pasarela procesa el cobro. */
export function ProcesandoPago({ texto = 'Procesando tu pago de forma segura…' }) {
  return (
    <div className="flex flex-col items-center py-10 text-center" role="status">
      <div className="relative h-16 w-16">
        <span className="absolute inset-0 animate-ping rounded-full bg-accent/20" />
        <span className="relative flex h-16 w-16 items-center justify-center rounded-full bg-ink text-white">
          <Lock className="h-6 w-6" />
        </span>
      </div>
      <p className="mt-6 text-[17px] font-semibold">{texto}</p>
      <p className="mt-1 text-sm text-ink-muted">No cierres esta ventana ni recargues la página.</p>
    </div>
  );
}

/**
 * Checkout en dos pasos: 1) revisar el viaje y el desglose; 2) pagar con tarjeta (reserva inmediata)
 * o enviar la solicitud sin pago (reserva con aprobación).
 */
export function CheckoutModal({ abierto, alCerrar, alojamiento, foto, llegada, salida, huespedes, cotizacion, extras, procesando, alConfirmar }) {
  const [paso, setPaso] = useState(1);
  const [tarjeta, setTarjeta] = useState(TARJETA_VACIA);
  const [errores, setErrores] = useState({});
  const inmediata = alojamiento.modoReserva === 'INMEDIATA';

  useEffect(() => {
    if (abierto) {
      setPaso(1);
      setErrores({});
    }
  }, [abierto]);

  const nombresExtras = useMemo(
    () => (alojamiento.serviciosAdicionales || []).filter((s) => extras.includes(s.id)).map((s) => s.nombre),
    [alojamiento, extras],
  );

  const pagar = () => {
    const v = validarTarjeta(tarjeta);
    setErrores(v);
    if (Object.keys(v).length) return;
    alConfirmar(tokenizar(tarjeta.numero));
  };

  const pie = procesando ? null : paso === 1 ? (
    <>
      <Button variante="ghost" onClick={alCerrar}>
        Cancelar
      </Button>
      {inmediata ? (
        <Button onClick={() => setPaso(2)} icono={CreditCard}>
          Continuar al pago
        </Button>
      ) : (
        <Button variante="brand" onClick={() => alConfirmar(null)}>
          Enviar solicitud al anfitrión
        </Button>
      )}
    </>
  ) : (
    <>
      <Button variante="ghost" onClick={() => setPaso(1)}>
        <ChevronLeft className="h-4 w-4" /> Atrás
      </Button>
      <Button variante="brand" icono={Lock} onClick={pagar}>
        Pagar {dinero(cotizacion?.total, cotizacion?.moneda)}
      </Button>
    </>
  );

  return (
    <Modal
      abierto={abierto}
      alCerrar={procesando ? undefined : alCerrar}
      titulo={procesando ? undefined : paso === 1 ? (inmediata ? 'Confirma y paga' : 'Solicita tu reserva') : 'Pago con tarjeta'}
      descripcion={procesando ? undefined : paso === 1 ? `Paso 1 de ${inmediata ? 2 : 1} · Revisa tu viaje` : 'Paso 2 de 2 · Pasarela segura'}
      ancho="max-w-xl"
      cerrarConFondo={!procesando}
      pie={pie}
    >
      {procesando ? (
        <ProcesandoPago texto={inmediata ? 'Procesando tu pago de forma segura…' : 'Enviando tu solicitud…'} />
      ) : paso === 1 ? (
        <div className="space-y-5">
          <div className="flex gap-4 rounded-3xl border border-line/60 p-3">
            <SafeImage src={foto} semilla={alojamiento.id} className="h-24 w-28 shrink-0 rounded-2xl" />
            <div className="min-w-0 py-1">
              <p className="line-clamp-2 text-[15px] font-semibold">{alojamiento.titulo}</p>
              <p className="text-[13px] text-ink-muted">{alojamiento.ciudad}</p>
              <p className="mt-1 text-[12.5px] text-ink-muted">{MODO_LABEL[alojamiento.modoReserva]}</p>
            </div>
          </div>
          <div className="grid grid-cols-2 gap-3 text-sm">
            <div className="rounded-2xl bg-canvas p-3">
              <p className="flex items-center gap-1.5 text-xs text-ink-muted">
                <CalendarDays className="h-3.5 w-3.5" /> Fechas
              </p>
              <p className="mt-1 font-semibold">{rangoFechas(llegada, salida)}</p>
            </div>
            <div className="rounded-2xl bg-canvas p-3">
              <p className="flex items-center gap-1.5 text-xs text-ink-muted">
                <Users className="h-3.5 w-3.5" /> Huéspedes
              </p>
              <p className="mt-1 font-semibold">{plural(huespedes, 'huésped', 'huéspedes')}</p>
            </div>
          </div>
          {cotizacion && (
            <div className="space-y-2 text-[14.5px]">
              <Fila t={`${dinero(cotizacion.precioNoche)} x ${plural(cotizacion.noches, 'noche')}`} v={cotizacion.precioBase} />
              {Number(cotizacion.tarifaLimpieza) > 0 && <Fila t="Limpieza" v={cotizacion.tarifaLimpieza} />}
              {(cotizacion.detalleServicios || []).map((s) => (
                <Fila key={s.id} t={s.nombre} v={s.precio} />
              ))}
              <Fila t="Tarifa de servicio" v={cotizacion.tarifaServicio} />
              {Number(cotizacion.descuento) > 0 && <Fila t="Descuento" v={-Number(cotizacion.descuento)} />}
              <div className="flex justify-between border-t border-line pt-3 text-[16px] font-semibold">
                <span>Total ({cotizacion.moneda || 'COP'})</span>
                <span>{dinero(cotizacion.total, cotizacion.moneda)}</span>
              </div>
            </div>
          )}
          {nombresExtras.length > 0 && <p className="text-xs text-ink-muted">Incluye: {nombresExtras.join(', ')}.</p>}
          {alojamiento.politica && (
            <p className="rounded-2xl bg-canvas px-4 py-3 text-[13px] text-ink-soft">
              <strong>Cancelación {String(alojamiento.politica.nombre).toLowerCase()}:</strong> {alojamiento.politica.descripcion}
            </p>
          )}
          {!inmediata && (
            <p className="flex gap-2 rounded-2xl border border-amber-200 bg-amber-50 px-4 py-3 text-[13px] text-amber-900">
              <Info className="mt-0.5 h-4 w-4 shrink-0" /> Aún no se cobra nada. Cuando el anfitrión apruebe tu solicitud te avisaremos para que pagues desde «Mis viajes».
            </p>
          )}
        </div>
      ) : (
        <div>
          <FormularioTarjeta tarjeta={tarjeta} setTarjeta={setTarjeta} errores={errores} />
          <p className="mt-4 flex items-center justify-center gap-1.5 text-center text-[12px] text-ink-faint">
            <ShieldCheck className="h-3.5 w-3.5" /> Pago procesado por Stripe (modo pruebas). Tus datos de tarjeta no se guardan.
          </p>
        </div>
      )}
    </Modal>
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
