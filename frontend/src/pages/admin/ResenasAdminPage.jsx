import { useState } from 'react';
import { Link } from 'react-router-dom';
import { Eye, EyeOff, MessageSquareOff } from 'lucide-react';
import { adminApi } from '../../api';
import { useAsync } from '../../hooks/useAsync';
import { useToast } from '../../context/ToastContext';
import { Button, Card, Confirmar, EmptyState, ErrorState, Input, Paginacion, SectionTitle, Skeleton } from '../../components/ui';
import { fechaHora } from '../../utils/format';

/**
 * Moderación de reseñas (HU-41). El backend no expone un listado de reseñas ocultas, así que se
 * reconstruye desde la auditoría (acción OCULTAR_RESENA, entidad RESENA) y se permite volver a mostrarlas.
 * Para ocultar, el administrador lo hace desde el detalle del alojamiento o aquí por número de reseña.
 */
export default function ResenasAdminPage() {
  const toast = useToast();
  const [pagina, setPagina] = useState(0);
  const [accion, setAccion] = useState(null);
  const [motivo, setMotivo] = useState('');
  const [enviando, setEnviando] = useState(false);
  const [idManual, setIdManual] = useState('');

  const ocultas = useAsync(() => adminApi.auditoria({ entidad: 'RESENA', accion: 'OCULTAR_RESENA', pagina, tamano: 20 }), [pagina]);
  const mostradas = useAsync(() => adminApi.auditoria({ entidad: 'RESENA', accion: 'MOSTRAR_RESENA', tamano: 100 }), [pagina]);

  // Una reseña sigue oculta si su último evento es OCULTAR (no hay un MOSTRAR posterior).
  const ultimaMostrada = new Map();
  (mostradas.data?.contenido || []).forEach((e) => {
    const previo = ultimaMostrada.get(e.entidadId);
    if (!previo || previo < e.fecha) ultimaMostrada.set(e.entidadId, e.fecha);
  });
  const vistas = new Set();
  const lista = (ocultas.data?.contenido || []).filter((e) => {
    if (vistas.has(e.entidadId)) return false;
    vistas.add(e.entidadId);
    const m = ultimaMostrada.get(e.entidadId);
    return !m || m < e.fecha;
  });

  const ejecutar = async () => {
    setEnviando(true);
    try {
      if (accion.tipo === 'mostrar') await adminApi.mostrarResena(accion.id, motivo);
      else await adminApi.ocultarResena(accion.id, motivo);
      toast.exito(`Reseña #${accion.id} ${accion.tipo === 'mostrar' ? 'visible de nuevo' : 'ocultada'}.`);
      setAccion(null);
      setMotivo('');
      setIdManual('');
      ocultas.recargar();
      mostradas.recargar();
    } catch (e) {
      toast.error(e);
    } finally {
      setEnviando(false);
    }
  };

  return (
    <div>
      <SectionTitle titulo="Reseñas" subtitulo="Oculta contenido inapropiado o vuelve a mostrarlo. Cada acción queda en auditoría." />

      <Card>
        <h2 className="text-[16px] font-semibold">Acción directa por número de reseña</h2>
        <p className="mt-1 text-sm text-ink-muted">También puedes ocultar reseñas desde el detalle de cada alojamiento.</p>
        <div className="mt-4 flex flex-wrap gap-2">
          <Input type="number" min={1} placeholder="ID de la reseña" value={idManual} onChange={(e) => setIdManual(e.target.value)} className="w-48" aria-label="ID de la reseña" />
          <Button variante="danger" icono={EyeOff} disabled={!idManual} onClick={() => setAccion({ tipo: 'ocultar', id: Number(idManual) })}>
            Ocultar
          </Button>
          <Button variante="outline" icono={Eye} disabled={!idManual} onClick={() => setAccion({ tipo: 'mostrar', id: Number(idManual) })}>
            Mostrar
          </Button>
        </div>
      </Card>

      <h2 className="mb-4 mt-10 text-lg font-semibold">Reseñas ocultas</h2>
      {ocultas.error ? (
        <ErrorState error={ocultas.error} alReintentar={ocultas.recargar} />
      ) : ocultas.cargando ? (
        <Skeleton className="h-48 w-full rounded-3xl" />
      ) : lista.length === 0 ? (
        <EmptyState icono={MessageSquareOff} titulo="No hay reseñas ocultas" />
      ) : (
        <Card padding="p-0" className="overflow-x-auto">
          <table className="table-app">
            <thead>
              <tr>
                <th>Reseña</th>
                <th>Ocultada</th>
                <th>Por</th>
                <th>Detalle</th>
                <th className="text-right">Acción</th>
              </tr>
            </thead>
            <tbody>
              {lista.map((e) => (
                <tr key={e.id}>
                  <td className="font-medium text-ink">#{e.entidadId}</td>
                  <td className="whitespace-nowrap">{fechaHora(e.fecha)}</td>
                  <td>#{e.usuarioId}</td>
                  <td className="max-w-[320px] truncate font-mono text-xs" title={e.detalle || ''}>
                    {e.detalle || '—'}
                  </td>
                  <td className="text-right">
                    <Button tamano="sm" variante="outline" icono={Eye} onClick={() => setAccion({ tipo: 'mostrar', id: e.entidadId })}>
                      Mostrar
                    </Button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </Card>
      )}
      <Paginacion pagina={ocultas.data?.pagina || 0} totalPaginas={ocultas.data?.totalPaginas || 0} alCambiar={setPagina} />
      <p className="mt-4 text-xs text-ink-faint">
        Ver el historial completo en <Link to="/admin/auditoria" className="text-accent hover:underline">Auditoría</Link>.
      </p>

      <Confirmar
        abierto={Boolean(accion)}
        alCerrar={() => setAccion(null)}
        alConfirmar={ejecutar}
        cargando={enviando}
        titulo={accion?.tipo === 'mostrar' ? `Mostrar reseña #${accion?.id}` : `Ocultar reseña #${accion?.id}`}
        mensaje={accion?.tipo === 'mostrar' ? 'Volverá a verse y a contar en el promedio.' : 'Dejará de verse y no contará en el promedio.'}
        textoConfirmar={accion?.tipo === 'mostrar' ? 'Mostrar' : 'Ocultar'}
        variante={accion?.tipo === 'mostrar' ? 'primary' : 'brand'}
      >
        <Input className="mt-4" label="Motivo" opcional maxLength={300} value={motivo} onChange={(e) => setMotivo(e.target.value)} />
      </Confirmar>
    </div>
  );
}
