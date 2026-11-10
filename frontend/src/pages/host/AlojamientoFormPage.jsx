import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { Check, ChevronLeft, Zap, UserCheck } from 'lucide-react';
import { alojamientosApi } from '../../api';
import { useCatalogos } from '../../hooks/useCatalogos';
import { useAuth } from '../../context/AuthContext';
import { useToast } from '../../context/ToastContext';
import { Button, Card, ErrorFormulario, Input, PantallaCarga, Select, Textarea, ErrorState } from '../../components/ui';
import { MODO_LABEL, ROLES, TIPOS_ALOJAMIENTO, TIPO_LABEL } from '../../utils/constants';
import { validarAlojamiento } from '../../utils/validators';

const VACIO = {
  titulo: '',
  descripcion: '',
  reglas: '',
  ciudad: '',
  direccion: '',
  tipo: '',
  capacidad: '',
  precioNoche: '',
  tarifaLimpieza: '',
  modoReserva: 'INMEDIATA',
  politicaId: '',
  servicios: [],
};

/**
 * Crear (HU-07) y editar (HU-08) una publicación con todas las validaciones de AlojamientoRequest
 * (HU-11, HU-12), tarifa de limpieza y política (HU-13) y modalidad de reserva (HU-14).
 * La publicación nace en BORRADOR; el backend vuelve a validar y responde 400/422 con el detalle.
 */
