import { useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { Check, X, CalendarX2 } from 'lucide-react';
import { alojamientosApi, reservasApi } from '../../api';
import { useAsync } from '../../hooks/useAsync';
import { useToast } from '../../context/ToastContext';
import { useNotificaciones } from '../../context/NotificationsContext';
import { Button, Card, Confirmar, EmptyState, ErrorState, EstadoBadge, Paginacion, SectionTitle, Select, Skeleton } from '../../components/ui';
import { ESTADO_RESERVA } from '../../utils/constants';
import { dinero, rangoFechas, plural } from '../../utils/format';

/** Reservas de mis alojamientos (HU-30) y aprobación/rechazo de solicitudes (HU-14). */
export default function HostReservasPage() {
  const [sp, setSp] = useSearchParams();
  const alojamientoId = sp.get('alojamientoId') || '';
  const [pagina, setPagina] = useState(0);
  const toast = useToast();
  const { invalidar } = useNotificaciones();
  const [decision, setDecision] = useState(null);
  const [enviando, setEnviando] = useState(false);

  const { data: mios } = useAsync(() => alojamientosApi.mios({ tamano: 100 }), []);
  const { data, error, cargando, recargar } = useAsync(() => reservasApi.anfitrion({ alojamientoId: alojamientoId || undefined, pagina, tamano: 20 }), [alojamientoId, pagina]);
  const lista = data?.contenido || [];

  const decidir = async () => {
    setEnviando(true);
    try {
      if (decision.tipo === 'aprobar') await reservasApi.aprobar(decision.reserva.id);
      else await reservasApi.rechazar(decision.reserva.id);
      toast.exito(decision.tipo === 'aprobar' ? 'Solicitud aprobada. El huésped recibirá el aviso para pagar.' : 'Solicitud rechazada. Las fechas quedaron libres.');
      setDecision(null);
      invalidar();
      recargar();
    } catch (e) {
      toast.error(e);
    } finally {
      setEnviando(false);
    }
  };

  const pendientes = lista.filter((r) => r.estado === 'EN_REVISION').length;

  return (
    <div>
      <SectionTitle
        titulo="Reservas"
        subtitulo={pendientes ? `${plural(pendientes, 'solicitud', 'solicitudes')} esperando tu respuesta.` : 'Prepara la llegada de tus huéspedes.'}
        accion={
          <Select
            aria-label="Filtrar por alojamiento"
            className="w-64"
            value={alojamientoId}
            onChange={(e) => {
              setPagina(0);
              setSp(e.target.value ? { alojamientoId: e.target.value } : {});
            }}
          >
            <option value="">Todos mis alojamientos</option>
            {(mios?.contenido || []).map((a) => (
              <option key={a.id} value={a.id}>
                {a.titulo}
              </option>
            ))}
          </Select>
        }
      />

      {error ? (
        <ErrorState error={error} alReintentar={recargar} />
      ) : cargando ? (
        <Skeleton className="h-72 w-full rounded-3xl" />
      ) : lista.length === 0 ? (
        <EmptyState icono={CalendarX2} titulo="Sin reservas todavía" descripcion="Cuando un huésped reserve uno de tus alojamientos aparecerá aquí." />
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
                <th className="text-right">Acciones</th>
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
                  <td className="max-w-[220px] truncate">{r.tituloAlojamiento}</td>
                  <td>{r.huespedNombre}</td>
                  <td className="whitespace-nowrap">
                    {rangoFechas(r.fechaLlegada, r.fechaSalida)}
                    <span className="block text-xs text-ink-faint">{plural(r.numHuespedes, 'huésped', 'huéspedes')}</span>
                  </td>
                  <td className="whitespace-nowrap font-medium text-ink">{dinero(r.total)}</td>
                  <td>
                    <EstadoBadge estado={r.estado} mapa={ESTADO_RESERVA} />
                  </td>
                  <td className="text-right">
                    {r.estado === 'EN_REVISION' ? (
                      <div className="flex justify-end gap-1.5">
                        <Button tamano="sm" icono={Check} onClick={() => setDecision({ tipo: 'aprobar', reserva: r })}>
                          Aprobar
                        </Button>
                        <Button tamano="sm" variante="danger" icono={X} onClick={() => setDecision({ tipo: 'rechazar', reserva: r })}>
                          Rechazar
                        </Button>
                      </div>
                    ) : (
                      <Button tamano="sm" variante="ghost" to={`/reservas/${r.id}`}>
                        Ver
                      </Button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </Card>
      )}
      <Paginacion pagina={data?.pagina || 0} totalPaginas={data?.totalPaginas || 0} alCambiar={setPagina} />

      <Confirmar
        abierto={Boolean(decision)}
        alCerrar={() => setDecision(null)}
        alConfirmar={decidir}
        cargando={enviando}
        titulo={decision?.tipo === 'aprobar' ? 'Aprobar solicitud' : 'Rechazar solicitud'}
        mensaje={decision && `${decision.reserva.huespedNombre} · ${rangoFechas(decision.reserva.fechaLlegada, decision.reserva.fechaSalida)} · ${dinero(decision.reserva.total)}`}
        textoConfirmar={decision?.tipo === 'aprobar' ? 'Aprobar' : 'Rechazar'}
        variante={decision?.tipo === 'aprobar' ? 'primary' : 'brand'}
      />
    </div>
  );
}
