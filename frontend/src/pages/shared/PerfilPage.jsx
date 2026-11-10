import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Mail, ShieldCheck, Power } from 'lucide-react';
import { usuariosApi } from '../../api';
import { useAuth } from '../../context/AuthContext';
import { useToast } from '../../context/ToastContext';
import { Button, Card, Confirmar, ErrorFormulario, EstadoBadge, Input, SectionTitle } from '../../components/ui';
import { ESTADO_USUARIO, ROL_LABEL, ROLES } from '../../utils/constants';
import { fechaLarga, iniciales } from '../../utils/format';
import { resolverImagen } from '../../utils/images';
import { validarPerfil } from '../../utils/validators';

/** Perfil (HU-05) y desactivación de la cuenta (HU-06). El correo no se modifica aquí. */
export default function PerfilPage() {
  const { usuario, setUsuario, rol, cerrarLocal } = useAuth();
  const toast = useToast();
  const navegar = useNavigate();
  const [f, setF] = useState({ nombre: '', telefono: '', fotoUrl: '', claveActual: '', nuevaClave: '', confirmar: '' });
  const [errores, setErrores] = useState({});
  const [error, setError] = useState(null);
  const [guardando, setGuardando] = useState(false);
  const [desactivar, setDesactivar] = useState(false);
  const [desactivando, setDesactivando] = useState(false);
  const [errorDesactivar, setErrorDesactivar] = useState(null);

  useEffect(() => {
    if (usuario) setF((x) => ({ ...x, nombre: usuario.nombre || '', telefono: usuario.telefono || '', fotoUrl: usuario.fotoUrl || '' }));
  }, [usuario]);

  const cambiar = (c) => (e) => setF({ ...f, [c]: e.target.value });

  const guardar = async (e) => {
    e.preventDefault();
    const v = validarPerfil(f);
    setErrores(v);
    setError(null);
    if (Object.keys(v).length) return;
    setGuardando(true);
    try {
      const actualizado = await usuariosApi.actualizar(f);
      setUsuario(actualizado);
      setF((x) => ({ ...x, claveActual: '', nuevaClave: '', confirmar: '' }));
      toast.exito(f.nuevaClave ? 'Perfil y clave actualizados.' : 'Perfil actualizado.');
    } catch (err) {
      if (err.errores) setErrores(err.errores);
      setError(err.status === 401 ? 'La clave actual no es correcta.' : err);
    } finally {
      setGuardando(false);
    }
  };

  const confirmarDesactivar = async () => {
    setDesactivando(true);
    setErrorDesactivar(null);
    try {
      await usuariosApi.desactivar();
      // El backend ya no acepta el token de una cuenta INACTIVA: se cierra la sesión local.
      cerrarLocal();
      toast.info('Tu cuenta fue desactivada. Puedes reactivarla iniciando sesión cuando quieras.');
      navegar('/', { replace: true });
    } catch (err) {
      setErrorDesactivar(err);
    } finally {
      setDesactivando(false);
    }
  };

  if (!usuario) return null;
  const foto = resolverImagen(f.fotoUrl);

  return (
    <div className="container-app max-w-4xl py-10">
      <SectionTitle titulo="Mi perfil" subtitulo="Mantén tus datos al día." />

      <div className="grid gap-6 lg:grid-cols-[280px_1fr]">
        <Card className="flex flex-col items-center text-center">
          {foto ? (
            <img src={foto} alt="" className="h-28 w-28 rounded-full object-cover shadow-soft" onError={(e) => (e.currentTarget.style.visibility = 'hidden')} />
          ) : (
            <span className="flex h-28 w-28 items-center justify-center rounded-full bg-ink text-3xl font-semibold text-white">{iniciales(usuario.nombre)}</span>
          )}
          <h2 className="mt-4 text-xl font-semibold">{usuario.nombre}</h2>
          <p className="mt-1 flex items-center gap-1.5 text-sm text-ink-muted">
            <Mail className="h-3.5 w-3.5" /> {usuario.correo}
          </p>
          <div className="mt-3 flex gap-2">
            <EstadoBadge estado={usuario.estado} mapa={ESTADO_USUARIO} />
            <span className="rounded-full bg-canvas px-2.5 py-0.5 text-xs font-medium">{ROL_LABEL[usuario.rol]}</span>
          </div>
          {usuario.creadoEn && <p className="mt-4 text-xs text-ink-faint">Miembro desde {fechaLarga(usuario.creadoEn)}</p>}
        </Card>

        <div className="space-y-6">
          <Card>
            <form onSubmit={guardar} noValidate className="space-y-4">
              <h3 className="text-lg font-semibold">Datos personales</h3>
              <Input label="Nombre" maxLength={120} value={f.nombre} onChange={cambiar('nombre')} error={errores.nombre} />
              <Input label="Correo" value={usuario.correo} disabled ayuda="El correo no se puede modificar." />
              <div className="grid gap-4 sm:grid-cols-2">
                <Input label="Teléfono" opcional maxLength={20} value={f.telefono} onChange={cambiar('telefono')} error={errores.telefono} placeholder="+57 300 000 0000" />
                <Input label="URL de la foto" opcional maxLength={500} value={f.fotoUrl} onChange={cambiar('fotoUrl')} error={errores.fotoUrl} placeholder="https://…" />
              </div>

              <div className="border-t border-line/60 pt-5">
                <h3 className="flex items-center gap-2 text-lg font-semibold">
                  <ShieldCheck className="h-5 w-5" /> Cambiar clave
                </h3>
                <p className="mb-4 mt-1 text-sm text-ink-muted">Déjalo vacío si no quieres cambiarla.</p>
                <div className="grid gap-4 sm:grid-cols-3">
                  <Input label="Clave actual" type="password" autoComplete="current-password" value={f.claveActual} onChange={cambiar('claveActual')} error={errores.claveActual} />
                  <Input label="Nueva clave" type="password" autoComplete="new-password" maxLength={64} value={f.nuevaClave} onChange={cambiar('nuevaClave')} error={errores.nuevaClave} />
                  <Input label="Confirmar" type="password" autoComplete="new-password" maxLength={64} value={f.confirmar} onChange={cambiar('confirmar')} error={errores.confirmar} />
                </div>
              </div>
              {error && <ErrorFormulario error={error} />}
              <div className="flex justify-end">
                <Button type="submit" cargando={guardando}>
                  Guardar cambios
                </Button>
              </div>
            </form>
          </Card>

          {rol !== ROLES.ADMIN && (
            <Card className="border-brand/15">
              <h3 className="flex items-center gap-2 text-lg font-semibold">
                <Power className="h-5 w-5 text-brand" /> Desactivar cuenta
              </h3>
              <p className="mt-1 text-sm text-ink-muted">
                Dejarás de usar la plataforma sin perder tu historial. {rol === ROLES.ANFITRION && 'Tus alojamientos no recibirán reservas mientras tanto. '}
                No es posible si tienes reservas confirmadas pendientes.
              </p>
              {errorDesactivar && (
                <div className="mt-4">
                  <ErrorFormulario error={errorDesactivar} />
                </div>
              )}
              <Button variante="danger" className="mt-5" onClick={() => setDesactivar(true)}>
                Desactivar mi cuenta
              </Button>
            </Card>
          )}
        </div>
      </div>

      <Confirmar
        abierto={desactivar}
        alCerrar={() => setDesactivar(false)}
        alConfirmar={async () => {
          await confirmarDesactivar();
          setDesactivar(false);
        }}
        cargando={desactivando}
        titulo="¿Desactivar tu cuenta?"
        mensaje="Se cerrará tu sesión. Podrás reactivarla en cualquier momento iniciando sesión con tu correo y clave."
        textoConfirmar="Desactivar"
        variante="brand"
      />
    </div>
  );
}
