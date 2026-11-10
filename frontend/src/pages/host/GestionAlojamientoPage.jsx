import { useCallback, useEffect, useRef, useState } from 'react';
import { Link, useParams, useSearchParams } from 'react-router-dom';
import { ChevronLeft, Pencil, Eye, ImagePlus, Link2, Trash2, CloudUpload, Plus, CalendarRange, Send, Pause, Play, Info, CalendarDays } from 'lucide-react';
import { alojamientosApi } from '../../api';
import { useToast } from '../../context/ToastContext';
import { Badge, Button, Card, Confirmar, ErrorFormulario, ErrorState, EstadoBadge, Input, Modal, PantallaCarga, SafeImage, Select } from '../../components/ui';
import AvailabilityCalendar from '../../components/listings/AvailabilityCalendar';
import { ACCIONES_ANFITRION, ESTADO_ALOJAMIENTO, ESTADO_PERIODO, MODO_LABEL, TIPO_LABEL } from '../../utils/constants';
import { dinero, fechaLarga, hoyIso, sumarDias } from '../../utils/format';
import { validarArchivoImagen, validarPeriodo, validarServicioAdicional, validarUrlImagen } from '../../utils/validators';

const PESTANAS = [
  { id: 'resumen', label: 'Resumen', icono: Info },
  { id: 'fotos', label: 'Fotos', icono: ImagePlus },
  { id: 'servicios', label: 'Servicios adicionales', icono: Plus },
  { id: 'disponibilidad', label: 'Disponibilidad', icono: CalendarRange },
];

const ICONO_ACCION = { ENVIAR_REVISION: Send, PAUSAR: Pause, ACTIVAR: Play };

/**
 * Gestión completa de una publicación del anfitrión:
 * estados (HU-09/10/11), fotos por URL o archivo (HU-07), servicios adicionales (HU-13) y periodos (HU-16/17).
 */
