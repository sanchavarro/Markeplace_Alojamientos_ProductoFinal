import { useState } from 'react';
import { Link } from 'react-router-dom';
import { CircleCheck, CirclePause, Ban, LockOpen, Inbox, Search } from 'lucide-react';
import { adminApi, alojamientosApi } from '../../api';
import { useAsync } from '../../hooks/useAsync';
import { useToast } from '../../context/ToastContext';
import { Button, Card, Confirmar, EmptyState, ErrorState, EstadoBadge, Input, Paginacion, SafeImage, SectionTitle, Skeleton } from '../../components/ui';
import { ESTADO_ALOJAMIENTO, TIPO_LABEL } from '../../utils/constants';
import { dinero } from '../../utils/format';
import { fotosDeAlojamiento } from '../../utils/images';

const ACCIONES = {
  APROBAR: { label: 'Aprobar', icono: CircleCheck, variante: 'primary', texto: 'La publicación pasará a ACTIVA y aparecerá en la búsqueda.' },
  PAUSAR: { label: 'Pausar', icono: CirclePause, variante: 'outline', texto: 'La publicación quedará fuera del catálogo.' },
  BLOQUEAR: { label: 'Bloquear', icono: Ban, variante: 'brand', texto: 'No aparecerá en búsquedas ni aceptará reservas. Sus reservas futuras pasan a revisión manual y se notifica al anfitrión.' },
  DESBLOQUEAR: { label: 'Desbloquear', icono: LockOpen, variante: 'outline', texto: 'La publicación saldrá del estado BLOQUEADA.' },
};

/** Moderación de publicaciones (HU-42, HU-43) con vista previa de fotos y motivo auditado. */
export default function ModeracionPage() {
  const toast = useToast();
  const [pagina, setPagina] = useState(0);
  const [accion, setAccion] = useState(null);
  const [motivo, setMotivo] = useState('');
  const [enviando, setEnviando] = useState(false);
  const [buscarId, setBuscarId] = useState('');
  const [encontrada, setEncontrada] = useState(null);
  const [buscando, setBuscando] = useState(false);
  const { data, error, cargando, recargar } = useAsync(() => adminApi.pendientes({ pagina, tamano: 10 }), [pagina]);
  const lista = data?.contenido || [];

  const ejecutar = async () => {
    setEnviando(true);
    try {
      const r = await adminApi.gestionarPublicacion(accion.alojamiento.id, accion.tipo, motivo);
      toast.exito(`«${r.titulo}» ahora está ${ESTADO_ALOJAMIENTO[r.estado]?.label.toLowerCase() || r.estado}.`);
      if (encontrada?.id === r.id) setEncontrada(r);
      setAccion(null);
      setMotivo('');
      recargar();
    } catch (e) {
      toast.error(e);
    } finally {
      setEnviando(false);
    }
  };

  const buscar = async (e) => {
    e.preventDefault();
    if (!buscarId) return;
    setBuscando(true);
    try {
      setEncontrada(await alojamientosApi.detalle(buscarId));
    } catch (er) {
      setEncontrada(null);
      toast.error(er);
    } finally {
      setBuscando(false);
    }
  };

  const botones = (a, tipos) =>
    tipos.map((t) => (
      <Button key={t} tamano="sm" variante={ACCIONES[t].variante} icono={ACCIONES[t].icono} onClick={() => setAccion({ tipo: t, alojamiento: a })}>
        {ACCIONES[t].label}
      </Button>
    ));

  return (
    <div>
      <SectionTitle titulo="Moderación" subtitulo="Revisa las publicaciones nuevas antes de que lleguen al catálogo." />

      {error ? (
        <ErrorState error={error} alReintentar={recargar} />
      ) : cargando ? (
        <Skeleton className="h-64 w-full rounded-3xl" />
      ) : lista.length === 0 ? (
        <EmptyState icono={Inbox} titulo="No hay publicaciones pendientes" descripcion="Todo está revisado. Las nuevas solicitudes aparecerán aquí." />
      ) : (
        <div className="space-y-5">
          {lista.map((a) => (
            <PendienteCard key={a.id} resumen={a} acciones={botones(a, ['APROBAR', 'PAUSAR', 'BLOQUEAR'])} />
          ))}
        </div>
      )}
      <Paginacion pagina={data?.pagina || 0} totalPaginas={data?.totalPaginas || 0} alCambiar={setPagina} />

      <Card className="mt-10">
        <h2 className="text-lg font-semibold">Gestionar cualquier publicación</h2>
        <p className="mt-1 text-sm text-ink-muted">Busca por número para bloquear, desbloquear o pausar una publicación ya activa.</p>
        <form onSubmit={buscar} className="mt-4 flex gap-2">
          <Input type="number" min={1} placeholder="ID del alojamiento" value={buscarId} onChange={(e) => setBuscarId(e.target.value)} className="w-56" aria-label="ID del alojamiento" />
          <Button type="submit" variante="secondary" icono={Search} cargando={buscando}>
            Buscar
          </Button>
        </form>
        {encontrada && (
          <div className="mt-5 flex flex-col gap-4 rounded-2xl bg-canvas p-4 sm:flex-row sm:items-center">
            <SafeImage src={fotosDeAlojamiento(encontrada)[0]} semilla={encontrada.id} className="h-20 w-28 shrink-0 rounded-xl" />
            <div className="min-w-0 flex-1">
              <EstadoBadge estado={encontrada.estado} mapa={ESTADO_ALOJAMIENTO} />
              <p className="mt-1 font-semibold">{encontrada.titulo}</p>
              <p className="text-sm text-ink-muted">
                {encontrada.ciudad} · Anfitrión: {encontrada.anfitrionNombre}
              </p>
            </div>
            <div className="flex flex-wrap gap-2">
              {botones(encontrada, encontrada.estado === 'BLOQUEADA' ? ['DESBLOQUEAR'] : encontrada.estado === 'ACTIVA' ? ['PAUSAR', 'BLOQUEAR'] : ['APROBAR', 'BLOQUEAR'])}
              <Button tamano="sm" variante="ghost" to={`/admin/alojamientos/${encontrada.id}/editar`}>
                Editar
              </Button>
            </div>
          </div>
        )}
      </Card>

      <Confirmar
        abierto={Boolean(accion)}
        alCerrar={() => setAccion(null)}
        alConfirmar={ejecutar}
        cargando={enviando}
        titulo={accion ? `${ACCIONES[accion.tipo].label} «${accion.alojamiento.titulo}»` : ''}
        mensaje={accion && ACCIONES[accion.tipo].texto}
        textoConfirmar={accion ? ACCIONES[accion.tipo].label : ''}
        variante={accion?.tipo === 'BLOQUEAR' ? 'brand' : 'primary'}
      >
        <Input className="mt-4" label="Motivo" opcional maxLength={300} value={motivo} onChange={(e) => setMotivo(e.target.value)} placeholder="Queda registrado en auditoría" />
      </Confirmar>
    </div>
  );
}

