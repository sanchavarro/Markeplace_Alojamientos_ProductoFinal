package co.edu.unbosque.utils;

import java.util.List;

/**
 * Valores permitidos del modelo de datos (restricciones CHECK) y nombres de eventos.
 * Se usan como String en las entidades, igual que en la base de datos.
 */
public final class Constantes {

    private Constantes() {
    }

    // usuario.rol
    public static final String ROL_HUESPED = "HUESPED";
    public static final String ROL_ANFITRION = "ANFITRION";
    public static final String ROL_ADMINISTRADOR = "ADMINISTRADOR";
    public static final List<String> ROLES_REGISTRABLES = List.of(ROL_HUESPED, ROL_ANFITRION);

    // usuario.estado
    public static final String USUARIO_ACTIVO = "ACTIVO";
    public static final String USUARIO_INACTIVO = "INACTIVO";
    public static final String USUARIO_RESTRINGIDO = "RESTRINGIDO";

    // alojamiento.estado
    public static final String ALOJAMIENTO_BORRADOR = "BORRADOR";
    public static final String ALOJAMIENTO_PENDIENTE_REVISION = "PENDIENTE_REVISION";
    public static final String ALOJAMIENTO_ACTIVA = "ACTIVA";
    public static final String ALOJAMIENTO_PAUSADA = "PAUSADA";
    public static final String ALOJAMIENTO_BLOQUEADA = "BLOQUEADA";

    // Tipos de alojamiento permitidos
    public static final List<String> TIPOS_ALOJAMIENTO = List.of("CASA", "APARTAMENTO", "FINCA", "CABANA",
            "HABITACION", "GLAMPING");

    // alojamiento.modo_reserva
    public static final String MODO_INMEDIATA = "INMEDIATA";
    public static final String MODO_APROBACION = "APROBACION";

    // reserva.estado
    public static final String RESERVA_PENDIENTE_PAGO = "PENDIENTE_PAGO";
    public static final String RESERVA_CONFIRMADA = "CONFIRMADA";
    public static final String RESERVA_PAGO_FALLIDO = "PAGO_FALLIDO";
    public static final String RESERVA_CANCELADA = "CANCELADA";
    public static final String RESERVA_COMPLETADA = "COMPLETADA";
    public static final String RESERVA_EN_REVISION = "EN_REVISION";

    // periodo_disponibilidad.estado
    public static final String PERIODO_DISPONIBLE = "DISPONIBLE";
    public static final String PERIODO_BLOQUEADO = "BLOQUEADO";
    public static final String PERIODO_RESERVADO = "RESERVADO";

    // pago.estado
    public static final String PAGO_APROBADO = "APROBADO";
    public static final String PAGO_RECHAZADO = "RECHAZADO";
    public static final String PAGO_DEVUELTO = "DEVUELTO";
    /** No se guarda en la tabla pago: solo indica en la respuesta que la pasarela no respondió (503). */
    public static final String PAGO_NO_DISPONIBLE = "NO_DISPONIBLE";

    // politica_cancelacion.tipo
    public static final String POLITICA_FLEXIBLE = "FLEXIBLE";
    public static final String POLITICA_ESTRICTA = "ESTRICTA";

    // evento_outbox.estado
    public static final String OUTBOX_PENDIENTE = "PENDIENTE";
    public static final String OUTBOX_PROCESADO = "PROCESADO";
    public static final String OUTBOX_ERROR = "ERROR";

    // Tipos de evento del outbox (ADR-07)
    public static final String EVENTO_USUARIO_REGISTRADO = "USUARIO_REGISTRADO";
    public static final String EVENTO_RECUPERACION_CLAVE = "RECUPERACION_CLAVE";
    public static final String EVENTO_RESERVA_CONFIRMADA = "RESERVA_CONFIRMADA";
    public static final String EVENTO_RESERVA_POR_APROBAR = "RESERVA_POR_APROBAR";
    public static final String EVENTO_RESERVA_APROBADA = "RESERVA_APROBADA";
    public static final String EVENTO_PAGO_FALLIDO = "PAGO_FALLIDO";
    public static final String EVENTO_RESERVA_CANCELADA = "RESERVA_CANCELADA";
    public static final String EVENTO_PUBLICACION_ESTADO = "PUBLICACION_ESTADO";
    public static final String EVENTO_USUARIO_ESTADO = "USUARIO_ESTADO";
    public static final String EVENTO_REPORTE_LISTO = "REPORTE_LISTO";

    // Resultado de auditoría
    public static final String AUDITORIA_EXITO = "EXITO";
    public static final String AUDITORIA_FALLO = "FALLO";

    // Claves de configuracion_tarifa (HU-24)
    public static final String TARIFA_SERVICIO_PORCENTAJE = "TARIFA_SERVICIO_PORCENTAJE";
    public static final String DESCUENTO_SEMANAL_PORCENTAJE = "DESCUENTO_SEMANAL_PORCENTAJE";
    public static final String NOCHES_DESCUENTO_SEMANAL = "NOCHES_DESCUENTO_SEMANAL";

    // Límites de negocio
    public static final int NOCHES_MAXIMAS = 30;
}