export default function GestionAlojamientoPage() {
  const { id } = useParams();
  const [sp, setSp] = useSearchParams();
  const pestana = sp.get('tab') || 'resumen';
  const toast = useToast();
  const [a, setA] = useState(null);
  const [error, setError] = useState(null);
  const [cambiando, setCambiando] = useState(false);
  const [errorEstado, setErrorEstado] = useState(null);

  const cargar = useCallback(async () => {
    try {
      setA(await alojamientosApi.detalle(id));
      setError(null);
    } catch (e) {
      setError(e);
    }
  }, [id]);

  useEffect(() => {
    cargar();
  }, [cargar]);

  const cambiarEstado = async (accion) => {
    setCambiando(true);
    setErrorEstado(null);
    try {
      const r = await alojamientosApi.cambiarEstado(id, accion);
      setA(r);
      toast.exito(accion === 'ENVIAR_REVISION' ? 'Enviado a revisión. El administrador lo revisará pronto.' : `Publicación ${ESTADO_ALOJAMIENTO[r.estado]?.label.toLowerCase()}.`);
    } catch (e) {
      // HU-11: el 422 indica qué campos faltan.
      setErrorEstado(e);
    } finally {
      setCambiando(false);
    }
  };

  if (error) return <ErrorState error={error} alReintentar={cargar} />;
  if (!a) return <PantallaCarga />;

  return (
    <div>
      <Link to="/anfitrion" className="mb-4 inline-flex items-center gap-1 text-sm text-ink-muted hover:text-ink">
        <ChevronLeft className="h-4 w-4" /> Mis alojamientos
      </Link>
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div className="min-w-0">
          <div className="flex items-center gap-2">
            <EstadoBadge estado={a.estado} mapa={ESTADO_ALOJAMIENTO} />
            <span className="text-xs text-ink-faint">#{a.id}</span>
          </div>
          <h1 className="mt-2 text-[28px] font-semibold tracking-tight">{a.titulo}</h1>
          <p className="text-[14.5px] text-ink-muted">
            {TIPO_LABEL[a.tipo]} · {a.ciudad} · {dinero(a.precioNoche)} / noche
          </p>
        </div>
        <div className="flex flex-wrap gap-2">
          <Button variante="outline" tamano="sm" icono={Eye} to={`/alojamientos/${a.id}`}>
            Vista previa
          </Button>
          {a.estado !== 'BLOQUEADA' && (
            <Button variante="outline" tamano="sm" icono={Pencil} to={`/anfitrion/alojamientos/${a.id}/editar`}>
              Editar datos
            </Button>
          )}
          <Button variante="outline" tamano="sm" icono={CalendarDays} to={`/anfitrion/reservas?alojamientoId=${a.id}`}>
            Reservas
          </Button>
          {(ACCIONES_ANFITRION[a.estado] || []).map((acc) => (
            <Button key={acc.accion} tamano="sm" icono={ICONO_ACCION[acc.accion]} cargando={cambiando} onClick={() => cambiarEstado(acc.accion)}>
              {acc.label}
            </Button>
          ))}
        </div>
      </div>
      {a.estado === 'PENDIENTE_REVISION' && <p className="mt-4 rounded-2xl bg-amber-50 px-4 py-3 text-sm text-amber-800">En revisión: el administrador debe aprobarla para que aparezca en la búsqueda.</p>}
      {a.estado === 'BLOQUEADA' && <p className="mt-4 rounded-2xl bg-brand-soft px-4 py-3 text-sm text-[#A1192C]">Un administrador bloqueó esta publicación. No aparece en búsquedas ni se puede editar.</p>}
      {errorEstado && (
        <div className="mt-4">
          <ErrorFormulario error={errorEstado} />
        </div>
      )}

      <div className="no-scrollbar mt-8 flex gap-1 overflow-x-auto border-b border-line/70">
        {PESTANAS.map((p) => (
          <button
            key={p.id}
            type="button"
            onClick={() => setSp({ tab: p.id }, { replace: true })}
            className={`-mb-px flex shrink-0 items-center gap-2 border-b-2 px-4 py-3 text-[14px] font-medium transition ${pestana === p.id ? 'border-ink text-ink' : 'border-transparent text-ink-muted hover:text-ink'}`}
          >
            <p.icono className="h-4 w-4" /> {p.label}
          </button>
        ))}
      </div>

      <div className="pt-8">
        {pestana === 'resumen' && <Resumen a={a} />}
        {pestana === 'fotos' && <Fotos a={a} alCambiar={cargar} />}
        {pestana === 'servicios' && <ServiciosAdicionales a={a} alCambiar={cargar} />}
        {pestana === 'disponibilidad' && <Disponibilidad a={a} />}
      </div>
    </div>
  );
}

function Resumen({ a }) {
  const filas = [
    ['Tipo', TIPO_LABEL[a.tipo]],
    ['Capacidad', `${a.capacidad} huéspedes`],
    ['Dirección', `${a.direccion}, ${a.ciudad}`],
    ['Precio por noche', dinero(a.precioNoche)],
    ['Tarifa de limpieza', dinero(a.tarifaLimpieza)],
    ['Modalidad', MODO_LABEL[a.modoReserva]],
    ['Política', a.politica ? `${a.politica.nombre} — ${a.politica.descripcion}` : 'Sin política (obligatoria para activar)'],
    ['Servicios', (a.servicios || []).map((s) => s.nombre).join(', ') || '—'],
    ['Fotos', `${a.imagenes?.length || 0} de 20`],
    ['Creado', fechaLarga(a.creadoEn)],
  ];
  return (
    <Card padding="p-0">
      <dl className="divide-y divide-line/60">
        {filas.map(([k, v]) => (
          <div key={k} className="grid gap-1 px-6 py-4 sm:grid-cols-[200px_1fr]">
            <dt className="text-sm text-ink-muted">{k}</dt>
            <dd className="text-[15px]">{v}</dd>
          </div>
        ))}
      </dl>
    </Card>
  );
}

