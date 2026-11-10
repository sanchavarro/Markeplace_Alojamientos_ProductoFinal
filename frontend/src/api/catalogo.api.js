import { http, datos, params } from './client';

/**
 * Consultas públicas: BusquedaRestController, CatalogoRestController, CotizacionRestController, TarifaRestController.
 */
export const catalogoApi = {
  /**
   * GET /busqueda (HU-19, HU-20, HU-21) → PaginaResponse<AlojamientoResumenDTO>
   * Parámetros: ciudad, tipo, capacidad, precioMin, precioMax, llegada, salida, servicios (List<Long>), pagina, tamano.
   */
  buscar: (filtros = {}, opciones = {}) =>
    datos(
      http.get('/busqueda', {
        params: params({
          ciudad: filtros.ciudad?.trim(),
          tipo: filtros.tipo,
          capacidad: filtros.capacidad,
          precioMin: filtros.precioMin,
          precioMax: filtros.precioMax,
          llegada: filtros.llegada,
          salida: filtros.salida,
          servicios: filtros.servicios,
          pagina: filtros.pagina,
          tamano: filtros.tamano,
        }),
        signal: opciones.signal,
      }),
    ),

  /** GET /busqueda/comparar?ids=1&ids=2 (HU-22) → List<AlojamientoDTO>. Entre 2 y 3 ids. */
  comparar: (ids) => datos(http.get('/busqueda/comparar', { params: { ids } })),

  politicas: () => datos(http.get('/catalogo/politicas')),
  servicios: () => datos(http.get('/catalogo/servicios')),
  tarifasCatalogo: () => datos(http.get('/catalogo/tarifas')),

  /** GET /tarifas → List<ConfiguracionTarifa> { id, clave, valor, vigenteDesde } (HU-24). */
  tarifas: () => datos(http.get('/tarifas')),

  /**
   * POST /cotizaciones (HU-23, HU-25) — CotizacionRequest
   * { alojamientoId, fechaLlegada, fechaSalida, numHuespedes, serviciosAdicionales: List<Long> }
   */
  cotizar: ({ alojamientoId, fechaLlegada, fechaSalida, numHuespedes, serviciosAdicionales = [] }, opciones = {}) =>
    datos(
      http.post(
        '/cotizaciones',
        { alojamientoId, fechaLlegada, fechaSalida, numHuespedes: Number(numHuespedes), serviciosAdicionales },
        { signal: opciones.signal },
      ),
    ),
};
