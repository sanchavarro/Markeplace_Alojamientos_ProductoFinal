package co.edu.unbosque.service.api;

import co.edu.unbosque.dto.ResenaRequest;
import co.edu.unbosque.dto.ResenasAlojamientoResponse;
import co.edu.unbosque.entity.Resena;
import co.edu.unbosque.utils.GenericServiceAPI;

/**
 * IResenas del diagrama de componentes (épica E10, HU-38 a HU-41).
 */
public interface ResenaServiceAPI extends GenericServiceAPI<Resena, Long> {

    Resena crear(Long huespedId, ResenaRequest request);

    ResenasAlojamientoResponse listar(Long alojamientoId, Integer pagina, Integer tamano);

    /** HU-41: ocultar (visible=false) o volver a mostrar una reseña, con motivo. */
    Resena cambiarVisibilidad(Long adminId, Long resenaId, boolean visible, String motivo);
}
