package com.taquillacine.api.support;

import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifica que las pruebas de integración se ejecuten únicamente contra la base
 * local aislada autorizada. Si el entorno apunta a otra base o a un host remoto,
 * las pruebas fallan de inmediato antes de crear o modificar datos.
 */
public final class GuardiaBaseDatosPruebas {

    public static final String BASE_AUTORIZADA = "taquilla_cine_test";

    private GuardiaBaseDatosPruebas() {
    }

    public static void verificarEntornoAutorizado(JdbcTemplate jdbcTemplate) {
        String baseActual = jdbcTemplate.queryForObject("SELECT current_database()", String.class);
        String hostActual = jdbcTemplate.queryForObject(
                "SELECT COALESCE(host(inet_server_addr()), 'local')", String.class);

        assertThat(baseActual)
                .as("Las pruebas solo pueden ejecutarse contra la base local '%s'", BASE_AUTORIZADA)
                .isEqualTo(BASE_AUTORIZADA);

        assertThat(hostActual)
                .as("Las pruebas solo pueden ejecutarse contra PostgreSQL local")
                .isIn("local", "127.0.0.1", "::1");
    }
}
