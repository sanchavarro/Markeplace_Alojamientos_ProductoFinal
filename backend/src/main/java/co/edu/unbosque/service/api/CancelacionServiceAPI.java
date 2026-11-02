package co.edu.unbosque.service.api;

import co.edu.unbosque.dto.CancelacionResponse;
import co.edu.unbosque.entity.Cancelacion;
import co.edu.unbosque.utils.GenericServiceAPI;

/**
 * ICancelacion del diagrama de componentes (épica E08, HU-32 a HU-35).
 */
public interface CancelacionServiceAPI extends GenericServiceAPI<Cancelacion, Long> {

    /** Muestra cuánto se devolvería sin cancelar todavía. */
    CancelacionResponse simular(Long usuarioId, String rol, Long reservaId);

    CancelacionResponse cancelar(Long usuarioId, String rol, Long reservaId, String motivo);

    /** HU-33: reintenta los reembolsos que quedaron en REEMBOLSO_PENDIENTE. Devuelve cuántos se completaron. */
    int reintentarReembolsosPendientes();
}
