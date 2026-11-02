-- ==========================================================================
-- Restricciones que JPA no crea por sí solo (se ejecuta al iniciar, es repetible).
-- Las sentencias se separan con doble arroba (ver spring.sql.init.separator).
-- 1. Llaves foráneas del modelo relacional
-- 2. Restricciones CHECK de estados y de negocio
-- 3. ADR-06: evento_auditoria solo admite INSERT (trigger)
-- ==========================================================================

-- 1. Llaves foráneas
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_notificacion_usuario') THEN
        ALTER TABLE notificacion ADD CONSTRAINT fk_notificacion_usuario FOREIGN KEY (usuario_id) REFERENCES usuario(id);
    END IF;
END $$
@@
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_clave_idempotencia_usuario') THEN
        ALTER TABLE clave_idempotencia ADD CONSTRAINT fk_clave_idempotencia_usuario FOREIGN KEY (usuario_id) REFERENCES usuario(id);
    END IF;
END $$
@@
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_clave_idempotencia_reserva') THEN
        ALTER TABLE clave_idempotencia ADD CONSTRAINT fk_clave_idempotencia_reserva FOREIGN KEY (reserva_id) REFERENCES reserva(id);
    END IF;
END $$
@@
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_alojamiento_anfitrion') THEN
        ALTER TABLE alojamiento ADD CONSTRAINT fk_alojamiento_anfitrion FOREIGN KEY (anfitrion_id) REFERENCES usuario(id);
    END IF;
END $$
@@
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_alojamiento_politica') THEN
        ALTER TABLE alojamiento ADD CONSTRAINT fk_alojamiento_politica FOREIGN KEY (politica_id) REFERENCES politica_cancelacion(id);
    END IF;
END $$
@@
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_imagen_alojamiento') THEN
        ALTER TABLE imagen ADD CONSTRAINT fk_imagen_alojamiento FOREIGN KEY (alojamiento_id) REFERENCES alojamiento(id);
    END IF;
END $$
@@
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_servicio_adicional_alojamiento') THEN
        ALTER TABLE servicio_adicional ADD CONSTRAINT fk_servicio_adicional_alojamiento FOREIGN KEY (alojamiento_id) REFERENCES alojamiento(id);
    END IF;
END $$
@@
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_alojamiento_servicio_alojamiento') THEN
        ALTER TABLE alojamiento_servicio ADD CONSTRAINT fk_alojamiento_servicio_alojamiento FOREIGN KEY (alojamiento_id) REFERENCES alojamiento(id);
    END IF;
END $$
@@
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_alojamiento_servicio_servicio') THEN
        ALTER TABLE alojamiento_servicio ADD CONSTRAINT fk_alojamiento_servicio_servicio FOREIGN KEY (servicio_id) REFERENCES servicio(id);
    END IF;
END $$
@@
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_periodo_disponibilidad_alojamiento') THEN
        ALTER TABLE periodo_disponibilidad ADD CONSTRAINT fk_periodo_disponibilidad_alojamiento FOREIGN KEY (alojamiento_id) REFERENCES alojamiento(id);
    END IF;
END $$
@@
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_periodo_disponibilidad_reserva') THEN
        ALTER TABLE periodo_disponibilidad ADD CONSTRAINT fk_periodo_disponibilidad_reserva FOREIGN KEY (reserva_id) REFERENCES reserva(id);
    END IF;
END $$
@@
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_reserva_huesped') THEN
        ALTER TABLE reserva ADD CONSTRAINT fk_reserva_huesped FOREIGN KEY (huesped_id) REFERENCES usuario(id);
    END IF;
END $$
@@
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_reserva_alojamiento') THEN
        ALTER TABLE reserva ADD CONSTRAINT fk_reserva_alojamiento FOREIGN KEY (alojamiento_id) REFERENCES alojamiento(id);
    END IF;
END $$
@@
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_reserva_servicio_adicional_reserva') THEN
        ALTER TABLE reserva_servicio_adicional ADD CONSTRAINT fk_reserva_servicio_adicional_reserva FOREIGN KEY (reserva_id) REFERENCES reserva(id);
    END IF;
END $$
@@
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_reserva_servicio_adicional_servicio_adicional') THEN
        ALTER TABLE reserva_servicio_adicional ADD CONSTRAINT fk_reserva_servicio_adicional_servicio_adicional FOREIGN KEY (servicio_adicional_id) REFERENCES servicio_adicional(id);
    END IF;
END $$
@@
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_pago_reserva') THEN
        ALTER TABLE pago ADD CONSTRAINT fk_pago_reserva FOREIGN KEY (reserva_id) REFERENCES reserva(id);
    END IF;
