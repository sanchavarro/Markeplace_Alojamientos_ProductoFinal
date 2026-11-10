import { useState } from 'react';
import { BellOff, CheckCheck } from 'lucide-react';
import { notificacionesApi } from '../../api';
import { useAsync } from '../../hooks/useAsync';
import { useNotificaciones } from '../../context/NotificationsContext';
import { useToast } from '../../context/ToastContext';
import { Badge, Button, EmptyState, ErrorState, Paginacion, SectionTitle, Skeleton } from '../../components/ui';
import { fechaHora, haceCuanto } from '../../utils/format';

const TIPO_LABEL = {
  USUARIO_REGISTRADO: 'Bienvenida',
  RECUPERACION_CLAVE: 'Seguridad',
  RESERVA_CONFIRMADA: 'Reserva',
  RESERVA_POR_APROBAR: 'Solicitud',
  RESERVA_APROBADA: 'Reserva',
  PAGO_FALLIDO: 'Pago',
  RESERVA_CANCELADA: 'Cancelación',
  PUBLICACION_ESTADO: 'Publicación',
  USUARIO_ESTADO: 'Cuenta',
  REPORTE_LISTO: 'Reporte',
};

/** Centro de notificaciones (HU-37): de la más reciente a la más antigua, paginado. */
export default function NotificacionesPage() {
  const [pagina, setPagina] = useState(0);
  const { invalidar, version, noLeidas } = useNotificaciones();
  const toast = useToast();
  const { data, error, cargando, recargar, setData } = useAsync(() => notificacionesApi.listar({ pagina, tamano: 20 }), [pagina, version]);

  const marcar = async (n) => {
    if (n.leida) return;
    try {
      await notificacionesApi.marcarLeida(n.id);
      setData((d) => ({ ...d, contenido: d.contenido.map((x) => (x.id === n.id ? { ...x, leida: true } : x)) }));
      invalidar();
    } catch (e) {
      toast.error(e);
    }
  };

  const marcarTodas = async () => {
    try {
      const r = await notificacionesApi.marcarTodas();
      toast.exito(r?.marcadas ? `${r.marcadas} notificaciones marcadas como leídas.` : 'Todas las notificaciones están leídas.');
      invalidar();
    } catch (e) {
      toast.error(e);
    }
  };

  const lista = data?.contenido || [];

  return (
    <div className="container-app max-w-3xl py-10">
      <SectionTitle
        titulo="Notificaciones"
        subtitulo={noLeidas ? `${noLeidas} sin leer` : 'Estás al día.'}
        accion={
          noLeidas > 0 && (
            <Button variante="outline" tamano="sm" icono={CheckCheck} onClick={marcarTodas}>
              Marcar todas como leídas
            </Button>
          )
        }
      />
      {error ? (
        <ErrorState error={error} alReintentar={recargar} />
      ) : cargando && !data ? (
        <div className="space-y-3">
          {[0, 1, 2, 3].map((i) => (
            <Skeleton key={i} className="h-20 w-full rounded-2xl" />
          ))}
        </div>
      ) : lista.length === 0 ? (
        <EmptyState icono={BellOff} titulo="Sin notificaciones" descripcion="Aquí verás novedades de tus reservas, cancelaciones y decisiones del administrador." />
      ) : (
        <ul className="divide-y divide-line/60 overflow-hidden rounded-3xl border border-line/50 bg-white shadow-soft">
          {lista.map((n) => (
            <li key={n.id}>
              <button type="button" onClick={() => marcar(n)} className={`flex w-full gap-4 px-5 py-4 text-left transition hover:bg-canvas/70 ${n.leida ? '' : 'bg-accent-soft/40'}`}>
                <span className={`mt-2 h-2.5 w-2.5 shrink-0 rounded-full ${n.leida ? 'bg-line' : 'bg-accent'}`} />
                <span className="min-w-0 flex-1">
                  <span className="flex flex-wrap items-center gap-2">
                    <Badge tono={n.leida ? 'neutral' : 'info'}>{TIPO_LABEL[n.tipo] || n.tipo}</Badge>
                    <span className="text-xs text-ink-faint" title={fechaHora(n.creadaEn)}>
                      {haceCuanto(n.creadaEn)}
                    </span>
                  </span>
                  <span className={`mt-1.5 block text-[15px] leading-snug ${n.leida ? 'text-ink-soft' : 'font-medium text-ink'}`}>{n.mensaje}</span>
                </span>
              </button>
            </li>
          ))}
        </ul>
      )}
      <Paginacion pagina={data?.pagina || 0} totalPaginas={data?.totalPaginas || 0} alCambiar={setPagina} />
    </div>
  );
}
