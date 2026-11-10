import { useCallback, useEffect, useRef, useState } from 'react';
import { Link, useLocation, useNavigate, useSearchParams } from 'react-router-dom';
import { Luggage, KeyRound, ShieldAlert, MailCheck, House, RotateCcw, RefreshCw } from 'lucide-react';
import { authApi } from '../../api';
import { useAuth, ESTADO_CUENTA_DESACTIVADA, ESTADO_CORREO_NO_VERIFICADO } from '../../context/AuthContext';
import { useToast } from '../../context/ToastContext';
import { Button, Input, ErrorFormulario, Spinner } from '../../components/ui';
import { HOME_POR_ROL, ROLES } from '../../utils/constants';
import { validarCorreo, validarLogin, validarRegistro, validarRestablecer } from '../../utils/validators';
import { IMAGENES_RESPALDO } from '../../utils/images';

/** Contenedor visual de las pantallas de autenticación (imagen + panel glass). */
function AuthShell({ titulo, subtitulo, children, pie }) {
  return (
    <div className="grid min-h-[calc(100vh-64px)] bg-canvas lg:grid-cols-2">
      <div className="hidden p-5 lg:block">
        <div className="sticky top-[84px] h-[calc(100vh-104px)] min-h-[520px] overflow-hidden rounded-[36px] shadow-lift">
          <img src={IMAGENES_RESPALDO[1]} alt="" className="absolute inset-0 h-full w-full object-cover" />
          <div className="absolute inset-0 bg-gradient-to-t from-black/60 via-black/10 to-transparent" />
          <div className="absolute bottom-10 left-10 right-10 text-white">
            <p className="text-[34px] font-semibold leading-tight tracking-tight">Cada estancia, a tu manera.</p>
            <p className="mt-2 max-w-md text-white/80">Reserva con precios claros o comparte tu espacio con huéspedes verificados.</p>
          </div>
        </div>
      </div>
      <div className="flex items-center justify-center px-5 py-12">
        <div className="w-full max-w-md animate-rise-in rounded-[28px] border border-white bg-white/90 p-8 shadow-lift backdrop-blur sm:p-10">
          <h1 className="text-[28px] font-semibold tracking-tight">{titulo}</h1>
          {subtitulo && <p className="mt-1.5 text-[15px] text-ink-muted">{subtitulo}</p>}
          <div className="mt-8">{children}</div>
          {pie && <div className="mt-8 border-t border-line/60 pt-6 text-center text-sm text-ink-muted">{pie}</div>}
        </div>
      </div>
    </div>
  );
}

/**
 * Captcha del registro (GET /auth/captcha). La respuesta correcta solo la conoce el servidor;
 * cada captcha es de un solo uso, así que tras un intento fallido se pide uno nuevo.
 */