/** Gestión multimedia: grilla de previsualización, drag & drop multipart y agregado por URL. */
function Fotos({ a, alCambiar }) {
  const toast = useToast();
  const entrada = useRef(null);
  const [arrastrando, setArrastrando] = useState(false);
  const [subidas, setSubidas] = useState([]);
  const [url, setUrl] = useState('');
  const [errorUrl, setErrorUrl] = useState(null);
  const [agregando, setAgregando] = useState(false);
  const [eliminar, setEliminar] = useState(null);
  const [eliminando, setEliminando] = useState(false);
  const imagenes = [...(a.imagenes || [])].sort((x, y) => (x.orden ?? 0) - (y.orden ?? 0));
  const espacio = Math.max(0, 20 - imagenes.length);

  const subir = async (archivos) => {
    const lista = Array.from(archivos || []).slice(0, espacio);
    if (archivos?.length > espacio) toast.aviso(`Una publicación admite máximo 20 imágenes. Se subirán ${espacio}.`);
    const validas = [];
    lista.forEach((archivo) => {
      const err = validarArchivoImagen(archivo);
      if (err) toast.error(err);
      else validas.push(archivo);
    });
    if (!validas.length) return;
    const tareas = validas.map((archivo, i) => ({ clave: `${archivo.name}-${Date.now()}-${i}`, nombre: archivo.name, vista: URL.createObjectURL(archivo), progreso: 0, archivo }));
    setSubidas((s) => [...s, ...tareas]);
    let ok = 0;
    for (const [i, t] of tareas.entries()) {
      try {
        await alojamientosApi.subirImagen(a.id, t.archivo, {
          orden: imagenes.length + i,
          onProgreso: (p) => setSubidas((s) => s.map((x) => (x.clave === t.clave ? { ...x, progreso: p } : x))),
        });
        ok += 1;
      } catch (e) {
        toast.error(`${t.nombre}: ${e.message}`);
      } finally {
        URL.revokeObjectURL(t.vista);
        setSubidas((s) => s.filter((x) => x.clave !== t.clave));
      }
    }
    if (ok) {
      toast.exito(`${ok} foto${ok > 1 ? 's' : ''} subida${ok > 1 ? 's' : ''}.`);
      alCambiar();
    }
  };

  const agregarUrl = async (e) => {
    e.preventDefault();
    const err = validarUrlImagen(url);
    setErrorUrl(err);
    if (err) return;
    setAgregando(true);
    try {
      await alojamientosApi.agregarImagenUrl(a.id, { url, orden: imagenes.length });
      setUrl('');
      toast.exito('Imagen agregada.');
      alCambiar();
    } catch (er) {
      setErrorUrl(er.errores?.url || er.message);
    } finally {
      setAgregando(false);
    }
  };

  const confirmarEliminar = async () => {
    setEliminando(true);
    try {
      await alojamientosApi.eliminarImagen(a.id, eliminar.id);
      toast.exito('Imagen eliminada.');
      setEliminar(null);
      alCambiar();
    } catch (e) {
      toast.error(e);
    } finally {
      setEliminando(false);
    }
  };

  return (
    <div className="space-y-6">
      <div
        onDragOver={(e) => {
          e.preventDefault();
          setArrastrando(true);
        }}
        onDragLeave={() => setArrastrando(false)}
        onDrop={(e) => {
          e.preventDefault();
          setArrastrando(false);
          subir(e.dataTransfer.files);
        }}
        className={`flex flex-col items-center justify-center rounded-3xl border-2 border-dashed px-6 py-12 text-center transition ${arrastrando ? 'border-accent bg-accent-soft' : 'border-line bg-canvas/60'}`}
      >
        <CloudUpload className={`h-10 w-10 ${arrastrando ? 'text-accent' : 'text-ink-faint'}`} strokeWidth={1.4} />
        <p className="mt-3 text-[16px] font-semibold">Arrastra tus fotos aquí</p>
        <p className="mt-1 text-sm text-ink-muted">JPG, PNG, WEBP o GIF · máximo 5 MB cada una · {espacio} espacios libres</p>
        <Button variante="outline" tamano="sm" className="mt-5" onClick={() => entrada.current?.click()} disabled={espacio === 0}>
          Seleccionar archivos
        </Button>
        <input
          ref={entrada}
          type="file"
          accept="image/jpeg,image/png,image/webp,image/gif"
          multiple
          className="hidden"
          onChange={(e) => {
            subir(e.target.files);
            e.target.value = '';
          }}
        />
      </div>

      <form onSubmit={agregarUrl} className="flex flex-col gap-3 sm:flex-row sm:items-start">
        <Input className="flex-1" placeholder="https://… (URL externa de la imagen)" value={url} onChange={(e) => setUrl(e.target.value)} error={errorUrl} maxLength={255} aria-label="URL de la imagen" />
        <Button type="submit" variante="secondary" icono={Link2} cargando={agregando} disabled={espacio === 0}>
          Agregar por URL
        </Button>
      </form>

      {imagenes.length === 0 && subidas.length === 0 ? (
        <p className="rounded-2xl bg-canvas px-4 py-6 text-center text-sm text-ink-muted">Aún no hay fotos. Las publicaciones con buenas fotos reciben más reservas.</p>
      ) : (
        <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-4">
          {imagenes.map((img, i) => (
            <div key={img.id} className="group relative overflow-hidden rounded-2xl">
              <SafeImage src={img.url} semilla={a.id + i} className="aspect-[4/3] w-full" imgClassName="transition duration-500 group-hover:scale-105" />
              {i === 0 && <Badge tono="dark" className="absolute left-2 top-2">Portada</Badge>}
              <button
                type="button"
                onClick={() => setEliminar(img)}
                className="glass absolute right-2 top-2 flex h-8 w-8 items-center justify-center rounded-full text-brand opacity-100 shadow-pill transition sm:opacity-0 sm:group-hover:opacity-100"
                aria-label="Eliminar imagen"
              >
                <Trash2 className="h-4 w-4" />
              </button>
            </div>
          ))}
          {subidas.map((s) => (
            <div key={s.clave} className="relative overflow-hidden rounded-2xl">
              <img src={s.vista} alt="" className="aspect-[4/3] w-full object-cover opacity-60" />
              <div className="absolute inset-x-3 bottom-3 h-1.5 overflow-hidden rounded-full bg-white/60">
                <div className="h-full rounded-full bg-ink transition-all" style={{ width: `${s.progreso}%` }} />
              </div>
            </div>
          ))}
        </div>
      )}

      <Confirmar abierto={Boolean(eliminar)} alCerrar={() => setEliminar(null)} alConfirmar={confirmarEliminar} cargando={eliminando} titulo="Eliminar foto" mensaje="La foto se eliminará de la publicación." textoConfirmar="Eliminar" variante="brand" />
    </div>
  );
}

