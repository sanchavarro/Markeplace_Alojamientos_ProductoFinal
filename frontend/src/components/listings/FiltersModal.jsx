import { useEffect, useState } from 'react';
import { Check } from 'lucide-react';
import { Button, Modal, Input } from '../ui';
import { TIPOS_ALOJAMIENTO, TIPO_LABEL } from '../../utils/constants';
import { validarRangoPrecio } from '../../utils/validators';

/** Filtros combinables con la búsqueda principal (HU-20): tipo, rango de precio y servicios. */
export default function FiltersModal({ abierto, alCerrar, valores, servicios, alAplicar }) {
  const [f, setF] = useState(valores);
  const [error, setError] = useState(null);

  // Se sincroniza solo al abrir, para no pisar lo que el usuario está editando.
  useEffect(() => {
    if (abierto) {
      setF(valores);
      setError(null);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [abierto]);

  const alternarServicio = (id) =>
    setF((p) => {
      const actuales = p.servicios || [];
      return { ...p, servicios: actuales.includes(id) ? actuales.filter((s) => s !== id) : [...actuales, id] };
    });

  const aplicar = () => {
    const err = validarRangoPrecio(f.precioMin, f.precioMax);
    if (err) {
      setError(err);
      return;
    }
    alAplicar(f);
  };

  const limpiar = () => setF({ ...f, tipo: '', precioMin: '', precioMax: '', servicios: [] });

  return (
    <Modal
      abierto={abierto}
      alCerrar={alCerrar}
      titulo="Filtros"
      ancho="max-w-2xl"
      pie={
        <div className="flex w-full items-center justify-between">
          <Button variante="link" onClick={limpiar}>
            Quitar filtros
          </Button>
          <Button onClick={aplicar}>Mostrar resultados</Button>
        </div>
      }
    >
      <section className="pb-6">
        <h3 className="mb-3 text-[17px] font-semibold">Tipo de alojamiento</h3>
        <div className="grid grid-cols-2 gap-2 sm:grid-cols-3">
          <button
            type="button"
            onClick={() => setF({ ...f, tipo: '' })}
            className={`rounded-2xl border px-4 py-3 text-left text-sm font-medium transition ${!f.tipo ? 'border-ink bg-ink text-white' : 'border-line hover:border-ink/40'}`}
          >
            Cualquier tipo
          </button>
          {TIPOS_ALOJAMIENTO.map((t) => (
            <button
              key={t}
              type="button"
              onClick={() => setF({ ...f, tipo: t })}
              className={`rounded-2xl border px-4 py-3 text-left text-sm font-medium transition ${f.tipo === t ? 'border-ink bg-ink text-white' : 'border-line hover:border-ink/40'}`}
            >
              {TIPO_LABEL[t]}
            </button>
          ))}
        </div>
      </section>

      <section className="border-t border-line/60 py-6">
        <h3 className="mb-1 text-[17px] font-semibold">Rango de precio por noche</h3>
        <p className="mb-4 text-sm text-ink-muted">Valores en pesos colombianos (COP).</p>
        <div className="grid grid-cols-2 gap-3">
          <Input label="Mínimo" type="number" min={0} step="1000" value={f.precioMin || ''} onChange={(e) => setF({ ...f, precioMin: e.target.value })} placeholder="0" />
          <Input label="Máximo" type="number" min={0} step="1000" value={f.precioMax || ''} onChange={(e) => setF({ ...f, precioMax: e.target.value })} placeholder="Sin límite" />
        </div>
        {error && <p className="mt-2 text-sm text-brand">{error}</p>}
      </section>

      <section className="border-t border-line/60 pt-6">
        <h3 className="mb-3 text-[17px] font-semibold">Servicios</h3>
        {servicios.length === 0 ? (
          <p className="text-sm text-ink-muted">No hay servicios en el catálogo.</p>
        ) : (
          <div className="grid grid-cols-1 gap-2 sm:grid-cols-2">
            {servicios.map((s) => {
              const activo = (f.servicios || []).includes(s.id);
              return (
                <button
                  key={s.id}
                  type="button"
                  onClick={() => alternarServicio(s.id)}
                  aria-pressed={activo}
                  className="flex items-center gap-3 rounded-xl px-2 py-2 text-left text-[15px] hover:bg-black/[0.03]"
                >
                  <span className={`flex h-5 w-5 items-center justify-center rounded-md border transition ${activo ? 'border-ink bg-ink text-white' : 'border-line'}`}>
                    {activo && <Check className="h-3.5 w-3.5" strokeWidth={3} />}
                  </span>
                  {s.nombre}
                </button>
              );
            })}
          </div>
        )}
      </section>
    </Modal>
  );
}