function Captcha({ valor, alCambiar, error, version }) {
  const [captcha, setCaptcha] = useState(null);
  const [cargando, setCargando] = useState(true);
  const [fallo, setFallo] = useState(null);

  const cargar = useCallback(async () => {
    setCargando(true);
    setFallo(null);
    try {
      const c = await authApi.captcha();
      setCaptcha(c);
      alCambiar({ captchaId: c.captchaId, captchaRespuesta: '' });
    } catch (e) {
      setFallo(e.message);
    } finally {
      setCargando(false);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    cargar();
  }, [cargar, version]);

  if (captcha && captcha.habilitado === false) return null;

  return (
    <div>
      <p className="label">Verificación de seguridad</p>
      <div className="flex items-center gap-3">
        <div className="flex h-[60px] w-[180px] shrink-0 items-center justify-center overflow-hidden rounded-xl border border-line bg-canvas">
          {cargando ? <Spinner className="h-5 w-5 text-ink-muted" /> : captcha ? <img src={captcha.imagen} alt="Texto del captcha" className="h-full w-full select-none" draggable={false} /> : <span className="px-2 text-center text-xs text-brand">{fallo}</span>}
        </div>
        <button type="button" onClick={cargar} className="flex h-10 w-10 items-center justify-center rounded-full border border-line text-ink-soft transition hover:bg-canvas hover:text-ink" aria-label="Generar otro captcha" title="Generar otro">
          <RefreshCw className={`h-4 w-4 ${cargando ? 'animate-spin' : ''}`} />
        </button>
      </div>
      <Input
        className="mt-3"
        placeholder="Escribe los 5 caracteres de la imagen"
        autoComplete="off"
        maxLength={5}
        value={valor}
        onChange={(e) => alCambiar({ captchaRespuesta: e.target.value.toUpperCase() })}
        error={error}
        aria-label="Respuesta del captcha"
      />
    </div>
  );
}

/** Seis casillas para el código de verificación (pega el código completo de una vez). */
function CodigoSeisDigitos({ valor, alCambiar, deshabilitado }) {
  const refs = useRef([]);
  const digitos = Array.from({ length: 6 }, (_, i) => valor[i] || '');

  const escribir = (i, texto) => {
    const limpio = texto.replace(/\D/g, '');
    if (!limpio) return;
    const nuevo = (valor.slice(0, i) + limpio + valor.slice(i + limpio.length)).slice(0, 6);
    alCambiar(nuevo);
    refs.current[Math.min(i + limpio.length, 5)]?.focus();
  };

  const tecla = (i, e) => {
    if (e.key === 'Backspace') {
      e.preventDefault();
      const nuevo = digitos.map((d, j) => (j === i ? '' : d)).join('');
      alCambiar(digitos[i] ? nuevo : valor.slice(0, Math.max(0, i - 1)) + valor.slice(i));
      if (!digitos[i]) refs.current[Math.max(0, i - 1)]?.focus();
    }
    if (e.key === 'ArrowLeft') refs.current[Math.max(0, i - 1)]?.focus();
    if (e.key === 'ArrowRight') refs.current[Math.min(5, i + 1)]?.focus();
  };

  return (
    <div className="flex justify-between gap-2" role="group" aria-label="Código de verificación">
      {digitos.map((d, i) => (
        <input
          key={i}
          ref={(el) => (refs.current[i] = el)}
          inputMode="numeric"
          autoComplete={i === 0 ? 'one-time-code' : 'off'}
          maxLength={6}
          value={d}
          disabled={deshabilitado}
          onChange={(e) => escribir(i, e.target.value)}
          onKeyDown={(e) => tecla(i, e)}
          onFocus={(e) => e.target.select()}
          aria-label={`Dígito ${i + 1}`}
          className="h-14 w-full min-w-0 rounded-2xl border border-line bg-white text-center text-[22px] font-semibold outline-none transition focus:border-accent focus:ring-4 focus:ring-accent/15"
        />
      ))}
    </div>
  );
}

/**
 * Confirmación del correo con el código de 6 dígitos (POST /auth/verificar-correo).
 * Se llega aquí después del registro o al intentar iniciar sesión sin haber confirmado.
 */
export function VerificarCorreoPage() {
  const { verificarCorreo } = useAuth();
  const toast = useToast();
  const navegar = useNavigate();
  const ubicacion = useLocation();
  const [sp] = useSearchParams();
  const correo = sp.get('correo') || ubicacion.state?.correo || '';
  const [codigo, setCodigo] = useState('');
  const [simulado, setSimulado] = useState(ubicacion.state?.codigoSimulado || null);
  const [error, setError] = useState(null);
  const [enviando, setEnviando] = useState(false);
  const [espera, setEspera] = useState(ubicacion.state?.recienEnviado ? 60 : 0);

  useEffect(() => {
    if (espera <= 0) return undefined;
    const t = setTimeout(() => setEspera((x) => x - 1), 1000);
    return () => clearTimeout(t);
  }, [espera]);

  const confirmar = async (e) => {
    e?.preventDefault();
    if (codigo.length !== 6) {
      setError('Ingresa los 6 dígitos del código.');
      return;
    }
    setEnviando(true);
    setError(null);
    try {
      const perfil = await verificarCorreo({ correo, codigo });
      toast.exito('¡Correo confirmado! Bienvenido.');
      navegar(HOME_POR_ROL[perfil.rol] || '/', { replace: true });
    } catch (er) {
      setError(er);
      setCodigo('');
    } finally {
      setEnviando(false);
    }
  };

  const reenviar = async () => {
    setError(null);
    try {
      const r = await authApi.reenviarVerificacion(correo);
      setSimulado(r.codigoVerificacionSimulado || null);
      toast.info('Te enviamos un código nuevo.');
      setEspera(60);
    } catch (er) {
      setError(er);
    }
  };

  if (!correo) {
    return (
      <AuthShell titulo="Confirma tu correo" subtitulo="No sabemos qué correo confirmar.">
        <Button completo to="/login">
          Ir a iniciar sesión
        </Button>
      </AuthShell>
    );
  }

  return (
    <AuthShell
      titulo="Confirma tu correo"
      subtitulo={ubicacion.state?.mensaje || `Escribe el código de 6 dígitos que enviamos a ${correo}.`}
      pie={
        <>
          ¿Correo equivocado?{' '}
          <Link to="/registro" className="font-medium text-accent hover:underline">
            Regístrate de nuevo
          </Link>
        </>
      }
    >
      <div className="mb-6 flex items-center gap-3 rounded-2xl bg-canvas p-4">
        <span className="flex h-11 w-11 shrink-0 items-center justify-center rounded-full bg-white shadow-soft">
          <MailCheck className="h-5 w-5 text-accent" />
        </span>
        <p className="min-w-0 text-sm text-ink-soft">
          Revisa tu bandeja de entrada (y la carpeta de spam). El código vence en 15 minutos.
          <strong className="mt-0.5 block truncate text-ink">{correo}</strong>
        </p>
      </div>
      {simulado && (
        <div className="mb-5 rounded-2xl border border-amber-200 bg-amber-50 p-4 text-sm text-amber-900">
          <p className="font-semibold">Modo de correo simulado</p>
          <p className="mt-1">El backend no envía correos reales en este modo. Tu código es:</p>
          <button type="button" onClick={() => setCodigo(simulado)} className="mt-2 rounded-xl bg-white px-3 py-1.5 font-mono text-lg font-semibold tracking-[0.3em] text-ink shadow-soft">
            {simulado}
          </button>
        </div>
      )}
      <form onSubmit={confirmar} className="space-y-5">
        <CodigoSeisDigitos valor={codigo} alCambiar={setCodigo} deshabilitado={enviando} />
        {error && <ErrorFormulario error={error} />}
        <Button type="submit" completo tamano="lg" cargando={enviando} disabled={codigo.length !== 6}>
          Confirmar y entrar
        </Button>
      </form>
      <p className="mt-5 text-center text-sm text-ink-muted">
        ¿No te llegó?{' '}
        <button type="button" onClick={reenviar} disabled={espera > 0} className="font-medium text-accent hover:underline disabled:text-ink-faint disabled:no-underline">
          {espera > 0 ? `Reenviar en ${espera} s` : 'Reenviar código'}
        </button>
      </p>
    </AuthShell>
  );
}

/**
 * Inicio de sesión (HU-02). Si la cuenta está desactivada (423 del backend) se muestra, sin salir de la
 * pantalla, la interfaz para reactivarla con las mismas credenciales (HU-06, POST /auth/reactivar).
 */
export function LoginPage() {
  const { login, reactivar } = useAuth();
  const toast = useToast();
  const navegar = useNavigate();
  const ubicacion = useLocation();
  const [f, setF] = useState({ correo: '', clave: '' });
  const [errores, setErrores] = useState({});
  const [error, setError] = useState(null);
  const [enviando, setEnviando] = useState(false);
  const [desactivada, setDesactivada] = useState(false);

  const destino = (perfil) => ubicacion.state?.desde || HOME_POR_ROL[perfil?.rol] || '/';

  const enviar = async (e) => {
    e.preventDefault();
    const v = validarLogin(f);
    setErrores(v);
    setError(null);
    if (Object.keys(v).length) return;
    setEnviando(true);
    try {
      const perfil = await login(f);
      toast.exito(`Hola de nuevo, ${perfil.nombre.split(' ')[0]}.`);
      navegar(destino(perfil), { replace: true });
    } catch (err) {
      if (err.codigo === ESTADO_CUENTA_DESACTIVADA) setDesactivada(true);
      else if (err.codigo === ESTADO_CORREO_NO_VERIFICADO) {
        toast.aviso('Primero confirma tu correo con el código que te enviamos.');
        navegar(`/verificar-correo?correo=${encodeURIComponent(err.datos?.correo || f.correo.trim())}`, {
          state: { mensaje: err.message, desde: ubicacion.state?.desde },
        });
      } else setError(err);
    } finally {
      setEnviando(false);
    }
  };

  const confirmarReactivacion = async () => {
    setEnviando(true);
    setError(null);
    try {
      const perfil = await reactivar(f);
      toast.exito('Tu cuenta fue reactivada. ¡Bienvenido de vuelta!');
      navegar(destino(perfil), { replace: true });
    } catch (err) {
      setError(err);
    } finally {
      setEnviando(false);
    }
  };

  if (desactivada) {
    return (
      <AuthShell titulo="Tu cuenta está desactivada" subtitulo="La desactivaste antes. Tu historial sigue intacto y puedes volver cuando quieras.">
        <div className="mb-6 flex items-center gap-4 rounded-2xl bg-canvas p-4">
          <span className="flex h-11 w-11 shrink-0 items-center justify-center rounded-full bg-white shadow-soft">
            <RotateCcw className="h-5 w-5" />
          </span>
          <p className="text-sm text-ink-soft">
            Reactivaremos <strong className="text-ink">{f.correo}</strong> con la clave que acabas de ingresar.
          </p>
        </div>
        {error && (
          <div className="mb-4">
            <ErrorFormulario error={error} />
          </div>
        )}
        <Button completo tamano="lg" onClick={confirmarReactivacion} cargando={enviando}>
          Reactivar mi cuenta
        </Button>
        <Button
          completo
          variante="ghost"
          className="mt-2"
          onClick={() => {
            setDesactivada(false);
            setF((x) => ({ ...x, clave: '' }));
          }}
          disabled={enviando}
        >
          Usar otra cuenta
        </Button>
      </AuthShell>
    );
  }

  return (
    <AuthShell
      titulo="Iniciar sesión"
      subtitulo="Accede con tu correo y tu clave."
      pie={
        <>
          ¿No tienes cuenta?{' '}
          <Link to="/registro" className="font-medium text-accent hover:underline">
            Crea una
          </Link>
        </>
      }
    >
      <form onSubmit={enviar} noValidate className="space-y-4">
        <Input label="Correo" type="email" autoComplete="email" value={f.correo} onChange={(e) => setF({ ...f, correo: e.target.value })} error={errores.correo} autoFocus />
        <Input label="Clave" type="password" autoComplete="current-password" value={f.clave} onChange={(e) => setF({ ...f, clave: e.target.value })} error={errores.clave} />
        <div className="text-right">
          <Link to="/recuperar" className="text-[13px] font-medium text-accent hover:underline">
            ¿Olvidaste tu clave?
          </Link>
        </div>
        {error && (
          <div className="flex gap-3">
            <div className="flex-1">
              <ErrorFormulario error={error} />
            </div>
          </div>
        )}
        {error?.status === 423 && <p className="flex items-center gap-2 text-xs text-ink-muted"><ShieldAlert className="h-3.5 w-3.5" /> Por seguridad, tras 5 intentos fallidos la cuenta se bloquea 15 minutos.</p>}
        <Button type="submit" completo tamano="lg" cargando={enviando}>
          Continuar
        </Button>
      </form>
    </AuthShell>
  );
}

/** Registro con elección elegante de rol: Huésped o Anfitrión (HU-01). El rol ADMINISTRADOR no es elegible. */
export function RegisterPage() {
  const { registrar, login } = useAuth();
  const toast = useToast();
  const navegar = useNavigate();
  const [sp] = useSearchParams();
  const [f, setF] = useState({
    nombre: '',
    correo: '',
    clave: '',
    confirmar: '',
    rol: sp.get('rol') === ROLES.ANFITRION ? ROLES.ANFITRION : ROLES.HUESPED,
    captchaId: '',
    captchaRespuesta: '',
  });
  const [versionCaptcha, setVersionCaptcha] = useState(0);
  const [aceptaTerminos, setAceptaTerminos] = useState(false);
  const [errores, setErrores] = useState({});
  const [error, setError] = useState(null);
  const [enviando, setEnviando] = useState(false);

  const cambiar = (campo) => (e) => setF({ ...f, [campo]: e.target.value });

  const enviar = async (e) => {
    e.preventDefault();
    const v = validarRegistro(f);
    setErrores(v);
    setError(null);
    if (f.captchaId && !f.captchaRespuesta.trim()) v.captcha = 'Escribe el texto de la imagen.';
    if (!aceptaTerminos) v.terminos = 'Debes aceptar los términos y la política de privacidad.';
    setErrores(v);
    if (Object.keys(v).length) return;
    setEnviando(true);
    try {
      const r = await registrar(f);
      if (r?.verificacionRequerida) {
        toast.exito('Cuenta creada. Ahora confirma tu correo.');
        navegar(`/verificar-correo?correo=${encodeURIComponent(f.correo.trim())}`, {
          replace: true,
          state: { mensaje: r.mensaje, codigoSimulado: r.codigoVerificacionSimulado, recienEnviado: true },
        });
        return;
      }
      const perfil = await login({ correo: f.correo, clave: f.clave });
      toast.exito('Tu cuenta fue creada. ¡Bienvenido!');
      navegar(HOME_POR_ROL[perfil.rol] || '/', { replace: true });
    } catch (err) {
      // Errores de validación del servidor (400) se muestran en cada campo.
      if (err.errores && Object.keys(err.errores).length) setErrores(err.errores);
      if (/captcha/i.test(err.message)) setErrores((x) => ({ ...x, captcha: err.message }));
      else setError(err);
      // El captcha es de un solo uso: tras cualquier intento se genera uno nuevo.
      setVersionCaptcha((x) => x + 1);
    } finally {
      setEnviando(false);
    }
  };

  const opciones = [
    { rol: ROLES.HUESPED, icono: Luggage, titulo: 'Quiero viajar', texto: 'Busca, compara y reserva alojamientos.' },
    { rol: ROLES.ANFITRION, icono: House, titulo: 'Quiero hospedar', texto: 'Publica tu espacio y recibe reservas.' },
  ];

  return (
    <AuthShell
      titulo="Crea tu cuenta"
      subtitulo="Elige cómo quieres usar la plataforma."
      pie={
        <>
          ¿Ya tienes cuenta?{' '}
          <Link to="/login" className="font-medium text-accent hover:underline">
            Inicia sesión
          </Link>
        </>
      }
    >
      <form onSubmit={enviar} noValidate className="space-y-4">
        <div className="grid grid-cols-2 gap-3" role="radiogroup" aria-label="Tipo de cuenta">
          {opciones.map((o) => {
            const activo = f.rol === o.rol;
            return (
              <button
                key={o.rol}
                type="button"
                role="radio"
                aria-checked={activo}
                onClick={() => setF({ ...f, rol: o.rol })}
                className={`flex flex-col items-start rounded-2xl border p-4 text-left transition ${activo ? 'border-ink bg-ink text-white shadow-soft' : 'border-line hover:border-ink/40'}`}
              >
                <o.icono className="h-5 w-5" strokeWidth={1.7} />
                <span className="mt-3 text-[14.5px] font-semibold">{o.titulo}</span>
                <span className={`mt-0.5 text-[12.5px] ${activo ? 'text-white/75' : 'text-ink-muted'}`}>{o.texto}</span>
              </button>
            );
          })}
        </div>
        {errores.rol && <p className="text-[13px] text-brand">{errores.rol}</p>}
        <Input label="Nombre completo" autoComplete="name" maxLength={120} value={f.nombre} onChange={cambiar('nombre')} error={errores.nombre} />
        <Input label="Correo" type="email" autoComplete="email" maxLength={150} value={f.correo} onChange={cambiar('correo')} error={errores.correo} />
        <Input label="Clave" type="password" autoComplete="new-password" maxLength={64} value={f.clave} onChange={cambiar('clave')} error={errores.clave} ayuda="Entre 8 y 64 caracteres." />
        <Input label="Confirmar clave" type="password" autoComplete="new-password" maxLength={64} value={f.confirmar} onChange={cambiar('confirmar')} error={errores.confirmar} />
        <Captcha
          version={versionCaptcha}
          valor={f.captchaRespuesta}
          error={errores.captcha}
          alCambiar={(c) => setF((x) => ({ ...x, ...c }))}
        />
        <label className="flex cursor-pointer items-start gap-3 text-[13.5px] text-ink-soft">
          <input type="checkbox" checked={aceptaTerminos} onChange={(e) => setAceptaTerminos(e.target.checked)} className="mt-0.5 h-4 w-4 shrink-0 rounded accent-ink" />
          <span>
            Acepto los términos de uso y la política de tratamiento de datos. Te enviaremos un código para confirmar tu correo.
            {errores.terminos && <span className="mt-1 block text-brand">{errores.terminos}</span>}
          </span>
        </label>
        {error && <ErrorFormulario error={error.status === 409 ? 'Ese correo ya está registrado. ¿Quieres iniciar sesión?' : error} />}
        <Button type="submit" completo tamano="lg" cargando={enviando}>
          Crear cuenta como {f.rol === ROLES.HUESPED ? 'huésped' : 'anfitrión'}
        </Button>
      </form>
    </AuthShell>
  );
}

/** Solicitud de recuperación (HU-04). Con correo simulado el backend devuelve el token para probar el flujo. */
export function RecoverPage() {
  const [correo, setCorreo] = useState('');
  const [error, setError] = useState(null);
  const [enviando, setEnviando] = useState(false);
  const [respuesta, setRespuesta] = useState(null);

  const enviar = async (e) => {
    e.preventDefault();
    const err = validarCorreo(correo);
    if (err) {
      setError(err);
      return;
    }
    setEnviando(true);
    setError(null);
    try {
      setRespuesta(await authApi.recuperar(correo));
    } catch (er) {
      setError(er);
    } finally {
      setEnviando(false);
    }
  };

  if (respuesta) {
    return (
      <AuthShell titulo="Revisa tu correo" subtitulo={respuesta.mensaje}>
        <div className="flex flex-col items-center gap-4 rounded-3xl bg-canvas p-6 text-center">
          <MailCheck className="h-10 w-10 text-accent" strokeWidth={1.5} />
          <p className="text-sm text-ink-soft">El enlace vence en 30 minutos y solo se puede usar una vez.</p>
        </div>
        {respuesta.tokenRecuperacionSimulado && (
          <div className="mt-5 rounded-2xl border border-amber-200 bg-amber-50 p-4 text-sm text-amber-900">
            <p className="font-semibold">Modo de correo simulado</p>
            <p className="mt-1">El backend no envía correos reales en este modo. Usa el enlace generado:</p>
            <Button className="mt-3" tamano="sm" to={`/restablecer?token=${encodeURIComponent(respuesta.tokenRecuperacionSimulado)}`}>
              Restablecer mi clave ahora
            </Button>
          </div>
        )}
        <Button variante="ghost" completo className="mt-4" to="/login">
          Volver a iniciar sesión
        </Button>
      </AuthShell>
    );
  }

  return (
    <AuthShell titulo="Recupera tu clave" subtitulo="Te enviaremos un enlace para crear una nueva." pie={<Link to="/login" className="font-medium text-accent hover:underline">Volver a iniciar sesión</Link>}>
      <form onSubmit={enviar} noValidate className="space-y-4">
        <Input label="Correo" type="email" autoComplete="email" value={correo} onChange={(e) => setCorreo(e.target.value)} error={typeof error === 'string' ? error : undefined} autoFocus />
        {error && typeof error !== 'string' && <ErrorFormulario error={error} />}
        <Button type="submit" completo tamano="lg" cargando={enviando} icono={KeyRound}>
          Enviar enlace
        </Button>
      </form>
    </AuthShell>
  );
}

/** Restablecer clave con el token del enlace /restablecer?token=... (HU-04). */
export function ResetPasswordPage() {
  const [sp] = useSearchParams();
  const toast = useToast();
  const navegar = useNavigate();
  const [f, setF] = useState({ token: sp.get('token') || '', nuevaClave: '', confirmar: '' });
  const [errores, setErrores] = useState({});
  const [error, setError] = useState(null);
  const [enviando, setEnviando] = useState(false);

  const enviar = async (e) => {
    e.preventDefault();
    const v = validarRestablecer(f);
    setErrores(v);
    setError(null);
    if (Object.keys(v).length) return;
    setEnviando(true);
    try {
      const r = await authApi.restablecer(f);
      toast.exito(r?.mensaje || 'La clave fue actualizada.');
      navegar('/login', { replace: true });
    } catch (er) {
      setError(er.status === 401 ? 'El enlace venció o ya fue usado. Solicita uno nuevo.' : er);
    } finally {
      setEnviando(false);
    }
  };

  return (
    <AuthShell titulo="Crea una nueva clave" subtitulo="Elige una clave segura que no uses en otros sitios.">
      <form onSubmit={enviar} noValidate className="space-y-4">
        {!sp.get('token') && <Input label="Código de recuperación" value={f.token} onChange={(e) => setF({ ...f, token: e.target.value })} error={errores.token} />}
        <Input label="Nueva clave" type="password" autoComplete="new-password" maxLength={64} value={f.nuevaClave} onChange={(e) => setF({ ...f, nuevaClave: e.target.value })} error={errores.nuevaClave} ayuda="Entre 8 y 64 caracteres." />
        <Input label="Confirmar clave" type="password" autoComplete="new-password" maxLength={64} value={f.confirmar} onChange={(e) => setF({ ...f, confirmar: e.target.value })} error={errores.confirmar} />
        {errores.token && sp.get('token') && <p className="text-sm text-brand">{errores.token}</p>}
        {error && <ErrorFormulario error={error} />}
        {error && (
          <Link to="/recuperar" className="block text-center text-sm font-medium text-accent hover:underline">
            Solicitar un enlace nuevo
          </Link>
        )}
        <Button type="submit" completo tamano="lg" cargando={enviando}>
          Guardar nueva clave
        </Button>
      </form>
    </AuthShell>
  );
}
