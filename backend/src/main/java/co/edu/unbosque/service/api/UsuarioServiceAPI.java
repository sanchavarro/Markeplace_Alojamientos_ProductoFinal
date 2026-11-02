package co.edu.unbosque.service.api;

import co.edu.unbosque.dto.ActualizarPerfilRequest;
import co.edu.unbosque.dto.PaginaResponse;
import co.edu.unbosque.dto.UsuarioDTO;
import co.edu.unbosque.entity.Usuario;
import co.edu.unbosque.utils.GenericServiceAPI;

/**
 * IUsuarios del diagrama de componentes: perfil (HU-05, HU-06) y gestión por el administrador (HU-44).
 */
public interface UsuarioServiceAPI extends GenericServiceAPI<Usuario, Long> {

    Usuario obtener(Long id);

    UsuarioDTO perfil(Long usuarioId);

    UsuarioDTO actualizarPerfil(Long usuarioId, ActualizarPerfilRequest request);

    UsuarioDTO desactivarCuenta(Long usuarioId);

    PaginaResponse<UsuarioDTO> listar(String estado, Integer pagina, Integer tamano);

    UsuarioDTO cambiarEstadoPorAdministrador(Long adminId, Long usuarioId, String nuevoEstado, String motivo);

    UsuarioDTO aDTO(Usuario usuario);
}
