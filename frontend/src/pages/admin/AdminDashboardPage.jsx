import { Users, House, CalendarCheck, Wallet, RefreshCw } from 'lucide-react';
import { reportesApi } from '../../api';
import { useAsync } from '../../hooks/useAsync';
import { Button, Card, ErrorState, SectionTitle, Skeleton, Stat } from '../../components/ui';
import { ESTADO_ALOJAMIENTO, ESTADO_RESERVA } from '../../utils/constants';
import { dinero, numero } from '../../utils/format';

/**
 * Barra horizontal de una sola serie (un solo tono, sin leyenda: el título nombra la serie).
 * Marcas delgadas, extremo redondeado anclado a la base, valores en tinta (no en el color de la serie)
 * y tooltip nativo por barra. Incluye tabla equivalente para lectores de pantalla.
 */
function BarrasHorizontales({ titulo, datos, formato = numero }) {
  const max = Math.max(1, ...datos.map((d) => Number(d.valor) || 0));
  return (
    <Card>
      <h3 className="text-[15px] font-semibold">{titulo}</h3>
      {datos.length === 0 ? (
        <p className="mt-6 text-sm text-ink-muted">Sin datos todavía.</p>
      ) : (
        <ul className="mt-5 space-y-3.5" aria-hidden="true">
          {datos.map((d) => {
            const ancho = `${Math.max(1.5, ((Number(d.valor) || 0) / max) * 100)}%`;
            return (
              <li key={d.etiqueta} className="group grid grid-cols-[120px_1fr_auto] items-center gap-3" title={`${d.etiqueta}: ${formato(d.valor)}`}>
                <span className="truncate text-[13px] text-ink-soft">{d.etiqueta}</span>
                <span className="h-2.5 rounded-full bg-canvas">
                  <span className="block h-full rounded-full bg-[#2E6BD6] transition-all duration-700 group-hover:bg-[#1F55B5]" style={{ width: ancho }} />
                </span>
                <span className="min-w-[2.5rem] text-right text-[13px] font-semibold tabular-nums text-ink">{formato(d.valor)}</span>
              </li>
            );
          })}
        </ul>
      )}
      <table className="sr-only">
        <caption>{titulo}</caption>
        <tbody>
          {datos.map((d) => (
            <tr key={d.etiqueta}>
              <th scope="row">{d.etiqueta}</th>
              <td>{formato(d.valor)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </Card>
  );
}

/** Dashboard del administrador (HU-46): usuarios, publicaciones, reservas por estado, ingresos y demanda. */
export default function AdminDashboardPage() {
  const { data: d, error, cargando, recargar } = useAsync(() => reportesApi.dashboard(), []);

  if (error) return <ErrorState error={error} alReintentar={recargar} />;

  const reservas = d?.reservas || {};
  const publicaciones = d?.publicaciones || {};
  const usuarios = d?.usuarios || {};
  const totalReservas = Object.values(reservas).reduce((s, v) => s + Number(v || 0), 0);

  return (
    <div>
      <SectionTitle
        titulo="Dashboard"
        subtitulo="El estado del negocio de un vistazo."
        accion={
          <Button variante="outline" tamano="sm" icono={RefreshCw} onClick={recargar} cargando={cargando}>
            Actualizar
          </Button>
        }
      />
      {cargando && !d ? (
        <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
          {[0, 1, 2, 3].map((i) => (
            <Skeleton key={i} className="h-28 rounded-3xl" />
          ))}
          <Skeleton className="h-64 rounded-3xl sm:col-span-2" />
          <Skeleton className="h-64 rounded-3xl sm:col-span-2" />
        </div>
      ) : (
        <>
          <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
            <Stat etiqueta="Usuarios" icono={Users} valor={numero(Number(usuarios.huespedes || 0) + Number(usuarios.anfitriones || 0))} detalle={`${numero(usuarios.huespedes)} huéspedes · ${numero(usuarios.anfitriones)} anfitriones · ${numero(usuarios.restringidos)} restringidos`} />
            <Stat etiqueta="Publicaciones activas" icono={House} valor={numero(publicaciones.ACTIVA || 0)} detalle={`${numero(publicaciones.PENDIENTE_REVISION || 0)} pendientes de revisión`} />
            <Stat etiqueta="Reservas" icono={CalendarCheck} valor={numero(totalReservas)} detalle={`${numero(reservas.CONFIRMADA || 0)} confirmadas`} />
            <Stat etiqueta="Ingresos aprobados" icono={Wallet} valor={dinero(d?.ingresosAprobadosCOP || 0)} detalle="Pagos aprobados (COP)" />
          </div>
          <div className="mt-6 grid gap-6 lg:grid-cols-2">
            <BarrasHorizontales titulo="Reservas por estado" datos={Object.entries(reservas).map(([k, v]) => ({ etiqueta: ESTADO_RESERVA[k]?.label || k, valor: v }))} />
            <BarrasHorizontales titulo="Ciudades con más demanda" datos={(d?.ciudadesConMasDemanda || []).map((c) => ({ etiqueta: c.ciudad, valor: c.reservas }))} />
            <BarrasHorizontales titulo="Publicaciones por estado" datos={Object.entries(publicaciones).map(([k, v]) => ({ etiqueta: ESTADO_ALOJAMIENTO[k]?.label || k, valor: v }))} />
            <BarrasHorizontales
              titulo="Usuarios por tipo"
              datos={[
                { etiqueta: 'Huéspedes', valor: usuarios.huespedes || 0 },
                { etiqueta: 'Anfitriones', valor: usuarios.anfitriones || 0 },
                { etiqueta: 'Restringidos', valor: usuarios.restringidos || 0 },
              ]}
            />
          </div>
        </>
      )}
    </div>
  );
}
