import { useEffect, useRef } from 'react';
import { createPortal } from 'react-dom';
import { X } from 'lucide-react';
import Button from './Button';

/**
 * Modal accesible con efecto glass: cierra con Escape y clic en el fondo, bloquea el scroll del body
 * y devuelve el foco al elemento que lo abrió.
 */
export default function Modal({ abierto, alCerrar, titulo, descripcion, children, pie, ancho = 'max-w-lg', cerrarConFondo = true }) {
  const panel = useRef(null);
  const previo = useRef(null);
  const cerrarRef = useRef(alCerrar);
  cerrarRef.current = alCerrar;

  // Solo depende de "abierto": si dependiera de alCerrar (nueva función en cada render del padre),
  // el foco saltaría al panel con cada tecla escrita dentro del modal.
  useEffect(() => {
    if (!abierto) return undefined;
    previo.current = document.activeElement;
    const overflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    const alTeclear = (e) => e.key === 'Escape' && cerrarRef.current?.();
    window.addEventListener('keydown', alTeclear);
    const t = setTimeout(() => panel.current?.focus(), 10);
    return () => {
      clearTimeout(t);
      document.body.style.overflow = overflow;
      window.removeEventListener('keydown', alTeclear);
      previo.current?.focus?.();
    };
  }, [abierto]);

  if (!abierto) return null;

  return createPortal(
    <div className="fixed inset-0 z-[70] flex items-end justify-center p-0 sm:items-center sm:p-6" role="dialog" aria-modal="true" aria-label={titulo}>
      <div className="absolute inset-0 animate-fade-in bg-black/35 backdrop-blur-[3px]" onClick={cerrarConFondo ? alCerrar : undefined} />
      <div
        ref={panel}
        tabIndex={-1}
        className={`relative flex max-h-[92vh] w-full ${ancho} animate-rise-in flex-col overflow-hidden rounded-t-3xl border border-white/60 bg-white/90 shadow-lift outline-none backdrop-blur-md sm:rounded-3xl`}
      >
        {(titulo || alCerrar) && (
          <div className="flex items-start justify-between gap-4 px-6 pb-2 pt-6">
            <div>
              {titulo && <h2 className="text-xl font-semibold text-ink">{titulo}</h2>}
              {descripcion && <p className="mt-1 text-sm text-ink-muted">{descripcion}</p>}
            </div>
            {alCerrar && (
              <button type="button" onClick={alCerrar} className="-mr-2 rounded-full p-2 text-ink-muted transition hover:bg-black/5 hover:text-ink" aria-label="Cerrar">
                <X className="h-5 w-5" />
              </button>
            )}
          </div>
        )}
        <div className="overflow-y-auto px-6 py-4">{children}</div>
        {pie && <div className="flex flex-wrap justify-end gap-2 border-t border-line/60 bg-canvas/40 px-6 py-4">{pie}</div>}
      </div>
    </div>,
    document.body,
  );
}

/** Diálogo de confirmación con campo opcional de motivo. */
export function Confirmar({ abierto, alCerrar, alConfirmar, titulo, mensaje, textoConfirmar = 'Confirmar', variante = 'primary', cargando, children }) {
  return (
    <Modal
      abierto={abierto}
      alCerrar={cargando ? undefined : alCerrar}
      titulo={titulo}
      ancho="max-w-md"
      pie={
        <>
          <Button variante="ghost" onClick={alCerrar} disabled={cargando}>
            Cancelar
          </Button>
          <Button variante={variante} onClick={alConfirmar} cargando={cargando}>
            {textoConfirmar}
          </Button>
        </>
      }
    >
      {mensaje && <p className="text-[15px] leading-relaxed text-ink-soft">{mensaje}</p>}
      {children}
    </Modal>
  );
}
