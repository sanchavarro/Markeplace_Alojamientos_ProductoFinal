import { useState } from 'react';
import { Search, SlidersHorizontal } from 'lucide-react';
import { hoyIso, sumarDias } from '../../utils/format';
import { validarFechasEstadia } from '../../utils/validators';

/**
 * Barra de búsqueda tipo píldora (HU-19): ciudad, llegada, salida y huéspedes.
 * Valida las fechas antes de buscar (HU-25) y abre el panel de filtros avanzados (HU-20).
 */
export default function SearchBar({ valores, alBuscar, alAbrirFiltros, filtrosActivos = 0 }) {
  const [f, setF] = useState({
    ciudad: valores.ciudad || '',
    llegada: valores.llegada || '',
    salida: valores.salida || '',
    capacidad: valores.capacidad || '',
  });
  const [error, setError] = useState(null);

  const cambiar = (campo) => (e) => {
    const valor = e.target.value;
    setF((prev) => {
      const nuevo = { ...prev, [campo]: valor };
      if (campo === 'llegada' && valor && (!prev.salida || prev.salida <= valor)) nuevo.salida = sumarDias(valor, 1);
      return nuevo;
    });
    setError(null);
  };

  const enviar = (e) => {
    e.preventDefault();
    if (f.llegada || f.salida) {
      const err = validarFechasEstadia(f.llegada, f.salida);
      if (err) {
        setError(err);
        return;
      }
    }
    if (f.capacidad && (Number(f.capacidad) < 1 || Number(f.capacidad) > 50)) {
      setError('El número de huéspedes debe estar entre 1 y 50.');
      return;
    }
    alBuscar({ ...f, ciudad: f.ciudad.trim() });
  };

  const segmento = 'flex min-w-0 flex-1 flex-col justify-center rounded-full px-6 py-2.5 transition hover:bg-black/[0.04] focus-within:bg-white focus-within:shadow-lift';
  const etiqueta = 'text-[11.5px] font-semibold text-ink';
  const control = 'w-full bg-transparent text-[14px] text-ink outline-none placeholder:text-ink-faint';

  return (
    <div className="w-full">
      <form onSubmit={enviar} className="mx-auto flex w-full max-w-4xl flex-col gap-2 md:flex-row md:items-center">
        <div className="flex flex-1 flex-col rounded-3xl border border-line/70 bg-white shadow-pill md:flex-row md:items-center md:rounded-full md:p-1.5">
          <label className={segmento}>
            <span className={etiqueta}>Destino</span>
            <input className={control} placeholder="¿A qué ciudad vas?" value={f.ciudad} onChange={cambiar('ciudad')} maxLength={80} />
          </label>
          <span className="hidden h-8 w-px bg-line md:block" />
          <label className={segmento}>
            <span className={etiqueta}>Llegada</span>
            <input type="date" className={control} min={hoyIso()} value={f.llegada} onChange={cambiar('llegada')} />
          </label>
          <span className="hidden h-8 w-px bg-line md:block" />
          <label className={segmento}>
            <span className={etiqueta}>Salida</span>
            <input type="date" className={control} min={f.llegada ? sumarDias(f.llegada, 1) : sumarDias(hoyIso(), 1)} value={f.salida} onChange={cambiar('salida')} />
          </label>
          <span className="hidden h-8 w-px bg-line md:block" />
          <div className="flex items-center md:flex-[1.1]">
            <label className={segmento}>
              <span className={etiqueta}>Huéspedes</span>
              <input type="number" min={1} max={50} className={control} placeholder="¿Cuántos?" value={f.capacidad} onChange={cambiar('capacidad')} />
            </label>
            <button
              type="submit"
              className="m-1.5 flex h-12 shrink-0 items-center justify-center gap-2 rounded-full bg-brand px-5 text-white shadow-sm transition hover:brightness-95 active:scale-95"
              aria-label="Buscar alojamientos"
            >
              <Search className="h-[18px] w-[18px]" strokeWidth={2.4} />
              <span className="text-[15px] font-semibold md:hidden lg:inline">Buscar</span>
            </button>
          </div>
        </div>
        <button
          type="button"
          onClick={alAbrirFiltros}
          className="relative flex h-12 items-center justify-center gap-2 rounded-full border border-line/70 bg-white px-5 text-[14px] font-medium shadow-pill transition hover:shadow-soft md:h-[64px]"
        >
          <SlidersHorizontal className="h-4 w-4" /> Filtros
          {filtrosActivos > 0 && (
            <span className="flex h-5 min-w-5 items-center justify-center rounded-full bg-ink px-1.5 text-[11px] font-semibold text-white">{filtrosActivos}</span>
          )}
        </button>
      </form>
      {error && (
        <p className="mt-3 text-center text-sm text-brand" role="alert">
          {error}
        </p>
      )}
    </div>
  );
}