END $$
@@
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_cancelacion_reserva') THEN
        ALTER TABLE cancelacion ADD CONSTRAINT fk_cancelacion_reserva FOREIGN KEY (reserva_id) REFERENCES reserva(id);
    END IF;
END $$
@@
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_cancelacion_usuario') THEN
        ALTER TABLE cancelacion ADD CONSTRAINT fk_cancelacion_usuario FOREIGN KEY (usuario_id) REFERENCES usuario(id);
    END IF;
END $$
@@
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_resena_reserva') THEN
        ALTER TABLE resena ADD CONSTRAINT fk_resena_reserva FOREIGN KEY (reserva_id) REFERENCES reserva(id);
    END IF;
END $$
@@
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_resena_huesped') THEN
        ALTER TABLE resena ADD CONSTRAINT fk_resena_huesped FOREIGN KEY (huesped_id) REFERENCES usuario(id);
    END IF;
END $$
@@
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_resena_alojamiento') THEN
        ALTER TABLE resena ADD CONSTRAINT fk_resena_alojamiento FOREIGN KEY (alojamiento_id) REFERENCES alojamiento(id);
    END IF;
END $$
@@

-- 2. Restricciones CHECK
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_usuario_rol') THEN
        ALTER TABLE usuario ADD CONSTRAINT ck_usuario_rol CHECK (rol IN ('HUESPED','ANFITRION','ADMINISTRADOR'));
    END IF;
END $$
@@
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_usuario_estado') THEN
        ALTER TABLE usuario ADD CONSTRAINT ck_usuario_estado CHECK (estado IN ('ACTIVO','INACTIVO','RESTRINGIDO'));
    END IF;
END $$
@@
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_usuario_intentos') THEN
        ALTER TABLE usuario ADD CONSTRAINT ck_usuario_intentos CHECK (intentos_fallidos >= 0);
    END IF;
END $$
@@
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_alojamiento_estado') THEN
        ALTER TABLE alojamiento ADD CONSTRAINT ck_alojamiento_estado CHECK (estado IN ('BORRADOR','PENDIENTE_REVISION','ACTIVA','PAUSADA','BLOQUEADA'));
    END IF;
END $$
@@
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_alojamiento_modo_reserva') THEN
        ALTER TABLE alojamiento ADD CONSTRAINT ck_alojamiento_modo_reserva CHECK (modo_reserva IN ('INMEDIATA','APROBACION'));
    END IF;
END $$
@@
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_alojamiento_capacidad') THEN
        ALTER TABLE alojamiento ADD CONSTRAINT ck_alojamiento_capacidad CHECK (capacidad > 0);
    END IF;
END $$
@@
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_alojamiento_precio_noche') THEN
        ALTER TABLE alojamiento ADD CONSTRAINT ck_alojamiento_precio_noche CHECK (precio_noche > 0);
    END IF;
END $$
@@
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_alojamiento_tarifa_limpieza') THEN
        ALTER TABLE alojamiento ADD CONSTRAINT ck_alojamiento_tarifa_limpieza CHECK (tarifa_limpieza IS NULL OR tarifa_limpieza >= 0);
    END IF;
END $$
@@
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_servicio_adicional_precio') THEN
        ALTER TABLE servicio_adicional ADD CONSTRAINT ck_servicio_adicional_precio CHECK (precio >= 0);
    END IF;
END $$
@@
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_politica_tipo') THEN
        ALTER TABLE politica_cancelacion ADD CONSTRAINT ck_politica_tipo CHECK (tipo IN ('FLEXIBLE','ESTRICTA'));
    END IF;
END $$
@@
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_periodo_estado') THEN
        ALTER TABLE periodo_disponibilidad ADD CONSTRAINT ck_periodo_estado CHECK (estado IN ('DISPONIBLE','BLOQUEADO','RESERVADO'));
    END IF;
END $$
@@
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_periodo_fechas') THEN
        ALTER TABLE periodo_disponibilidad ADD CONSTRAINT ck_periodo_fechas CHECK (fecha_fin > fecha_inicio);
    END IF;
END $$
@@
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_periodo_reservado_con_reserva') THEN
        ALTER TABLE periodo_disponibilidad ADD CONSTRAINT ck_periodo_reservado_con_reserva CHECK ((estado = 'RESERVADO') = (reserva_id IS NOT NULL));
    END IF;
END $$
@@
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_configuracion_valor') THEN
        ALTER TABLE configuracion_tarifa ADD CONSTRAINT ck_configuracion_valor CHECK (valor >= 0);
    END IF;
