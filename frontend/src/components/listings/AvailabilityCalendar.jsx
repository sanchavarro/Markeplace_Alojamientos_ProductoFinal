import { useMemo, useState } from 'react';
import { ChevronLeft, ChevronRight } from 'lucide-react';
import { aIsoFecha, hoyIso, parseFecha, sumarDias } from '../../utils/format';

const DIAS = ['L', 'M', 'X', 'J', 'V', 'S', 'D'];

/**
 * Estado de cada noche según los periodos del backend (HU-17), con la misma semántica de
 * DisponibilidadServiceImpl.estaDisponible:
 *  - Reservada/no disponible: hay un periodo RESERVADO/BLOQUEADO con fechaInicio <= noche < fechaFin.
 *  - Disponible: hay un periodo DISPONIBLE con fechaInicio <= noche y fechaFin >= noche + 1.
 *  - En cualquier otro caso la noche no se ofrece.
 */
export const estadoNoche = (iso, periodos) => {
  if (iso < hoyIso()) return 'pasado';
  const siguiente = sumarDias(iso, 1);
  if (periodos.some((p) => p.estado === 'RESERVADO' && p.fechaInicio <= iso && iso < p.fechaFin)) return 'reservado';
  if (periodos.some((p) => p.estado === 'BLOQUEADO' && p.fechaInicio <= iso && iso < p.fechaFin)) return 'bloqueado';
  if (periodos.some((p) => p.estado === 'DISPONIBLE' && p.fechaInicio <= iso && p.fechaFin >= siguiente)) return 'disponible';
  return 'sin_oferta';
};

/** ¿Todas las noches entre llegada (incluida) y salida (excluida) están disponibles? */
export const rangoDisponible = (llegada, salida, periodos) => {
  if (!llegada || !salida || salida <= llegada) return false;
  for (let d = llegada; d < salida; d = sumarDias(d, 1)) {
    if (estadoNoche(d, periodos) !== 'disponible') return false;
  }
  return true;
};

const ESTILO = {
  disponible: 'text-ink hover:border-ink cursor-pointer',
  reservado: 'text-ink-faint line-through decoration-brand/70 cursor-not-allowed',
  bloqueado: 'text-ink-faint line-through cursor-not-allowed',
  sin_oferta: 'text-ink-faint/70 cursor-not-allowed',
  pasado: 'text-ink-faint/50 cursor-not-allowed',
};

function Mes({ anio, mes, periodos, llegada, salida, alElegir }) {
  const primero = new Date(anio, mes, 1);
  const desfase = (primero.getDay() + 6) % 7;
  const diasMes = new Date(anio, mes + 1, 0).getDate();
  const celdas = [...Array(desfase).fill(null), ...Array.from({ length: diasMes }, (_, i) => aIsoFecha(new Date(anio, mes, i + 1)))];

  return (
    <div>
      <p className="mb-3 text-center text-[15px] font-semibold capitalize">{primero.toLocaleDateString('es-CO', { month: 'long', year: 'numeric' })}</p>
      <div className="grid grid-cols-7 gap-y-1 text-center">
        {DIAS.map((d) => (
          <span key={d} className="pb-1 text-[11px] font-semibold text-ink-faint">
            {d}
          </span>
        ))}
        {celdas.map((iso, i) => {
          if (!iso) return <span key={`v-${i}`} />;
          const estado = estadoNoche(iso, periodos);
          const esExtremo = iso === llegada || iso === salida;
          const enRango = llegada && salida && iso > llegada && iso < salida;
          // El día de salida se puede elegir aunque esa noche no esté disponible.
          const seleccionable = estado === 'disponible' || (llegada && !salida && iso > llegada);
          return (
            <div key={iso} className={`relative flex h-10 items-center justify-center ${enRango ? 'bg-canvas' : ''} ${iso === llegada && salida ? 'rounded-l-full bg-canvas' : ''} ${iso === salida ? 'rounded-r-full bg-canvas' : ''}`}>
              <button
                type="button"
                disabled={!alElegir || !seleccionable}
                onClick={() => alElegir?.(iso)}
                title={{ disponible: 'Disponible', reservado: 'Reservado', bloqueado: 'No disponible', sin_oferta: 'No disponible', pasado: '' }[estado]}
                className={`flex h-10 w-10 items-center justify-center rounded-full border border-transparent text-[13.5px] font-medium transition ${
                  esExtremo ? 'bg-ink !text-white no-underline' : ESTILO[estado]
                } ${!alElegir && estado === 'disponible' ? 'cursor-default hover:border-transparent' : ''}`}
              >
                {Number(iso.slice(8))}
              </button>
              {estado === 'disponible' && !esExtremo && <span className="absolute bottom-1 h-1 w-1 rounded-full bg-emerald-500" />}
            </div>
          );
        })}
      </div>
    </div>
  );
}

export default function AvailabilityCalendar({ periodos = [], llegada, salida, alCambiarRango }) {
  const hoy = parseFecha(hoyIso());
  const [base, setBase] = useState({ anio: hoy.getFullYear(), mes: hoy.getMonth() });

  const siguiente = useMemo(() => {
    const d = new Date(base.anio, base.mes + 1, 1);
    return { anio: d.getFullYear(), mes: d.getMonth() };
  }, [base]);

  const mover = (delta) =>
    setBase((b) => {
      const d = new Date(b.anio, b.mes + delta, 1);
      if (d < new Date(hoy.getFullYear(), hoy.getMonth(), 1)) return b;
      return { anio: d.getFullYear(), mes: d.getMonth() };
    });

  const elegir = alCambiarRango
    ? (iso) => {
        if (!llegada || salida || iso <= llegada) alCambiarRango(iso, '');
        else alCambiarRango(llegada, iso);
      }
    : undefined;

  return (
    <div>
      <div className="relative">
        <div className="absolute inset-x-0 top-0 flex justify-between">
          <button type="button" onClick={() => mover(-1)} className="flex h-8 w-8 items-center justify-center rounded-full hover:bg-black/5" aria-label="Mes anterior">
            <ChevronLeft className="h-4 w-4" />
          </button>
          <button type="button" onClick={() => mover(1)} className="flex h-8 w-8 items-center justify-center rounded-full hover:bg-black/5" aria-label="Mes siguiente">
            <ChevronRight className="h-4 w-4" />
          </button>
        </div>
        <div className="grid gap-10 md:grid-cols-2">
          <Mes {...base} periodos={periodos} llegada={llegada} salida={salida} alElegir={elegir} />
          <div className="hidden md:block">
            <Mes {...siguiente} periodos={periodos} llegada={llegada} salida={salida} alElegir={elegir} />
          </div>
        </div>
      </div>
      <div className="mt-5 flex flex-wrap gap-x-5 gap-y-2 text-xs text-ink-muted">
        <span className="flex items-center gap-1.5">
          <span className="h-2 w-2 rounded-full bg-emerald-500" /> Disponible
        </span>
        <span className="flex items-center gap-1.5">
          <span className="text-ink-faint line-through decoration-brand">12</span> Reservado
        </span>
        <span className="flex items-center gap-1.5">
          <span className="text-ink-faint line-through">12</span> No disponible
        </span>
        {alCambiarRango && (llegada || salida) && (
          <button type="button" onClick={() => alCambiarRango('', '')} className="ml-auto font-medium text-ink underline underline-offset-2">
            Borrar fechas
          </button>
        )}
      </div>
    </div>
  );
}
