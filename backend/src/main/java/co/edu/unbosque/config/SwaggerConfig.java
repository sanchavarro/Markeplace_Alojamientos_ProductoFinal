package co.edu.unbosque.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

/**
 * Documentación de la API (MKT-102). Se abre en http://localhost:8080/swagger-ui.html
 * El botón "Authorize" recibe el token JWT del login para probar las rutas protegidas.
 */
@Configuration
public class SwaggerConfig {

    private static final String ESQUEMA_JWT = "bearerAuth";

    @Bean
    public OpenAPI documentacionApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("API Marketplace de Alojamientos - Inversiones LR")
                        .description("Backend del marketplace: autenticación, publicaciones, búsqueda, cotización, "
                                + "reservas con pago, cancelación, reseñas, administración y reportes.")
                        .version("1.0"))
                .addSecurityItem(new SecurityRequirement().addList(ESQUEMA_JWT))
                .components(new Components().addSecuritySchemes(ESQUEMA_JWT, new SecurityScheme()
                        .name(ESQUEMA_JWT)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")));
    }
}
