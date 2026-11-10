import { useNavigate } from 'react-router-dom';
import { X, ArrowRight } from 'lucide-react';
import { useCompare } from '../../context/CompareContext';
import { MAX_COMPARAR } from '../../utils/constants';
import SafeImage from '../ui/SafeImage';
import { Button } from '../ui';

/** Barra flotante glass con la selección para comparar (HU-22). */
export default function CompareBar() {
  const { seleccion, quitar, vaciar } = useCompare();
  const navegar = useNavigate();
  if (seleccion.length === 0) return null;

  return (
    <div className="pointer-events-none fixed inset-x-0 bottom-5 z-30 flex justify-center px-4">
      <div className="glass pointer-events-auto flex w-full max-w-2xl animate-rise-in items-center gap-3 rounded-full border border-white/70 py-2 pl-2 pr-2 shadow-lift">
        <div className="flex -space-x-2">
          {Array.from({ length: MAX_COMPARAR }).map((_, i) => {
            const a = seleccion[i];
            return a ? (
              <div key={a.id} className="group relative">
                <SafeImage src={a.imagenPrincipal} semilla={a.id} alt={a.titulo} className="h-11 w-11 rounded-full ring-2 ring-white" />
                <button
                  type="button"
                  onClick={() => quitar(a.id)}
                  className="absolute -right-1 -top-1 hidden h-5 w-5 items-center justify-center rounded-full bg-ink text-white group-hover:flex"
                  aria-label={`Quitar ${a.titulo}`}
                >
                  <X className="h-3 w-3" />
                </button>
              </div>
            ) : (
              <div key={`vacio-${i}`} className="h-11 w-11 rounded-full border-2 border-dashed border-line bg-white/60 ring-2 ring-white" />
            );
          })}
        </div>
        <p className="min-w-0 flex-1 truncate text-[13.5px] text-ink-soft">
          <span className="font-semibold text-ink">{seleccion.length}</span> de {MAX_COMPARAR} para comparar
          {seleccion.length < 2 && <span className="hidden sm:inline"> · elige al menos 2</span>}
        </p>
        <button type="button" onClick={vaciar} className="hidden text-[13px] font-medium text-ink-muted hover:text-ink sm:block">
          Limpiar
        </button>
        <Button tamano="sm" disabled={seleccion.length < 2} onClick={() => navegar(`/comparar?ids=${seleccion.map((a) => a.id).join(',')}`)}>
          Comparar <ArrowRight className="h-4 w-4" />
        </Button>
      </div>
    </div>
  );
}
