package co.edu.unbosque.utils;

import java.math.BigDecimal;
import java.util.logging.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * PATRÓN ADAPTADOR (PAT-01) — corresponde a IPasarelaPago del diagrama de componentes.
 * Es la única clase que el negocio usa para cobrar o reembolsar. Por dentro:
 *  - elige la fábrica de la pasarela según pagos.modo (Abstract Factory),
 *  - envuelve cada llamada en el cortacircuitos con tiempo límite de 10 s (ADR-04).
 */
@Component
public class PasarelaPagoAdapter {

    private static final Logger logger = Logger.getLogger(PasarelaPagoAdapter.class.getName());

    private final FabricaPasarelaPago fabrica;
    private final Cortacircuitos cortacircuitos;

    public PasarelaPagoAdapter(@Value("${pagos.modo}") String modo,
                               @Value("${stripe.secret.key}") String stripeKey,
                               Cortacircuitos cortacircuitos) {
        this.fabrica = "stripe".equalsIgnoreCase(modo) ? new FabricaPagoStripe(stripeKey) : new FabricaPagoSimulado();
        this.cortacircuitos = cortacircuitos;
        logger.info("pasarela_pagos=" + fabrica.nombre());
    }

    public ResultadoPago cobrar(BigDecimal monto, String metodoPago, String claveIdempotencia, String descripcion) {
        ProcesadorCobro procesador = fabrica.crearProcesadorCobro();
        return cortacircuitos.ejecutar(() -> procesador.cobrar(monto, metodoPago, claveIdempotencia, descripcion));
    }

    public ResultadoPago reembolsar(String referenciaPago, BigDecimal monto, String claveIdempotencia) {
        ProcesadorReembolso procesador = fabrica.crearProcesadorReembolso();
        return cortacircuitos.ejecutar(() -> procesador.reembolsar(referenciaPago, monto, claveIdempotencia));
    }

    public String getNombrePasarela() {
        return fabrica.nombre();
    }

    public String getEstadoCortacircuitos() {
        return cortacircuitos.getEstado().name();
    }
}
