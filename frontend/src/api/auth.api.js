import { http, datos } from './client';

/**
 * Épica E01 · AuthRestController (/auth).
 * Cuerpos JSON idénticos a los DTO del backend:
 *  - RegistroRequest  { nombre, correo, clave, rol }            (HU-01)
 *  - LoginRequest     { correo, clave }                          (HU-02, HU-06)
 *  - RecuperarClaveRequest  { correo }                           (HU-04)
 *  - RestablecerClaveRequest { token, nuevaClave }               (HU-04)
 * LoginResponse = { token, tipo, expiraEnSegundos, usuario: UsuarioDTO }
 */
export const authApi = {
  /** GET /auth/captcha → { captchaId, imagen (data URI SVG), expiraEnSegundos, habilitado } */
  captcha: () => datos(http.get('/auth/captcha', { headers: { 'Cache-Control': 'no-store' } })),

  /**
   * Registro con captcha. Respuesta: { usuario, verificacionRequerida, mensaje, codigoVerificacionSimulado? }.
   * La cuenta no puede iniciar sesión hasta confirmar el correo.
   */
  registrar: ({ nombre, correo, clave, rol, captchaId, captchaRespuesta }) =>
    datos(
      http.post('/auth/registro', {
        nombre: nombre.trim(),
        correo: correo.trim(),
        clave,
        rol,
        captchaId: captchaId || null,
        captchaRespuesta: captchaRespuesta?.trim() || null,
      }),
    ),

  /** POST /auth/verificar-correo { correo, codigo } → LoginResponse (deja la sesión iniciada). */
  verificarCorreo: ({ correo, codigo }) => datos(http.post('/auth/verificar-correo', { correo: correo.trim(), codigo: codigo.trim() })),

  /** POST /auth/reenviar-verificacion { correo } → { mensaje, codigoVerificacionSimulado? } */
  reenviarVerificacion: (correo) => datos(http.post('/auth/reenviar-verificacion', { correo: correo.trim() })),

  login: ({ correo, clave }) => datos(http.post('/auth/login', { correo: correo.trim(), clave })),

  /** Revoca el token en el servidor (ListaTokensRevocados). Debe llamarse ANTES de limpiar el estado local. */
  logout: () => datos(http.post('/auth/logout')),

  reactivar: ({ correo, clave }) => datos(http.post('/auth/reactivar', { correo: correo.trim(), clave })),

  /** Respuesta: { mensaje, tokenRecuperacionSimulado? } (el token solo llega con correo.modo=simulado). */
  recuperar: (correo) => datos(http.post('/auth/recuperar', { correo: correo.trim() })),

  restablecer: ({ token, nuevaClave }) => datos(http.post('/auth/restablecer', { token: token.trim(), nuevaClave })),
};
