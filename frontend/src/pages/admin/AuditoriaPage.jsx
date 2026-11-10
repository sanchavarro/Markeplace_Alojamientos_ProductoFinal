import { useState } from 'react';
import { ScrollText, Filter } from 'lucide-react';
import { adminApi } from '../../api';
import { useAsync } from '../../hooks/useAsync';
import { Badge, Button, Card, EmptyState, ErrorState, Input, Paginacion, SectionTitle, Select, Skeleton } from '../../components/ui';
import { fechaHora } from '../../utils/format';

const ENTIDADES = ['USUARIO', 'ALOJAMIENTO', 'RESERVA', 'RESENA', 'REPORTE', 'TARIFA', 'PERIODO', 'PAGO'];
const VACIO = { entidad: '', usuarioId: '', accion: '', desde: '', hasta: '' };

/** Registro de auditoría de solo lectura (HU-45) con filtros por usuario, acción y fechas, paginado. */
export default function AuditoriaPage() {
  const [borrador, setBorrador] = useState(VACIO);
  const [filtros, setFiltros] = useState(VACIO);
  const [pagina, setPagina] = useState(0);
  const [errorFiltro, setErrorFiltro] = useState(null);
  const { data, error, cargando, recargar } = useAsync(
    () => adminApi.auditoria({ ...filtros, accion: filtros.accion.trim().toUpperCase() || undefined, pagina, tamano: 25 }),
    [filtros, pagina],
  );
  const lista = data?.contenido || [];

  const aplicar = (e) => {
    e.preventDefault();
    if (borrador.desde && borrador.hasta && borrador.hasta < borrador.desde) {
      setErrorFiltro('La fecha final no puede ser anterior a la inicial.');
      return;
    }
    setErrorFiltro(null);
    setPagina(0);
    setFiltros(borrador);
  };

  return (
    <div>
      <SectionTitle titulo="Auditoría" subtitulo="Quién hizo cada acción crítica. Registro inmutable de solo lectura." />
      <Card>
        <form onSubmit={aplicar} className="grid gap-3 sm:grid-cols-2 lg:grid-cols-6 lg:items-end">
          <Select label="Entidad" value={borrador.entidad} onChange={(e) => setBorrador({ ...borrador, entidad: e.target.value })}>
            <option value="">Todas</option>
            {ENTIDADES.map((x) => (
              <option key={x} value={x}>
                {x}
              </option>
            ))}
          </Select>
          <Input label="ID usuario" type="number" min={1} value={borrador.usuarioId} onChange={(e) => setBorrador({ ...borrador, usuarioId: e.target.value })} />
          <Input label="Acción" placeholder="CANCELAR_RESERVA" value={borrador.accion} onChange={(e) => setBorrador({ ...borrador, accion: e.target.value })} />
          <Input label="Desde" type="date" value={borrador.desde} onChange={(e) => setBorrador({ ...borrador, desde: e.target.value })} />
          <Input label="Hasta" type="date" value={borrador.hasta} onChange={(e) => setBorrador({ ...borrador, hasta: e.target.value })} />
          <div className="flex gap-2">
            <Button type="submit" icono={Filter} completo>
              Filtrar
            </Button>
            <Button
              variante="ghost"
              onClick={() => {
                setBorrador(VACIO);
                setFiltros(VACIO);
                setPagina(0);
              }}
            >
              Limpiar
            </Button>
          </div>
        </form>
        {errorFiltro && <p className="mt-3 text-sm text-brand">{errorFiltro}</p>}
      </Card>

      <div className="mt-6">
        {error ? (
          <ErrorState error={error} alReintentar={recargar} />
        ) : cargando ? (
          <Skeleton className="h-96 w-full rounded-3xl" />
        ) : lista.length === 0 ? (
          <EmptyState icono={ScrollText} titulo="Sin eventos para estos filtros" />
        ) : (
          <Card padding="p-0" className="overflow-x-auto">
            <table className="table-app">
              <thead>
                <tr>
                  <th>Fecha</th>
                  <th>Usuario</th>
                  <th>Entidad</th>
                  <th>Acción</th>
                  <th>Resultado</th>
                  <th>Detalle</th>
                </tr>
              </thead>
              <tbody>
                {lista.map((ev) => (
                  <tr key={ev.id}>
                    <td className="whitespace-nowrap">{fechaHora(ev.fecha)}</td>
                    <td>{ev.usuarioId ? `#${ev.usuarioId}` : 'Anónimo'}</td>
                    <td className="whitespace-nowrap">
                      {ev.entidad}
                      {ev.entidadId && <span className="text-ink-faint"> #{ev.entidadId}</span>}
                    </td>
                    <td className="whitespace-nowrap font-mono text-[12.5px] text-ink">{ev.accion}</td>
                    <td>
                      <Badge tono={ev.resultado === 'EXITO' ? 'success' : 'danger'}>{ev.resultado}</Badge>
                    </td>
                    <td className="max-w-[340px]">
                      <span className="line-clamp-2 break-all font-mono text-[12px] text-ink-muted" title={ev.detalle || ''}>
                        {ev.detalle || '—'}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </Card>
        )}
      </div>
      <Paginacion pagina={data?.pagina || 0} totalPaginas={data?.totalPaginas || 0} alCambiar={setPagina} />
    </div>
  );
}
