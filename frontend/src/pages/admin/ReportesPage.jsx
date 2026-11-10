import { useEffect, useState } from 'react';
import { Download, FileSpreadsheet, FileText, FileType2, Clock } from 'lucide-react';
import { descargarBlob, reportesApi } from '../../api';
import { useAsync } from '../../hooks/useAsync';
import { useToast } from '../../context/ToastContext';
import { useNotificaciones } from '../../context/NotificationsContext';
import { Button, Card, EmptyState, ErrorState, EstadoBadge, Input, Paginacion, SectionTitle, Skeleton } from '../../components/ui';
import { ESTADO_EXPORTACION } from '../../utils/constants';
import { fechaCorta, fechaHora, numero } from '../../utils/format';
import { EXPORTACIONES_INTERVALO_MS } from '../../utils/env';

const FORMATOS = [
  { valor: 'pdf', label: 'PDF', icono: FileText },
  { valor: 'excel', label: 'Excel', icono: FileSpreadsheet },
  { valor: 'csv', label: 'CSV', icono: FileType2 },
];

/**
 * Exportación de reportes (HU-47, HU-48):
 *  - En segundo plano: POST /reportes/exportaciones → se consulta el estado (PENDIENTE → PROCESANDO → LISTO)
 *    y al terminar se descarga el binario (Blob) de GET /reportes/exportaciones/{id}/archivo.
 *  - Descarga inmediata: GET /reportes/reservas.
 * El backend registra en auditoría quién exporta, el tipo y los filtros.
 */