function ServiciosAdicionales({ a, alCambiar }) {
  const toast = useToast();
  const [f, setF] = useState({ nombre: '', precio: '' });
  const [errores, setErrores] = useState({});
  const [guardando, setGuardando] = useState(false);
  const [borrando, setBorrando] = useState(null);

  const agregar = async (e) => {
    e.preventDefault();
    const v = validarServicioAdicional(f);
    setErrores(v);
    if (Object.keys(v).length) return;
    setGuardando(true);
    try {
      await alojamientosApi.agregarServicioAdicional(a.id, f);
      setF({ nombre: '', precio: '' });
      toast.exito('Servicio adicional agregado.');
      alCambiar();
    } catch (er) {
      setErrores(er.errores || { nombre: er.message });
    } finally {
      setGuardando(false);
    }
  };

  const eliminar = async (s) => {
    setBorrando(s.id);
    try {
      await alojamientosApi.eliminarServicioAdicional(a.id, s.id);
      toast.exito(`«${s.nombre}» eliminado.`);
      alCambiar();
    } catch (e) {
      toast.error(e);
    } finally {
      setBorrando(null);
    }
  };

  const lista = a.serviciosAdicionales || [];
  return (
    <div className="grid gap-6 lg:grid-cols-[1fr_340px]">
      <Card padding="p-0">
        {lista.length === 0 ? (
          <p className="px-6 py-10 text-center text-sm text-ink-muted">Sin servicios adicionales. Ofrece desayuno, traslados o parqueadero con su costo.</p>
        ) : (
          <ul className="divide-y divide-line/60">
            {lista.map((s) => (
              <li key={s.id} className="flex items-center justify-between gap-4 px-6 py-4">
                <span className="text-[15px] font-medium">{s.nombre}</span>
                <span className="flex items-center gap-4">
                  <span className="text-[15px]">{dinero(s.precio)}</span>
                  <Button variante="ghost" tamano="icon" onClick={() => eliminar(s)} cargando={borrando === s.id} aria-label={`Eliminar ${s.nombre}`}>
                    <Trash2 className="h-4 w-4 text-brand" />
                  </Button>
                </span>
              </li>
            ))}
          </ul>
        )}
      </Card>
      <Card>
        <form onSubmit={agregar} noValidate className="space-y-4">
          <h3 className="text-[16px] font-semibold">Nuevo servicio</h3>
          <Input label="Nombre" maxLength={80} value={f.nombre} onChange={(e) => setF({ ...f, nombre: e.target.value })} error={errores.nombre} placeholder="Desayuno" />
          <Input label="Precio (COP)" type="number" min={0} step="1000" value={f.precio} onChange={(e) => setF({ ...f, precio: e.target.value })} error={errores.precio} />
          <Button type="submit" completo icono={Plus} cargando={guardando}>
            Agregar
          </Button>
        </form>
      </Card>
    </div>
  );
}