END $$
@@
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_reserva_estado') THEN
        ALTER TABLE reserva ADD CONSTRAINT ck_reserva_estado CHECK (estado IN ('PENDIENTE_PAGO','CONFIRMADA','PAGO_FALLIDO','CANCELADA','COMPLETADA','EN_REVISION'));
    END IF;
END $$
@@
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_reserva_fechas') THEN
        ALTER TABLE reserva ADD CONSTRAINT ck_reserva_fechas CHECK (fecha_salida > fecha_llegada);
    END IF;
END $$
@@
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_reserva_huespedes') THEN
        ALTER TABLE reserva ADD CONSTRAINT ck_reserva_huespedes CHECK (num_huespedes > 0);
    END IF;
END $$
@@
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_reserva_noches') THEN
        ALTER TABLE reserva ADD CONSTRAINT ck_reserva_noches CHECK (noches > 0);
    END IF;
END $$
@@
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_reserva_montos') THEN
        ALTER TABLE reserva ADD CONSTRAINT ck_reserva_montos CHECK (precio_base >= 0 AND tarifa_limpieza >= 0 AND tarifa_servicio >= 0 AND descuento >= 0 AND total >= 0);
    END IF;
END $$
@@
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_reserva_servicio_precio') THEN
        ALTER TABLE reserva_servicio_adicional ADD CONSTRAINT ck_reserva_servicio_precio CHECK (precio >= 0);
    END IF;
END $$
@@
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_pago_estado') THEN
        ALTER TABLE pago ADD CONSTRAINT ck_pago_estado CHECK (estado IN ('APROBADO','RECHAZADO','DEVUELTO'));
    END IF;
END $$
@@
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_pago_monto') THEN
        ALTER TABLE pago ADD CONSTRAINT ck_pago_monto CHECK (monto >= 0);
    END IF;
END $$
@@
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_cancelacion_devolucion') THEN
        ALTER TABLE cancelacion ADD CONSTRAINT ck_cancelacion_devolucion CHECK (valor_devolucion >= 0);
    END IF;
END $$
@@
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_resena_calificacion') THEN
        ALTER TABLE resena ADD CONSTRAINT ck_resena_calificacion CHECK (calificacion BETWEEN 1 AND 5);
    END IF;
END $$
@@
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_outbox_estado') THEN
        ALTER TABLE evento_outbox ADD CONSTRAINT ck_outbox_estado CHECK (estado IN ('PENDIENTE','PROCESADO','ERROR'));
    END IF;
END $$
@@
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_outbox_intentos') THEN
        ALTER TABLE evento_outbox ADD CONSTRAINT ck_outbox_intentos CHECK (intentos BETWEEN 0 AND 3);
    END IF;
END $$
@@
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_notificacion_tipo') THEN
        ALTER TABLE notificacion ADD CONSTRAINT ck_notificacion_tipo CHECK (length(tipo) > 0);
    END IF;
END $$
@@
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_auditoria_resultado') THEN
        ALTER TABLE evento_auditoria ADD CONSTRAINT ck_auditoria_resultado CHECK (resultado IN ('EXITO','FALLO'));
    END IF;
END $$
@@

-- Exportaciones de reportes en segundo plano (HU-47)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_reporte_exportacion_admin') THEN
        ALTER TABLE reporte_exportacion ADD CONSTRAINT fk_reporte_exportacion_admin FOREIGN KEY (admin_id) REFERENCES usuario(id);
    END IF;
END $$
@@

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_reporte_exportacion_estado') THEN
        ALTER TABLE reporte_exportacion ADD CONSTRAINT ck_reporte_exportacion_estado CHECK (estado IN ('PENDIENTE','PROCESANDO','LISTO','ERROR'));
    END IF;
END $$
@@

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_reporte_exportacion_formato') THEN
        ALTER TABLE reporte_exportacion ADD CONSTRAINT ck_reporte_exportacion_formato CHECK (formato IN ('CSV','PDF','EXCEL'));
    END IF;
END $$
@@

-- 3. Auditoría de solo inserción (ADR-06, T14 Mantener rastro de auditoría)

CREATE OR REPLACE FUNCTION fn_auditoria_solo_insercion() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'evento_auditoria es de solo inserción: no se permite % (ADR-06)', TG_OP;
END;
$$ LANGUAGE plpgsql
@@
DROP TRIGGER IF EXISTS tg_auditoria_solo_insercion ON evento_auditoria
@@
CREATE TRIGGER tg_auditoria_solo_insercion
    BEFORE UPDATE OR DELETE ON evento_auditoria
    FOR EACH ROW EXECUTE FUNCTION fn_auditoria_solo_insercion()
@@