/** Tarjeta de revisión: carga el detalle para previsualizar todas las fotos del alojamiento. */
function PendienteCard({ resumen, acciones }) {
  const { data: a } = useAsync(() => alojamientosApi.detalle(resumen.id), [resumen.id]);
  const fotos = a ? fotosDeAlojamiento(a, 1) : [resumen.imagenPrincipal];
  return (
    <Card padding="p-0" className="overflow-hidden">
      <div className="no-scrollbar flex gap-1 overflow-x-auto">
        {fotos.slice(0, 6).map((f, i) => (
          <SafeImage key={`${f}-${i}`} src={f} semilla={resumen.id + i} className={`h-44 shrink-0 ${i === 0 ? 'w-72' : 'w-48'}`} />
        ))}
      </div>
      <div className="flex flex-col gap-4 p-5 lg:flex-row lg:items-start lg:justify-between">
        <div className="min-w-0">
          <p className="text-xs font-medium text-ink-muted">
            #{resumen.id} · {TIPO_LABEL[resumen.tipo]} · {resumen.ciudad} · {dinero(resumen.precioNoche)} / noche · {resumen.capacidad} huéspedes
          </p>
          <h3 className="mt-1 text-[18px] font-semibold">{resumen.titulo}</h3>
          {a && (
            <>
              <p className="mt-1 text-sm text-ink-muted">Anfitrión: {a.anfitrionNombre} · Política: {a.politica?.nombre || '—'} · {a.imagenes?.length || 0} fotos</p>
              <p className="mt-2 line-clamp-2 max-w-2xl text-[14px] text-ink-soft">{a.descripcion}</p>
            </>
          )}
          <Link to={`/alojamientos/${resumen.id}`} className="mt-2 inline-block text-sm font-medium text-accent hover:underline">
            Ver la publicación completa
          </Link>
        </div>
        <div className="flex shrink-0 flex-wrap gap-2">{acciones}</div>
      </div>
    </Card>
  );
}
