import { useState } from 'react';
import { Link } from 'react-router-dom';
import { Plus, House, Settings2, Eye, Star } from 'lucide-react';
import { alojamientosApi } from '../../api';
import { useAsync } from '../../hooks/useAsync';
import { useToast } from '../../context/ToastContext';
import { Button, EmptyState, ErrorState, EstadoBadge, Paginacion, SafeImage, SectionTitle, Skeleton } from '../../components/ui';
import { ACCIONES_ANFITRION, ESTADO_ALOJAMIENTO, TIPO_LABEL } from '../../utils/constants';
import { calificacion, dinero } from '../../utils/format';

/** Panel del anfitrión: mis publicaciones (GET /alojamientos/mios) con acciones de estado (HU-09, HU-10). */
export default function HostAlojamientosPage() {
  const [pagina, setPagina] = useState(0);
  const toast = useToast();
  const [cambiando, setCambiando] = useState(null);
  const { data, error, cargando, recargar } = useAsync(() => alojamientosApi.mios({ pagina, tamano: 12 }), [pagina]);
  const lista = data?.contenido || [];

  const accion = async (a, acc) => {
    setCambiando(a.id);
    try {
      const r = await alojamientosApi.cambiarEstado(a.id, acc.accion);
      toast.exito(`«${a.titulo}» ahora está ${ESTADO_ALOJAMIENTO[r.estado]?.label.toLowerCase() || r.estado}.`);
      recargar();
    } catch (e) {
      toast.error(e, { titulo: 'No se pudo cambiar el estado' });
    } finally {
      setCambiando(null);
    }
  };

  return (
    <div>
      <SectionTitle
        titulo="Mis alojamientos"
        subtitulo="Crea, edita y controla cuándo recibe reservas cada publicación."
        accion={
          <Button icono={Plus} to="/anfitrion/alojamientos/nuevo">
            Nuevo alojamiento
          </Button>
        }
      />
      {error ? (
        <ErrorState error={error} alReintentar={recargar} />
      ) : cargando ? (
        <div className="grid gap-6 sm:grid-cols-2 xl:grid-cols-3">
          {[0, 1, 2].map((i) => (
            <Skeleton key={i} className="h-80 rounded-3xl" />
          ))}
        </div>
      ) : lista.length === 0 ? (
        <EmptyState
          icono={House}
          titulo="Publica tu primer alojamiento"
          descripcion="Empieza con un borrador: agrega fotos, servicios y disponibilidad, y envíalo a revisión cuando esté listo."
          accion={
            <Button icono={Plus} to="/anfitrion/alojamientos/nuevo">
              Crear alojamiento
            </Button>
          }
        />
      ) : (
        <div className="grid gap-6 sm:grid-cols-2 xl:grid-cols-3">
          {lista.map((a) => (
            <article key={a.id} className="group overflow-hidden rounded-3xl border border-line/50 bg-white shadow-soft transition hover:shadow-lift">
              <Link to={`/anfitrion/alojamientos/${a.id}`} className="relative block">
                <SafeImage src={a.imagenPrincipal} semilla={a.id} alt={a.titulo} className="aspect-[16/10] w-full" imgClassName="transition duration-700 group-hover:scale-[1.03]" />
                <span className="absolute left-3 top-3">
                  <EstadoBadge estado={a.estado} mapa={ESTADO_ALOJAMIENTO} />
                </span>
              </Link>
              <div className="p-5">
                <p className="text-xs font-medium text-ink-muted">
                  {TIPO_LABEL[a.tipo]} · {a.ciudad}
                </p>
                <h3 className="mt-1 line-clamp-1 text-[17px] font-semibold">{a.titulo}</h3>
                <div className="mt-2 flex items-center justify-between text-[14px]">
                  <span>
                    <strong>{dinero(a.precioNoche)}</strong> <span className="text-ink-muted">/ noche</span>
                  </span>
                  <span className="flex items-center gap-1 text-ink-soft">
                    <Star className="h-3.5 w-3.5 fill-ink" /> {calificacion(a.promedioCalificacion) || 'Nuevo'}
                  </span>
                </div>
                <div className="mt-5 flex flex-wrap gap-2">
                  <Button tamano="sm" icono={Settings2} to={`/anfitrion/alojamientos/${a.id}`}>
                    Gestionar
                  </Button>
                  <Button tamano="sm" variante="outline" icono={Eye} to={`/alojamientos/${a.id}`}>
                    Ver
                  </Button>
                  {(ACCIONES_ANFITRION[a.estado] || []).map((acc) => (
                    <Button key={acc.accion} tamano="sm" variante="secondary" cargando={cambiando === a.id} onClick={() => accion(a, acc)}>
                      {acc.label}
                    </Button>
                  ))}
                </div>
              </div>
            </article>
          ))}
        </div>
      )}
      <Paginacion pagina={data?.pagina || 0} totalPaginas={data?.totalPaginas || 0} alCambiar={setPagina} />
    </div>
  );
}