export default function AlojamientoFormPage() {
  const { id } = useParams();
  const editando = Boolean(id);
  const { rol } = useAuth();
  const navegar = useNavigate();
  const toast = useToast();
  const { politicas, servicios, cargando: cargandoCatalogos } = useCatalogos();
  const [f, setF] = useState(VACIO);
  const [errores, setErrores] = useState({});
  const [error, setError] = useState(null);
  const [cargando, setCargando] = useState(editando);
  const [errorCarga, setErrorCarga] = useState(null);
  const [guardando, setGuardando] = useState(false);

  useEffect(() => {
    if (!editando) return;
    alojamientosApi
      .detalle(id)
      .then((a) =>
        setF({
          titulo: a.titulo || '',
          descripcion: a.descripcion || '',
          reglas: a.reglas || '',
          ciudad: a.ciudad || '',
          direccion: a.direccion || '',
          tipo: a.tipo || '',
          capacidad: a.capacidad ?? '',
          precioNoche: a.precioNoche ?? '',
          tarifaLimpieza: a.tarifaLimpieza ?? '',
          modoReserva: a.modoReserva || 'INMEDIATA',
          politicaId: a.politica?.id ?? '',
          servicios: (a.servicios || []).map((s) => s.id),
        }),
      )
      .catch(setErrorCarga)
      .finally(() => setCargando(false));
  }, [editando, id]);

  const cambiar = (c) => (e) => setF({ ...f, [c]: e.target.value });
  const alternarServicio = (sid) => setF((x) => ({ ...x, servicios: x.servicios.includes(sid) ? x.servicios.filter((s) => s !== sid) : [...x.servicios, sid] }));

  const guardar = async (e) => {
    e.preventDefault();
    const v = validarAlojamiento(f);
    setErrores(v);
    setError(null);
    if (Object.keys(v).length) {
      toast.aviso('Revisa los campos marcados.');
      return;
    }
    setGuardando(true);
    try {
      const r = editando ? await alojamientosApi.editar(id, f) : await alojamientosApi.crear(f);
      toast.exito(editando ? 'Cambios guardados.' : 'Borrador creado. Ahora agrega fotos y disponibilidad.');
      navegar(rol === ROLES.ADMIN ? `/alojamientos/${r.id}` : `/anfitrion/alojamientos/${r.id}${editando ? '' : '?tab=fotos'}`);
    } catch (err) {
      if (err.errores) setErrores(err.errores);
      setError(err);
    } finally {
      setGuardando(false);
    }
  };

  if (cargando || cargandoCatalogos) return <PantallaCarga />;
  if (errorCarga) return <ErrorState error={errorCarga} />;

  return (
    <div className="max-w-3xl">
      <button type="button" onClick={() => navegar(-1)} className="mb-4 inline-flex items-center gap-1 text-sm text-ink-muted hover:text-ink">
        <ChevronLeft className="h-4 w-4" /> Volver
      </button>
      <h1 className="text-[32px] font-semibold tracking-tight">{editando ? 'Editar alojamiento' : 'Nuevo alojamiento'}</h1>
      <p className="mt-1 text-[15px] text-ink-muted">{editando ? 'Los cambios no alteran las reservas ya confirmadas.' : 'Se guardará como borrador hasta que lo envíes a revisión.'}</p>

      <form onSubmit={guardar} noValidate className="mt-8 space-y-6">
        <Card>
          <h2 className="mb-4 text-lg font-semibold">Lo básico</h2>
          <div className="space-y-4">
            <Input label="Título" maxLength={120} value={f.titulo} onChange={cambiar('titulo')} error={errores.titulo} ayuda="Entre 5 y 120 caracteres." />
            <Textarea label="Descripción" rows={5} value={f.descripcion} onChange={cambiar('descripcion')} error={errores.descripcion} ayuda="Mínimo 20 caracteres. Cuenta qué hace especial tu espacio." />
            <div className="grid gap-4 sm:grid-cols-2">
              <Select label="Tipo de alojamiento" value={f.tipo} onChange={cambiar('tipo')} error={errores.tipo}>
                <option value="">Selecciona…</option>
                {TIPOS_ALOJAMIENTO.map((t) => (
                  <option key={t} value={t}>
                    {TIPO_LABEL[t]}
                  </option>
                ))}
              </Select>
              <Input label="Capacidad (huéspedes)" type="number" min={1} max={50} value={f.capacidad} onChange={cambiar('capacidad')} error={errores.capacidad} />
            </div>
          </div>
        </Card>

        <Card>
          <h2 className="mb-4 text-lg font-semibold">Ubicación</h2>
          <div className="grid gap-4 sm:grid-cols-2">
            <Input label="Ciudad" maxLength={80} value={f.ciudad} onChange={cambiar('ciudad')} error={errores.ciudad} />
            <Input label="Dirección" maxLength={150} value={f.direccion} onChange={cambiar('direccion')} error={errores.direccion} />
          </div>
        </Card>

        <Card>
          <h2 className="mb-4 text-lg font-semibold">Precio y reservas</h2>
          <div className="grid gap-4 sm:grid-cols-2">
            <Input label="Precio por noche (COP)" type="number" min={1} step="1000" value={f.precioNoche} onChange={cambiar('precioNoche')} error={errores.precioNoche} />
            <Input label="Tarifa de limpieza (COP)" opcional type="number" min={0} step="1000" value={f.tarifaLimpieza} onChange={cambiar('tarifaLimpieza')} error={errores.tarifaLimpieza} />
          </div>
          <p className="label mt-5">Modalidad de reserva</p>
          <div className="grid gap-3 sm:grid-cols-2">
            {[
              { v: 'INMEDIATA', icono: Zap, t: 'Se confirma al aprobarse el pago, sin tu intervención.' },
              { v: 'APROBACION', icono: UserCheck, t: 'Revisas cada solicitud antes de que el huésped pague.' },
            ].map((o) => (
              <button
                key={o.v}
                type="button"
                onClick={() => setF({ ...f, modoReserva: o.v })}
                className={`flex gap-3 rounded-2xl border p-4 text-left transition ${f.modoReserva === o.v ? 'border-ink ring-1 ring-ink' : 'border-line hover:border-ink/40'}`}
                aria-pressed={f.modoReserva === o.v}
              >
                <o.icono className="mt-0.5 h-5 w-5 shrink-0" strokeWidth={1.7} />
                <span>
                  <span className="block text-[14.5px] font-semibold">{MODO_LABEL[o.v]}</span>
                  <span className="mt-0.5 block text-[13px] text-ink-muted">{o.t}</span>
                </span>
              </button>
            ))}
          </div>
          <Select className="mt-5" label="Política de cancelación" value={f.politicaId} onChange={cambiar('politicaId')} error={errores.politicaId}>
            <option value="">Selecciona…</option>
            {politicas.map((p) => (
              <option key={p.id} value={p.id}>
                {p.nombre} — {p.descripcion}
              </option>
            ))}
          </Select>
        </Card>

        <Card>
          <h2 className="mb-1 text-lg font-semibold">Servicios incluidos</h2>
          <p className="mb-4 text-sm text-ink-muted">Los servicios adicionales con costo se configuran después, en la gestión del alojamiento.</p>
          <div className="grid gap-2 sm:grid-cols-2">
            {servicios.map((s) => {
              const activo = f.servicios.includes(s.id);
              return (
                <button key={s.id} type="button" onClick={() => alternarServicio(s.id)} aria-pressed={activo} className={`flex items-center gap-3 rounded-2xl border px-4 py-3 text-left text-[14.5px] transition ${activo ? 'border-ink bg-canvas' : 'border-line hover:border-ink/30'}`}>
                  <span className={`flex h-5 w-5 items-center justify-center rounded-md border ${activo ? 'border-ink bg-ink text-white' : 'border-line'}`}>{activo && <Check className="h-3.5 w-3.5" strokeWidth={3} />}</span>
                  {s.nombre}
                </button>
              );
            })}
          </div>
        </Card>

        <Card>
          <Textarea label="Reglas de la casa" opcional rows={4} maxLength={2000} value={f.reglas} onChange={cambiar('reglas')} error={errores.reglas} placeholder="No se permiten fiestas. Hora de llegada desde las 3:00 p. m.…" />
        </Card>

        {error && <ErrorFormulario error={error} />}
        <div className="flex justify-end gap-3">
          <Button variante="ghost" onClick={() => navegar(-1)}>
            Cancelar
          </Button>
          <Button type="submit" tamano="lg" cargando={guardando}>
            {editando ? 'Guardar cambios' : 'Crear borrador'}
          </Button>
        </div>
      </form>
    </div>
  );
}
