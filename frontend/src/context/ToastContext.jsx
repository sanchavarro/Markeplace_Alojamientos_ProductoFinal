import { createContext, useCallback, useContext, useMemo, useRef, useState } from 'react';
import { CircleCheck, TriangleAlert, Info, CircleX, X } from 'lucide-react';

const ToastContext = createContext(null);

const ESTILOS = {
  exito: { icono: CircleCheck, color: 'text-emerald-600' },
  error: { icono: CircleX, color: 'text-brand' },
  aviso: { icono: TriangleAlert, color: 'text-amber-500' },
  info: { icono: Info, color: 'text-accent' },
};

export function ToastProvider({ children }) {
  const [toasts, setToasts] = useState([]);
  const contador = useRef(0);

  const cerrar = useCallback((id) => setToasts((lista) => lista.filter((t) => t.id !== id)), []);

  const mostrar = useCallback(
    (tipo, mensaje, opciones = {}) => {
      contador.current += 1;
      const id = contador.current;
      setToasts((lista) => [...lista.slice(-3), { id, tipo, mensaje, titulo: opciones.titulo }]);
      setTimeout(() => cerrar(id), opciones.duracion || (tipo === 'error' ? 6500 : 4200));
      return id;
    },
    [cerrar],
  );

  const api = useMemo(
    () => ({
      exito: (m, o) => mostrar('exito', m, o),
      error: (m, o) => mostrar('error', typeof m === 'string' ? m : m?.message || 'Ocurrió un error.', o),
      aviso: (m, o) => mostrar('aviso', m, o),
      info: (m, o) => mostrar('info', m, o),
    }),
    [mostrar],
  );

  return (
    <ToastContext.Provider value={api}>
      {children}
      <div
        aria-live="polite"
        className="pointer-events-none fixed inset-x-0 bottom-4 z-[80] flex flex-col items-center gap-2 px-4 sm:bottom-6 sm:right-6 sm:left-auto sm:items-end"
      >
        {toasts.map((t) => {
          const { icono: Icono, color } = ESTILOS[t.tipo] || ESTILOS.info;
          return (
            <div
              key={t.id}
              role="status"
              className="glass pointer-events-auto flex w-full max-w-sm animate-rise-in items-start gap-3 rounded-2xl border border-white/60 p-4 shadow-lift"
            >
              <Icono className={`mt-0.5 h-5 w-5 shrink-0 ${color}`} />
              <div className="min-w-0 flex-1">
                {t.titulo && <p className="text-sm font-semibold text-ink">{t.titulo}</p>}
                <p className="text-sm text-ink-soft">{t.mensaje}</p>
              </div>
              <button
                type="button"
                onClick={() => cerrar(t.id)}
                className="rounded-full p-1 text-ink-faint transition hover:bg-black/5 hover:text-ink"
                aria-label="Cerrar aviso"
              >
                <X className="h-4 w-4" />
              </button>
            </div>
          );
        })}
      </div>
    </ToastContext.Provider>
  );
}

export const useToast = () => {
  const ctx = useContext(ToastContext);
  if (!ctx) throw new Error('useToast debe usarse dentro de <ToastProvider>.');
  return ctx;
};