/** HU-16 (CRUD de periodos) y HU-17 (vista de calendario). Los periodos RESERVADOS los crea el sistema (HU-18). */
function Disponibilidad({ a }) {
  const toast = useToast();
  const [periodos, setPeriodos] = useState([]);
  const [cargando, setCargando] = useState(true);
  const [editar, setEditar] = useState(null);
  const [eliminar, setEliminar] = useState(null);
  const [eliminando, setEliminando] = useState(false);

  const cargar = useCallback(async () => {
    setCargando(true);
    try {
      setPeriodos((await alojamientosApi.calendario(a.id, { desde: sumarDias(hoyIso(), -30), hasta: sumarDias(hoyIso(), 540) })) || []);
    } catch (e) {
      toast.error(e);
    } finally {
      setCargando(false);
    }
  }, [a.id, toast]);

  useEffect(() => {
    cargar();
  }, [cargar]);

  const confirmarEliminar = async () => {
    setEliminando(true);
    try {
      await alojamientosApi.eliminarPeriodo(a.id, eliminar.id);
      toast.exito('Periodo eliminado.');
      setEliminar(null);
      cargar();
    } catch (e) {
      toast.error(e);
    } finally {
      setEliminando(false);
    }
  };

  return (
    <div className="space-y-6">
      <Card>
        <AvailabilityCalendar periodos={periodos} />
      </Card>
      <div className="flex items-center justify-between">
        <h3 className="text-lg font-semibold">Periodos</h3>
        <Button tamano="sm" icono={Plus} onClick={() => setEditar({})}>
          Nuevo periodo
        </Button>
      </div>
      <Card padding="p-0" className="overflow-x-auto">
        {cargando ? (
          <p className="px-6 py-8 text-center text-sm text-ink-muted">Cargando periodos…</p>
        ) : periodos.length === 0 ? (
          <p className="px-6 py-8 text-center text-sm text-ink-muted">Sin periodos. Agrega al menos uno DISPONIBLE para recibir reservas.</p>
        ) : (
          <table className="table-app">
            <thead>
              <tr>
                <th>Desde</th>
                <th>Hasta</th>
                <th>Estado</th>
                <th className="text-right">Acciones</th>
              </tr>
            </thead>
            <tbody>
              {periodos.map((p) => (
                <tr key={p.id}>
                  <td>{fechaLarga(p.fechaInicio)}</td>
                  <td>{fechaLarga(p.fechaFin)}</td>
                  <td>
                    <EstadoBadge estado={p.estado} mapa={ESTADO_PERIODO} />
                    {p.reservaId && <span className="ml-2 text-xs text-ink-faint">Reserva #{p.reservaId}</span>}
                  </td>
                  <td className="text-right">
                    {p.estado !== 'RESERVADO' ? (
                      <div className="flex justify-end gap-1">
                        <Button variante="ghost" tamano="sm" onClick={() => setEditar(p)}>
                          Editar
                        </Button>
                        <Button variante="ghost" tamano="sm" className="text-brand" onClick={() => setEliminar(p)}>
                          Eliminar
                        </Button>
                      </div>
                    ) : (
                      <span className="text-xs text-ink-faint">Automático</span>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </Card>

      <PeriodoModal alojamientoId={a.id} periodo={editar} alCerrar={() => setEditar(null)} alGuardar={cargar} />
      <Confirmar
        abierto={Boolean(eliminar)}
        alCerrar={() => setEliminar(null)}
        alConfirmar={confirmarEliminar}
        cargando={eliminando}
        titulo="Eliminar periodo"
        mensaje="No se puede eliminar un periodo que ya tiene reservas confirmadas."
        textoConfirmar="Eliminar"
        variante="brand"
      />
    </div>
  );
}

function PeriodoModal({ alojamientoId, periodo, alCerrar, alGuardar }) {
  const toast = useToast();
  const [f, setF] = useState({ fechaInicio: '', fechaFin: '', estado: 'DISPONIBLE' });
  const [errores, setErrores] = useState({});
  const [error, setError] = useState(null);
  const [guardando, setGuardando] = useState(false);
  const editando = Boolean(periodo?.id);

  useEffect(() => {
    if (periodo) {
      setF({ fechaInicio: periodo.fechaInicio || hoyIso(), fechaFin: periodo.fechaFin || sumarDias(hoyIso(), 30), estado: periodo.estado || 'DISPONIBLE' });
      setErrores({});
      setError(null);
    }
  }, [periodo]);

  const guardar = async () => {
    const v = validarPeriodo(f);
    setErrores(v);
    if (Object.keys(v).length) return;
    setGuardando(true);
    setError(null);
    try {
      if (editando) await alojamientosApi.editarPeriodo(alojamientoId, periodo.id, f);
      else await alojamientosApi.crearPeriodo(alojamientoId, f);
      toast.exito(editando ? 'Periodo actualizado.' : 'Periodo creado.');
      alGuardar();
      alCerrar();
    } catch (e) {
      setError(e);
    } finally {
      setGuardando(false);
    }
  };

  return (
    <Modal
      abierto={Boolean(periodo)}
      alCerrar={alCerrar}
      titulo={editando ? 'Editar periodo' : 'Nuevo periodo'}
      ancho="max-w-md"
      pie={
        <>
          <Button variante="ghost" onClick={alCerrar}>
            Cancelar
          </Button>
          <Button onClick={guardar} cargando={guardando}>
            Guardar
          </Button>
        </>
      }
    >
      <div className="space-y-4">
        <div className="grid grid-cols-2 gap-3">
          <Input label="Desde" type="date" value={f.fechaInicio} onChange={(e) => setF({ ...f, fechaInicio: e.target.value })} error={errores.fechaInicio} />
          <Input label="Hasta" type="date" min={f.fechaInicio} value={f.fechaFin} onChange={(e) => setF({ ...f, fechaFin: e.target.value })} error={errores.fechaFin} />
        </div>
        <Select label="Estado" value={f.estado} onChange={(e) => setF({ ...f, estado: e.target.value })} error={errores.estado}>
          <option value="DISPONIBLE">Disponible para reservas</option>
          <option value="BLOQUEADO">Bloqueado (no disponible)</option>
        </Select>
        {error && <ErrorFormulario error={error} />}
      </div>
    </Modal>
  );
}
