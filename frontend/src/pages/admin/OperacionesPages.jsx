import { useState } from 'react';
import { Link } from 'react-router-dom';
import { ShieldAlert, Play, Percent, Save, Activity, Database, CreditCard, Gauge, RefreshCw } from 'lucide-react';
import { adminApi, catalogoApi, reservasApi, saludApi } from '../../api';
import { useAsync } from '../../hooks/useAsync';
import { useToast } from '../../context/ToastContext';
import { Badge, Button, Card, Confirmar, EmptyState, ErrorState, EstadoBadge, Input, Paginacion, SectionTitle, Skeleton, Stat } from '../../components/ui';
import { ESTADO_RESERVA, TARIFA_LABEL } from '../../utils/constants';
import { dinero, fechaHora, numero, rangoFechas } from '../../utils/format';

/** Reservas de publicaciones bloqueadas en revisión manual (HU-42): MANTENER o CANCELAR. */
export function ReservasRevisionPage() {
  const toast = useToast();
  const [pagina, setPagina] = useState(0);
  const [accion, setAccion] = useState(null);
  const [motivo, setMotivo] = useState('');
  const [enviando, setEnviando] = useState(false);
  const { data, error, cargando, recargar } = useAsync(() => adminApi.reservasRevision({ pagina, tamano: 20 }), [pagina]);
  const lista = data?.contenido || [];

  const ejecutar = async () => {
    setEnviando(true);
    try {
      await adminApi.resolverRevision(accion.reserva.id, accion.tipo, motivo);
      toast.exito(accion.tipo === 'MANTENER' ? `La reserva #${accion.reserva.id} se mantiene.` : `La reserva #${accion.reserva.id} fue cancelada con devolución.`);
      setAccion(null);
      setMotivo('');
      recargar();
    } catch (e) {
      toast.error(e);
    } finally {
      setEnviando(false);
    }
  };

  return (
    <div>
      <SectionTitle titulo="Reservas en revisión" subtitulo="Reservas futuras de publicaciones bloqueadas. Decide si se mantienen o se cancelan." />
      {error ? (
        <ErrorState error={error} alReintentar={recargar} />
      ) : cargando ? (
        <Skeleton className="h-64 w-full rounded-3xl" />
      ) : lista.length === 0 ? (
        <EmptyState icono={ShieldAlert} titulo="Nada que revisar" descripcion="Cuando bloquees una publicación con reservas futuras aparecerán aquí." />
      ) : (
        <Card padding="p-0" className="overflow-x-auto">
          <table className="table-app">
            <thead>
              <tr>
                <th>Reserva</th>
                <th>Alojamiento</th>
                <th>Huésped</th>
                <th>Fechas</th>
                <th>Total</th>
                <th>Estado</th>
                <th className="text-right">Decisión</th>
              </tr>
            </thead>
            <tbody>
              {lista.map((r) => (
                <tr key={r.id}>
                  <td>
                    <Link to={`/reservas/${r.id}`} className="font-medium text-accent hover:underline">
                      #{r.id}
                    </Link>
                  </td>
                  <td className="max-w-[200px] truncate">{r.tituloAlojamiento}</td>
                  <td>{r.huespedNombre}</td>
                  <td className="whitespace-nowrap">{rangoFechas(r.fechaLlegada, r.fechaSalida)}</td>
                  <td className="whitespace-nowrap">{dinero(r.total)}</td>
                  <td>
                    <EstadoBadge estado={r.estado} mapa={ESTADO_RESERVA} />
                  </td>
                  <td className="text-right">
                    <div className="flex justify-end gap-1.5">
                      <Button tamano="sm" variante="outline" onClick={() => setAccion({ tipo: 'MANTENER', reserva: r })}>
                        Mantener
                      </Button>
                      <Button tamano="sm" variante="danger" onClick={() => setAccion({ tipo: 'CANCELAR', reserva: r })}>
                        Cancelar
                      </Button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </Card>
      )}
      <Paginacion pagina={data?.pagina || 0} totalPaginas={data?.totalPaginas || 0} alCambiar={setPagina} />
      <Confirmar
        abierto={Boolean(accion)}
        alCerrar={() => setAccion(null)}
        alConfirmar={ejecutar}
        cargando={enviando}
        titulo={accion?.tipo === 'MANTENER' ? 'Mantener reserva' : 'Cancelar reserva'}
        mensaje={accion?.tipo === 'MANTENER' ? 'La reserva sigue vigente y sale de revisión.' : 'Se cancela la reserva y se gestiona la devolución al huésped.'}
        textoConfirmar={accion?.tipo === 'MANTENER' ? 'Mantener' : 'Cancelar reserva'}
        variante={accion?.tipo === 'MANTENER' ? 'primary' : 'brand'}
      >
        <Input className="mt-4" label="Motivo" opcional maxLength={300} value={motivo} onChange={(e) => setMotivo(e.target.value)} />
      </Confirmar>
      <ReservaPorId />
    </div>
  );
}

function ReservaPorId() {
  const [id, setId] = useState('');
  const [r, setR] = useState(null);
  const [error, setError] = useState(null);
  const [cargando, setCargando] = useState(false);
  const buscar = async (e) => {
    e.preventDefault();
    if (!id) return;
    setCargando(true);
    setError(null);
    try {
      setR(await reservasApi.detalle(id));
    } catch (er) {
      setR(null);
      setError(er.message);
    } finally {
      setCargando(false);
    }
  };
  return (
    <Card className="mt-10">
      <h2 className="text-[16px] font-semibold">Consultar cualquier reserva</h2>
      <form onSubmit={buscar} className="mt-3 flex gap-2">
        <Input type="number" min={1} placeholder="ID de la reserva" value={id} onChange={(e) => setId(e.target.value)} className="w-48" aria-label="ID de la reserva" />
        <Button type="submit" variante="secondary" cargando={cargando}>
          Buscar
        </Button>
      </form>
      {error && <p className="mt-3 text-sm text-brand">{error}</p>}
      {r && (
        <Link to={`/reservas/${r.id}`} className="mt-4 flex flex-wrap items-center gap-3 rounded-2xl bg-canvas p-4 text-sm hover:bg-[#EBEBEF]">
          <EstadoBadge estado={r.estado} mapa={ESTADO_RESERVA} />
          <span className="font-semibold">#{r.id}</span> {r.tituloAlojamiento} · {r.huespedNombre} · {rangoFechas(r.fechaLlegada, r.fechaSalida)} · {dinero(r.total)}
        </Link>
      )}
    </Card>
  );
}

/** Tarifas configurables (HU-24) y ejecución manual de tareas programadas (POST /admin/tareas). */
export function TarifasPage() {
  const toast = useToast();
  const { data, error, cargando, recargar } = useAsync(() => catalogoApi.tarifas(), []);
  const [valores, setValores] = useState({});
  const [guardando, setGuardando] = useState(null);
  const [tareas, setTareas] = useState(false);
  const [ejecutando, setEjecutando] = useState(false);
  const [resultadoTareas, setResultadoTareas] = useState(null);

  const guardar = async (t) => {
    const valor = valores[t.clave];
    const n = Number(valor);
    if (valor === undefined || valor === '' || !Number.isFinite(n) || n < 0 || n > 100) {
      toast.aviso('El valor debe estar entre 0 y 100.');
      return;
    }
    setGuardando(t.clave);
    try {
      await adminApi.actualizarTarifa(t.clave, n);
      toast.exito('Tarifa actualizada. Aplica a las nuevas cotizaciones en menos de un minuto.');
      setValores((v) => ({ ...v, [t.clave]: undefined }));
      recargar();
    } catch (e) {
      toast.error(e);
    } finally {
      setGuardando(null);
    }
  };

  const ejecutarTareas = async () => {
    setEjecutando(true);
    try {
      setResultadoTareas(await adminApi.ejecutarTareas());
      toast.exito('Tareas programadas ejecutadas.');
      setTareas(false);
    } catch (e) {
      toast.error(e);
    } finally {
      setEjecutando(false);
    }
  };

  return (
    <div>
      <SectionTitle titulo="Tarifas y operaciones" subtitulo="Ajusta el modelo de negocio sin volver a desplegar." />
      {error ? (
        <ErrorState error={error} alReintentar={recargar} />
      ) : cargando ? (
        <Skeleton className="h-48 w-full rounded-3xl" />
      ) : (
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
          {(data || []).map((t) => {
            const info = TARIFA_LABEL[t.clave] || { label: t.clave, unidad: '' };
            const editado = valores[t.clave] !== undefined;
            return (
              <Card key={t.id || t.clave}>
                <div className="flex items-center justify-between">
                  <p className="text-[13px] font-medium text-ink-muted">{info.label}</p>
                  <Percent className="h-4 w-4 text-ink-faint" />
                </div>
                <p className="mt-1 text-[30px] font-semibold tracking-tight">
                  {numero(t.valor)} <span className="text-base font-normal text-ink-muted">{info.unidad}</span>
                </p>
                <p className="text-xs text-ink-faint">Vigente desde {fechaHora(t.vigenteDesde)}</p>
                <div className="mt-4 flex gap-2">
                  <Input type="number" min={0} max={100} step="0.5" placeholder="Nuevo valor" value={valores[t.clave] ?? ''} onChange={(e) => setValores((v) => ({ ...v, [t.clave]: e.target.value }))} className="flex-1" aria-label={`Nuevo valor para ${info.label}`} />
                  <Button icono={Save} disabled={!editado} cargando={guardando === t.clave} onClick={() => guardar(t)}>
                    Guardar
                  </Button>
                </div>
              </Card>
            );
          })}
        </div>
      )}

      <Card className="mt-8">
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div>
            <h2 className="text-lg font-semibold">Tareas programadas</h2>
            <p className="mt-1 max-w-xl text-sm text-ink-muted">Completa estadías finalizadas, vence pagos pendientes, limpia claves de idempotencia de más de 24 h y reintenta reembolsos pendientes.</p>
          </div>
          <Button icono={Play} onClick={() => setTareas(true)}>
            Ejecutar ahora
          </Button>
        </div>
        {resultadoTareas && (
          <div className="mt-6 grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
            <Stat etiqueta="Reservas completadas" valor={numero(resultadoTareas.reservasCompletadas)} />
            <Stat etiqueta="Pagos vencidos" valor={numero(resultadoTareas.pagosPendientesVencidos)} />
            <Stat etiqueta="Claves eliminadas" valor={numero(resultadoTareas.clavesIdempotenciaEliminadas)} />
            <Stat etiqueta="Reembolsos reintentados" valor={numero(resultadoTareas.reembolsosReintentados)} />
          </div>
        )}
      </Card>
      <Confirmar abierto={tareas} alCerrar={() => setTareas(false)} alConfirmar={ejecutarTareas} cargando={ejecutando} titulo="Ejecutar tareas programadas" mensaje="Se ejecutarán ahora las tareas que normalmente corren en segundo plano." textoConfirmar="Ejecutar" />
    </div>
  );
}

/** Monitoreo (HU-49): GET /salud (público) y GET /salud/metricas (administrador). */
export function SaludPage() {
  const salud = useAsync(() => saludApi.estado(), []);
  const metricas = useAsync(() => saludApi.metricas(), []);
  const s = salud.data;
  const m = metricas.data;
  const recargar = () => {
    salud.recargar();
    metricas.recargar();
  };
  const tono = !s ? 'neutral' : s.estado === 'ok' ? 'success' : s.estado === 'degradado' ? 'warning' : 'danger';

  return (
    <div>
      <SectionTitle
        titulo="Salud del sistema"
        subtitulo="Estado de la base de datos, la pasarela de pagos y métricas técnicas."
        accion={
          <Button variante="outline" tamano="sm" icono={RefreshCw} onClick={recargar} cargando={salud.cargando || metricas.cargando}>
            Actualizar
          </Button>
        }
      />
      {salud.error ? (
        <ErrorState error={salud.error} alReintentar={salud.recargar} />
      ) : (
        <div className="grid gap-4 md:grid-cols-3">
          <Card>
            <div className="flex items-center gap-2 text-[13px] font-medium text-ink-muted">
              <Activity className="h-4 w-4" /> Estado general
            </div>
            <div className="mt-3">
              <Badge tono={tono}>{s ? String(s.estado).toUpperCase() : 'Consultando…'}</Badge>
            </div>
            <p className="mt-3 text-xs text-ink-faint">{s?.aplicacion}</p>
            <p className="text-xs text-ink-faint">{s?.hora && fechaHora(s.hora)}</p>
          </Card>
          <Card>
            <div className="flex items-center gap-2 text-[13px] font-medium text-ink-muted">
              <Database className="h-4 w-4" /> Base de datos
            </div>
            <div className="mt-3">
              <Badge tono={s?.baseDatos === 'UP' ? 'success' : 'danger'}>{s?.baseDatos || '—'}</Badge>
            </div>
          </Card>
          <Card>
            <div className="flex items-center gap-2 text-[13px] font-medium text-ink-muted">
              <CreditCard className="h-4 w-4" /> Pasarela de pagos
            </div>
            <p className="mt-3 text-[15px] font-semibold">{s?.pasarelaPagos || '—'}</p>
            <p className="mt-1 text-xs text-ink-muted">
              Cortacircuitos: <strong>{s?.cortacircuitos || '—'}</strong>
            </p>
          </Card>
        </div>
      )}

      <h2 className="mb-4 mt-10 flex items-center gap-2 text-lg font-semibold">
        <Gauge className="h-5 w-5" /> Métricas
      </h2>
      {metricas.error ? (
        <ErrorState error={metricas.error} alReintentar={metricas.recargar} />
      ) : !m ? (
        <Skeleton className="h-40 w-full rounded-3xl" />
      ) : (
        <>
          <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
            <Stat etiqueta="Peticiones" valor={numero(m.peticiones)} detalle={m.iniciadoEn ? `Desde ${fechaHora(m.iniciadoEn)}` : undefined} />
            <Stat etiqueta="Tiempo promedio" valor={`${numero(m.tiempoPromedioMs)} ms`} />
            <Stat etiqueta="Errores internos" valor={numero(m.erroresInternos)} />
            <Stat etiqueta="Reservas confirmadas" valor={numero(m.reservasConfirmadas)} />
            <Stat etiqueta="Pagos rechazados" valor={numero(m.pagosRechazados)} />
            <Stat etiqueta="Conflictos de reserva (409)" valor={numero(m.conflictosDeReserva)} />
            <Stat etiqueta="Respuestas idempotentes" valor={numero(m.respuestasIdempotentes)} />
            <Stat etiqueta="Cortacircuitos pagos" valor={m.cortacircuitosPagos || '—'} />
          </div>
          <div className="mt-6 grid gap-4 lg:grid-cols-2">
            <Card>
              <h3 className="text-[15px] font-semibold">Respuestas por código HTTP</h3>
              <div className="mt-4 flex flex-wrap gap-2">
                {Object.entries(m.porCodigoHttp || {}).map(([codigo, total]) => (
                  <span key={codigo} className="rounded-xl bg-canvas px-3 py-2 text-sm">
                    <strong className={Number(codigo) >= 500 ? 'text-brand' : Number(codigo) >= 400 ? 'text-amber-700' : 'text-emerald-700'}>{codigo}</strong>
                    <span className="ml-2 text-ink-soft">{numero(total)}</span>
                  </span>
                ))}
              </div>
            </Card>
            <Card>
              <h3 className="text-[15px] font-semibold">Outbox de notificaciones</h3>
              <div className="mt-4 grid grid-cols-3 gap-3 text-center">
                {['pendientes', 'procesados', 'error'].map((k) => (
                  <div key={k} className="rounded-2xl bg-canvas p-3">
                    <p className="text-xs capitalize text-ink-muted">{k}</p>
                    <p className="text-xl font-semibold">{numero(m.outbox?.[k] ?? 0)}</p>
                  </div>
                ))}
              </div>
            </Card>
          </div>
        </>
      )}
    </div>
  );
}
