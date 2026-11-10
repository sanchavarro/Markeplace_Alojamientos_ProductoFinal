import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from 'react';
import { authApi, usuariosApi } from '../api';
import {
  EVENTO_SESION_EXPIRADA,
  guardarSesion,
  leerSesion,
  limpiarSesion,
  msParaExpirar,
} from '../utils/session';

/**
 * Contexto de autenticación (HU-02, HU-03, HU-06).
 *
 * Flujo de inicio de sesión corregido:
 *   1. POST /auth/login { correo, clave } → { token, tipo, expiraEnSegundos, usuario }.
 *   2. Se guarda el token (sessionStorage + memoria) con su expiración.
 *   3. Se pide de inmediato GET /usuarios/perfil con el token: el rol y el estado se toman de ahí
 *      (no del cuerpo del login ni del payload del JWT), evitando estados inconsistentes.
 *   4. Si el perfil falla, la sesión se descarta por completo (no queda un token huérfano).
 *
 * Al recargar la página se rehidrata la sesión pidiendo el perfil otra vez ("cargando" = true mientras tanto),
 * de modo que las rutas protegidas no redirigen al login antes de tiempo.
 */
const AuthContext = createContext(null);

export const ESTADO_CUENTA_DESACTIVADA = 'CUENTA_DESACTIVADA';
export const ESTADO_CORREO_NO_VERIFICADO = 'CORREO_NO_VERIFICADO';

/** El backend responde 423 "Tu cuenta está desactivada..." cuando el usuario está INACTIVO (HU-06). */
const esCuentaDesactivada = (error) => error?.status === 423 && /desactivad/i.test(error?.message || '');

export function AuthProvider({ children }) {
  const [usuario, setUsuario] = useState(null);
  const [cargando, setCargando] = useState(true);
  const [avisoSesion, setAvisoSesion] = useState(null);
  const temporizador = useRef(null);
  const usuarioRef = useRef(null);
  usuarioRef.current = usuario;

  const programarExpiracion = useCallback(() => {
    clearTimeout(temporizador.current);
    const ms = msParaExpirar();
    if (ms > 0) {
      temporizador.current = setTimeout(() => {
        limpiarSesion();
        setUsuario(null);
        setAvisoSesion('Tu sesión de 60 minutos terminó. Inicia sesión de nuevo.');
      }, Math.min(ms, 2147483647));
    }
  }, []);

  const cerrarLocal = useCallback((mensaje) => {
    clearTimeout(temporizador.current);
    limpiarSesion();
    setUsuario(null);
    if (mensaje) setAvisoSesion(mensaje);
  }, []);

  /** Carga el perfil con el token vigente. Si falla, la sesión se invalida. */
  const cargarPerfil = useCallback(async () => {
    const perfil = await usuariosApi.perfil();
    setUsuario(perfil);
    return perfil;
  }, []);

  // Rehidratación al montar la aplicación.
  useEffect(() => {
    let activo = true;
    (async () => {
      if (!leerSesion()) {
        setCargando(false);
        return;
      }
      try {
        const perfil = await usuariosApi.perfil();
        if (activo) {
          setUsuario(perfil);
          programarExpiracion();
        }
      } catch {
        if (activo) cerrarLocal();
      } finally {
        if (activo) setCargando(false);
      }
    })();
    return () => {
      activo = false;
    };
  }, [cerrarLocal, programarExpiracion]);

  // El interceptor de Axios avisa cuando el backend rechaza el token (vencido, revocado o cuenta restringida).
  useEffect(() => {
    const alExpirar = (e) => {
      if (usuarioRef.current) setAvisoSesion(e.detail?.mensaje || 'Tu sesión terminó. Inicia sesión de nuevo.');
      setUsuario(null);
      clearTimeout(temporizador.current);
    };
    window.addEventListener(EVENTO_SESION_EXPIRADA, alExpirar);
    return () => window.removeEventListener(EVENTO_SESION_EXPIRADA, alExpirar);
  }, []);

  useEffect(() => () => clearTimeout(temporizador.current), []);

  const establecerSesion = useCallback(
    async (respuestaLogin) => {
      guardarSesion(respuestaLogin);
      try {
        const perfil = await cargarPerfil();
        programarExpiracion();
        setAvisoSesion(null);
        return perfil;
      } catch (error) {
        cerrarLocal();
        throw error;
      }
    },
    [cargarPerfil, cerrarLocal, programarExpiracion],
  );

  const login = useCallback(
    async (credenciales) => {
      try {
        const respuesta = await authApi.login(credenciales);
        return await establecerSesion(respuesta);
      } catch (error) {
        if (esCuentaDesactivada(error)) error.codigo = ESTADO_CUENTA_DESACTIVADA;
        // 403 { codigo: "CORREO_NO_VERIFICADO" }: la cuenta existe pero falta confirmar el correo.
        if (error?.datos?.codigo === ESTADO_CORREO_NO_VERIFICADO) error.codigo = ESTADO_CORREO_NO_VERIFICADO;
        throw error;
      }
    },
    [establecerSesion],
  );

  const reactivar = useCallback(
    async (credenciales) => {
      const respuesta = await authApi.reactivar(credenciales);
      return establecerSesion(respuesta);
    },
    [establecerSesion],
  );

  const registrar = useCallback((datos) => authApi.registrar(datos), []);

  /** Confirma el correo con el código de 6 dígitos; el backend devuelve la sesión iniciada. */
  const verificarCorreo = useCallback(
    async (datos) => {
      const respuesta = await authApi.verificarCorreo(datos);
      return establecerSesion(respuesta);
    },
    [establecerSesion],
  );

  /** HU-02: primero se revoca el token en el servidor y después se limpia el estado local (siempre). */
  const logout = useCallback(async () => {
    try {
      if (leerSesion()) await authApi.logout();
    } catch {
      /* Si el token ya venció el servidor responde 401: igual se limpia la sesión local. */
    } finally {
      cerrarLocal();
    }
  }, [cerrarLocal]);

  const refrescarPerfil = useCallback(async () => {
    try {
      return await cargarPerfil();
    } catch (error) {
      if (error?.status === 401) cerrarLocal();
      throw error;
    }
  }, [cargarPerfil, cerrarLocal]);

  const valor = useMemo(
    () => ({
      usuario,
      rol: usuario?.rol || null,
      autenticado: Boolean(usuario),
      cargando,
      avisoSesion,
      limpiarAviso: () => setAvisoSesion(null),
      login,
      reactivar,
      registrar,
      verificarCorreo,
      logout,
      cerrarLocal,
      refrescarPerfil,
      setUsuario,
      tieneRol: (...roles) => Boolean(usuario && roles.includes(usuario.rol)),
    }),
    [usuario, cargando, avisoSesion, login, reactivar, registrar, verificarCorreo, logout, cerrarLocal, refrescarPerfil],
  );

  return <AuthContext.Provider value={valor}>{children}</AuthContext.Provider>;
}

export const useAuth = () => {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth debe usarse dentro de <AuthProvider>.');
  return ctx;
};
