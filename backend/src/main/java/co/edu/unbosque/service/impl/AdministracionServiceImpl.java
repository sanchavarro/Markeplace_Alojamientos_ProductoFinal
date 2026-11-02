package co.edu.unbosque.service.impl;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import co.edu.unbosque.dto.AlojamientoDTO;
import co.edu.unbosque.dto.AlojamientoResumenDTO;
import co.edu.unbosque.dto.PaginaResponse;
import co.edu.unbosque.dto.ReservaResponse;
import co.edu.unbosque.dto.UsuarioDTO;
import co.edu.unbosque.entity.Alojamiento;
import co.edu.unbosque.entity.EventoAuditoria;
import co.edu.unbosque.entity.Resena;
import co.edu.unbosque.service.api.AdministracionServiceAPI;
import co.edu.unbosque.service.api.AlojamientoServiceAPI;
import co.edu.unbosque.service.api.CancelacionServiceAPI;
import co.edu.unbosque.service.api.EventoAuditoriaServiceAPI;
import co.edu.unbosque.service.api.ResenaServiceAPI;
import co.edu.unbosque.service.api.ReservaServiceAPI;
import co.edu.unbosque.service.api.UsuarioServiceAPI;
import co.edu.unbosque.utils.Constantes;
import co.edu.unbosque.utils.ReglaNegocioException;

@Service
public class AdministracionServiceImpl implements AdministracionServiceAPI {

    @Autowired
    private AlojamientoServiceAPI alojamientoServiceAPI;

    @Autowired
    private UsuarioServiceAPI usuarioServiceAPI;

    @Autowired
    private ResenaServiceAPI resenaServiceAPI;

    @Autowired
    private ReservaServiceAPI reservaServiceAPI;

    @Autowired
    private EventoAuditoriaServiceAPI eventoAuditoriaServiceAPI;

    @Autowired
    private CancelacionServiceAPI cancelacionServiceAPI;

    @Override
    public PaginaResponse<AlojamientoResumenDTO> publicacionesPendientes(Integer pagina, Integer tamano) {
        return alojamientoServiceAPI.pendientesDeRevision(pagina, tamano);
    }

    /**
     * HU-42 BLOQUEAR / DESBLOQUEAR, HU-43 APROBAR / PAUSAR.
     * PAUSAR una publicación pendiente la devuelve al anfitrión como BORRADOR para que la corrija.
     */
    @Override
    @Transactional
    public AlojamientoDTO gestionarPublicacion(Long adminId, Long alojamientoId, String accion, String motivo) {
        Alojamiento alojamiento = alojamientoServiceAPI.obtener(alojamientoId);
        String nuevoEstado;
        switch (accion.trim().toUpperCase()) {
            case "APROBAR":
                nuevoEstado = Constantes.ALOJAMIENTO_ACTIVA;
                break;
            case "PAUSAR":
                nuevoEstado = Constantes.ALOJAMIENTO_PENDIENTE_REVISION.equals(alojamiento.getEstado())
                        ? Constantes.ALOJAMIENTO_BORRADOR
                        : Constantes.ALOJAMIENTO_PAUSADA;
                break;
            case "BLOQUEAR":
                nuevoEstado = Constantes.ALOJAMIENTO_BLOQUEADA;
                break;
            case "DESBLOQUEAR":
                nuevoEstado = Constantes.ALOJAMIENTO_PENDIENTE_REVISION;
                break;
            default:
                throw new ReglaNegocioException("Acción no válida. Usa APROBAR, PAUSAR, BLOQUEAR o DESBLOQUEAR.");
        }
        return alojamientoServiceAPI.cambiarEstadoPorAdministrador(adminId, alojamientoId, nuevoEstado, motivo);
    }

    @Override
    public PaginaResponse<UsuarioDTO> usuarios(String estado, Integer pagina, Integer tamano) {
        return usuarioServiceAPI.listar(estado, pagina, tamano);
    }

    /** HU-44: RESTRINGIR o REACTIVAR. */
    @Override
    @Transactional
    public UsuarioDTO gestionarUsuario(Long adminId, Long usuarioId, String accion, String motivo) {
        if (adminId.equals(usuarioId)) {
            throw new ReglaNegocioException("No puedes cambiar el estado de tu propia cuenta.");
        }
        switch (accion.trim().toUpperCase()) {
            case "RESTRINGIR":
                return usuarioServiceAPI.cambiarEstadoPorAdministrador(adminId, usuarioId, Constantes.USUARIO_RESTRINGIDO,
                        motivo);
            case "REACTIVAR":
                return usuarioServiceAPI.cambiarEstadoPorAdministrador(adminId, usuarioId, Constantes.USUARIO_ACTIVO,
                        motivo);
            default:
                throw new ReglaNegocioException("Acción no válida. Usa RESTRINGIR o REACTIVAR.");
        }
    }

    @Override
    public Resena cambiarVisibilidadResena(Long adminId, Long resenaId, boolean visible, String motivo) {
        return resenaServiceAPI.cambiarVisibilidad(adminId, resenaId, visible, motivo);
    }

    /** HU-45: consulta de auditoría paginada (ADR-08). */
    @Override
    public PaginaResponse<EventoAuditoria> auditoria(String entidad, Long usuarioId, String accion, LocalDate desde,
                                                     LocalDate hasta, Integer pagina, Integer tamano) {
        return eventoAuditoriaServiceAPI.consultar(entidad, usuarioId, accion, desde, hasta, pagina, tamano);
    }

    @Override
    public PaginaResponse<ReservaResponse> reservasEnRevision(Integer pagina, Integer tamano) {
        return reservaServiceAPI.enRevisionManual(pagina, tamano);
    }

    /** HU-42: el administrador revisa las reservas de una publicación bloqueada. */
    @Override
    public Object resolverReservaEnRevision(Long adminId, Long reservaId, String accion, String motivo) {
        switch (accion.trim().toUpperCase()) {
            case "MANTENER":
                return reservaServiceAPI.mantenerTrasRevision(adminId, reservaId, motivo);
            case "CANCELAR":
                return cancelacionServiceAPI.cancelar(adminId, Constantes.ROL_ADMINISTRADOR, reservaId,
                        motivo == null || motivo.isBlank() ? "Publicación bloqueada por el administrador" : motivo);
            default:
                throw new ReglaNegocioException("Acción no válida. Usa MANTENER o CANCELAR.");
        }
    }

    /** Ejecuta a mano las tareas programadas (útil para probar con Postman). */
    @Override
    public Map<String, Integer> ejecutarTareasProgramadas() {
        Map<String, Integer> resultado = new LinkedHashMap<>();
        resultado.put("reservasCompletadas", reservaServiceAPI.completarReservasFinalizadas());
        resultado.put("pagosPendientesVencidos", reservaServiceAPI.vencerPagosPendientes());
        resultado.put("clavesIdempotenciaEliminadas", reservaServiceAPI.limpiarClavesIdempotencia());
        resultado.put("reembolsosReintentados", cancelacionServiceAPI.reintentarReembolsosPendientes());
        return resultado;
    }
}
