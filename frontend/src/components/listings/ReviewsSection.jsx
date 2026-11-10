import { useCallback, useEffect, useState } from 'react';
import { Star, EyeOff } from 'lucide-react';
import { adminApi, alojamientosApi } from '../../api';
import { useAuth } from '../../context/AuthContext';
import { useToast } from '../../context/ToastContext';
import { fechaLarga, plural } from '../../utils/format';
import { ROLES } from '../../utils/constants';
import { Button, Confirmar, Estrellas, Input, Skeleton } from '../ui';

/**
 * Reseñas y promedio del alojamiento (HU-40): solo las visibles cuentan en el promedio.
 * El administrador puede ocultar una reseña desde aquí con motivo (HU-41).
 */
export default function ReviewsSection({ alojamientoId }) {
  const { rol } = useAuth();
  const toast = useToast();
  const [datos, setDatos] = useState(null);
  const [lista, setLista] = useState([]);
  const [pagina, setPagina] = useState(0);
  const [cargando, setCargando] = useState(true);
  const [ocultar, setOcultar] = useState(null);
  const [motivo, setMotivo] = useState('');
  const [enviando, setEnviando] = useState(false);

  const cargar = useCallback(
    async (p = 0) => {
      setCargando(true);
      try {
        const r = await alojamientosApi.resenas(alojamientoId, { pagina: p, tamano: 6 });
        setDatos(r);
        setPagina(p);
        setLista((prev) => (p === 0 ? r?.resenas?.contenido || [] : [...prev, ...(r?.resenas?.contenido || [])]));
      } catch (e) {
        toast.error(e);
      } finally {
        setCargando(false);
      }
    },
    [alojamientoId, toast],
  );

  useEffect(() => {
    cargar(0);
  }, [cargar]);

  const confirmarOcultar = async () => {
    setEnviando(true);
    try {
      await adminApi.ocultarResena(ocultar.id, motivo);
      toast.exito(`Reseña #${ocultar.id} ocultada. Quedó registrada en auditoría.`);
      setOcultar(null);
      setMotivo('');
      cargar(0);
    } catch (e) {
      toast.error(e);
    } finally {
      setEnviando(false);
    }
  };

  const total = datos?.total || 0;
  const hayMas = datos?.resenas && pagina < (datos.resenas.totalPaginas || 0) - 1;

  return (
    <section id="resenas">
      <h2 className="flex items-center gap-2 text-[22px] font-semibold">
        <Star className="h-5 w-5 fill-ink" />
        {datos?.promedio ? `${Number(datos.promedio).toFixed(1)} · ${plural(total, 'reseña')}` : 'Aún sin reseñas'}
      </h2>

      {cargando && lista.length === 0 ? (
        <div className="mt-6 grid gap-8 md:grid-cols-2">
          {[0, 1].map((i) => (
            <div key={i} className="space-y-2">
              <Skeleton className="h-4 w-1/3" />
              <Skeleton className="h-3.5 w-full" />
              <Skeleton className="h-3.5 w-4/5" />
            </div>
          ))}
        </div>
      ) : lista.length === 0 ? (
        <p className="mt-3 text-[15px] text-ink-muted">Las reseñas aparecen cuando los huéspedes completan su estadía.</p>
      ) : (
        <div className="mt-6 grid gap-x-12 gap-y-8 md:grid-cols-2">
          {lista.map((r) => (
            <article key={r.id} className="group">
              <div className="flex items-center gap-3">
                <span className="flex h-10 w-10 items-center justify-center rounded-full bg-canvas text-sm font-semibold text-ink-soft">H{String(r.huespedId).slice(-2)}</span>
                <div>
                  <p className="text-[15px] font-semibold">Huésped verificado</p>
                  <p className="text-[13px] text-ink-muted">{fechaLarga(r.creadaEn)}</p>
                </div>
              </div>
              <div className="mt-2.5">
                <Estrellas valor={r.calificacion} tamano="h-3.5 w-3.5" />
              </div>
              {r.comentario && <p className="mt-2 text-[15px] leading-relaxed text-ink-soft">{r.comentario}</p>}
              {rol === ROLES.ADMIN && (
                <button type="button" onClick={() => setOcultar(r)} className="mt-2 inline-flex items-center gap-1.5 text-[13px] font-medium text-brand hover:underline">
                  <EyeOff className="h-3.5 w-3.5" /> Ocultar reseña
                </button>
              )}
            </article>
          ))}
        </div>
      )}

      {hayMas && (
        <Button variante="outline" className="mt-8" onClick={() => cargar(pagina + 1)} cargando={cargando}>
          Mostrar más reseñas
        </Button>
      )}

      <Confirmar
        abierto={Boolean(ocultar)}
        alCerrar={() => setOcultar(null)}
        alConfirmar={confirmarOcultar}
        cargando={enviando}
        titulo="Ocultar reseña"
        mensaje="La reseña dejará de mostrarse y no contará en el promedio. Podrás volver a mostrarla desde el panel de administración."
        textoConfirmar="Ocultar"
        variante="brand"
      >
        <Input className="mt-4" label="Motivo" opcional maxLength={300} value={motivo} onChange={(e) => setMotivo(e.target.value)} placeholder="Contenido inapropiado…" />
      </Confirmar>
    </section>
  );
}