export default function ReportesPage() {
  const toast = useToast();
  const { invalidar } = useNotificaciones();
  const [f, setF] = useState({ formato: 'pdf', desde: '', hasta: '' });
  const [error, setError] = useState(null);
  const [solicitando, setSolicitando] = useState(false);
  const [descargando, setDescargando] = useState(null);
  const [pagina, setPagina] = useState(0);
  const { data, error: errorLista, cargando, recargar, setData } = useAsync(() => reportesApi.exportaciones({ pagina, tamano: 10 }), [pagina]);
  const lista = data?.contenido || [];
  const enCurso = lista.some((x) => x.estado === 'PENDIENTE' || x.estado === 'PROCESANDO');

  // Sondeo del estado mientras haya exportaciones en curso.
  useEffect(() => {
    if (!enCurso) return undefined;
    const t = setInterval(async () => {
      try {
        const nuevos = await Promise.all(lista.map((x) => (x.estado === 'PENDIENTE' || x.estado === 'PROCESANDO' ? reportesApi.exportacion(x.id) : x)));
        const terminados = nuevos.filter((n, i) => n.estado !== lista[i].estado && n.estado === 'LISTO');
        if (terminados.length) {
          toast.exito(`El reporte #${terminados[0].id} está listo para descargar.`);
          invalidar();
        }
        setData((d) => ({ ...d, contenido: nuevos }));
      } catch {
        /* se reintenta en el siguiente ciclo */
      }
    }, EXPORTACIONES_INTERVALO_MS);
    return () => clearInterval(t);
  }, [enCurso, lista, setData, toast, invalidar]);

  const validar = () => {
    if (f.desde && f.hasta && f.hasta < f.desde) return 'La fecha final no puede ser anterior a la inicial.';
    return null;
  };

  const solicitar = async () => {
    const err = validar();
    setError(err);
    if (err) return;
    setSolicitando(true);
    try {
      const r = await reportesApi.solicitarExportacion(f);
      toast.info(`Reporte #${r.id} en cola. Te avisaremos cuando esté listo.`);
      setPagina(0);
      recargar();
    } catch (e) {
      setError(e.message);
    } finally {
      setSolicitando(false);
    }
  };

  const descargaInmediata = async () => {
    const err = validar();
    setError(err);
    if (err) return;
    setDescargando('inmediata');
    try {
      const { blob, nombre } = await reportesApi.descargarReservas(f);
      descargarBlob(blob, nombre);
    } catch (e) {
      toast.error(e);
    } finally {
      setDescargando(null);
    }
  };

  const descargar = async (x) => {
    setDescargando(x.id);
    try {
      const { blob, nombre } = await reportesApi.descargarExportacion(x.id, x.nombreArchivo);
      descargarBlob(blob, nombre);
    } catch (e) {
      toast.error(e);
    } finally {
      setDescargando(null);
    }
  };

  return (
    <div>
      <SectionTitle titulo="Reportes" subtitulo="Exporta las reservas en PDF, Excel o CSV con los filtros que elijas." />
      <Card>
        <p className="label">Formato</p>
        <div className="grid grid-cols-3 gap-3 sm:max-w-md">
          {FORMATOS.map((o) => (
            <button
              key={o.valor}
              type="button"
              onClick={() => setF({ ...f, formato: o.valor })}
              aria-pressed={f.formato === o.valor}
              className={`flex flex-col items-center gap-2 rounded-2xl border py-4 text-sm font-medium transition ${f.formato === o.valor ? 'border-ink bg-ink text-white' : 'border-line hover:border-ink/40'}`}
            >
              <o.icono className="h-5 w-5" strokeWidth={1.7} />
              {o.label}
            </button>
          ))}
        </div>
        <div className="mt-5 grid gap-3 sm:max-w-md sm:grid-cols-2">
          <Input label="Desde" opcional type="date" value={f.desde} onChange={(e) => setF({ ...f, desde: e.target.value })} />
          <Input label="Hasta" opcional type="date" value={f.hasta} onChange={(e) => setF({ ...f, hasta: e.target.value })} />
        </div>
        {error && <p className="mt-3 text-sm text-brand">{error}</p>}
        <div className="mt-6 flex flex-wrap gap-2">
          <Button icono={Clock} onClick={solicitar} cargando={solicitando}>
            Generar en segundo plano
          </Button>
          <Button variante="outline" icono={Download} onClick={descargaInmediata} cargando={descargando === 'inmediata'}>
            Descargar ahora
          </Button>
        </div>
      </Card>

      <h2 className="mb-4 mt-10 text-lg font-semibold">Mis exportaciones</h2>
      {errorLista ? (
        <ErrorState error={errorLista} alReintentar={recargar} />
      ) : cargando && !data ? (
        <Skeleton className="h-48 w-full rounded-3xl" />
      ) : lista.length === 0 ? (
        <EmptyState icono={FileText} titulo="Aún no has generado reportes" />
      ) : (
        <Card padding="p-0" className="overflow-x-auto">
          <table className="table-app">
            <thead>
              <tr>
                <th>#</th>
                <th>Formato</th>
                <th>Rango</th>
                <th>Estado</th>
                <th>Registros</th>
                <th>Solicitado</th>
                <th className="text-right">Archivo</th>
              </tr>
            </thead>
            <tbody>
              {lista.map((x) => (
                <tr key={x.id}>
                  <td className="font-medium text-ink">{x.id}</td>
                  <td className="uppercase">{x.formato}</td>
                  <td className="whitespace-nowrap">{x.desde || x.hasta ? `${fechaCorta(x.desde)} – ${fechaCorta(x.hasta)}` : 'Todo'}</td>
                  <td>
                    <EstadoBadge estado={x.estado} mapa={ESTADO_EXPORTACION} />
                    {x.mensajeError && <p className="mt-1 text-xs text-brand">{x.mensajeError}</p>}
                  </td>
                  <td>{x.registros != null ? numero(x.registros) : '—'}</td>
                  <td className="whitespace-nowrap">{fechaHora(x.creadoEn)}</td>
                  <td className="text-right">
                    {x.estado === 'LISTO' ? (
                      <Button tamano="sm" variante="outline" icono={Download} cargando={descargando === x.id} onClick={() => descargar(x)}>
                        Descargar
                      </Button>
                    ) : (
                      <span className="text-xs text-ink-faint">{x.estado === 'ERROR' ? 'No disponible' : 'Generando…'}</span>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </Card>
      )}
      <Paginacion pagina={data?.pagina || 0} totalPaginas={data?.totalPaginas || 0} alCambiar={setPagina} />
    </div>
  );
}
