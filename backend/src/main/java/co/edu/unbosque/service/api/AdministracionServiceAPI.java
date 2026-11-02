package co.edu.unbosque.service.api;

import java.time.LocalDate;
import co.edu.unbosque.dto.ReservaResponse;
import java.util.Map;
import co.edu.unbosque.dto.AlojamientoDTO;
import co.edu.unbosque.dto.AlojamientoResumenDTO;
import co.edu.unbosque.dto.PaginaResponse;
import co.edu.unbosque.dto.UsuarioDTO;
import co.edu.unbosque.entity.EventoAuditoria;
import co.edu.unbosque.entity.Resena;

/**
 * IModeracion del diagrama de componentes (épica E11, HU-41 a HU-45).
 */
public interface AdministracionServiceAPI {

    PaginaResponse<AlojamientoResumenDTO> publicacionesPendientes(Integer pagina, Integer tamano);

    AlojamientoDTO gestionarPublicacion(Long adminId, Long alojamientoId, String accion, String motivo);

    PaginaResponse<UsuarioDTO> usuarios(String estado, Integer pagina, Integer tamano);

    UsuarioDTO gestionarUsuario(Long adminId, Long usuarioId, String accion, String motivo);

    Resena cambiarVisibilidadResena(Long adminId, Long resenaId, boolean visible, String motivo);

    PaginaResponse<EventoAuditoria> auditoria(String entidad, Long usuarioId, String accion, LocalDate desde,
                                              LocalDate hasta, Integer pagina, Integer tamano);

    PaginaResponse<ReservaResponse> reservasEnRevision(Integer pagina, Integer tamano);

    /** HU-42: MANTENER la reserva o CANCELAR con devolución total. */
    Object resolverReservaEnRevision(Long adminId, Long reservaId, String accion, String motivo);

    Map<String, Integer> ejecutarTareasProgramadas();
}
