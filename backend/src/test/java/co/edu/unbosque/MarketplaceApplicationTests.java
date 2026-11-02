package co.edu.unbosque;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Verifica que la aplicación arranca: entidades, schema-extra.sql, seguridad y datos iniciales.
 * Requiere la base marketplace_test (ver docs/base_de_datos.sql).
 */
@SpringBootTest
@ActiveProfiles("test")
class MarketplaceApplicationTests {

    @Test
    void contextLoads() {
    }
}
