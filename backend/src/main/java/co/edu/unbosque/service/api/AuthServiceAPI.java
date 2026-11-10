package co.edu.unbosque.service.api;

import java.util.Map;
import co.edu.unbosque.dto.LoginRequest;
import co.edu.unbosque.dto.LoginResponse;
import co.edu.unbosque.dto.RegistroRequest;
import co.edu.unbosque.dto.RestablecerClaveRequest;
import co.edu.unbosque.dto.VerificarCorreoRequest;

/**
 * IAuth del diagrama de componentes (épica E01, ADR-05).
 */
public interface AuthServiceAPI {

    Map<String, Object> registrar(RegistroRequest request);

    LoginResponse verificarCorreo(VerificarCorreoRequest request);

    Map<String, Object> reenviarVerificacion(String correo);

    LoginResponse login(LoginRequest request);

    void logout(String token);

    LoginResponse reactivar(LoginRequest request);

    Map<String, Object> solicitarRecuperacion(String correo);

    void restablecerClave(RestablecerClaveRequest request);
}
