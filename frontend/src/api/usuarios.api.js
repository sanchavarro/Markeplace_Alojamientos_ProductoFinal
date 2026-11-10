import { http, datos } from './client';

/**
 * Épica E02 · UsuarioRestController (/usuarios).
 * ActualizarPerfilRequest { nombre, telefono, fotoUrl, claveActual, nuevaClave } (HU-05). El correo no se edita.
 */
export const usuariosApi = {
  perfil: () => datos(http.get('/usuarios/perfil')),

  actualizar: ({ nombre, telefono, fotoUrl, claveActual, nuevaClave }) => {
    const cuerpo = {
      nombre: nombre.trim(),
      telefono: telefono?.trim() || '',
      fotoUrl: fotoUrl?.trim() || null,
    };
    if (nuevaClave) {
      cuerpo.claveActual = claveActual;
      cuerpo.nuevaClave = nuevaClave;
    }
    // Un 401 "La clave actual no es correcta." no debe cerrar la sesión (ver interceptor en client.js).
    return datos(http.put('/usuarios/perfil', cuerpo, { error401Negocio: /clave actual/i }));
  },

  /** HU-06. 409 si hay reservas confirmadas pendientes. */
  desactivar: () => datos(http.patch('/usuarios/perfil/desactivar')),
};
