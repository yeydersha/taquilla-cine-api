package com.taquillacine.api;

import com.taquillacine.api.model.Compra;
import com.taquillacine.api.model.Pelicula;
import com.taquillacine.api.repository.CompraRepository;
import com.taquillacine.api.support.PruebaIntegracionBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifica que la compra sea atómica.
 *
 * <p>El fallo se inyecta únicamente en el entorno de pruebas mediante una
 * subclase {@link Primary} de {@link CompraRepository} que registra la compra y
 * luego lanza una excepción, simulando un fallo entre el INSERT y el descuento
 * de cupos. El código de producción no contiene interruptores ni lógica de
 * fallo artificial.</p>
 */
class CompraAtomicidadIntegrationTest extends PruebaIntegracionBase {

    @TestConfiguration
    static class ConfiguracionFalloControlado {

        @Bean
        @Primary
        CompraRepository compraRepositoryConFallo(JdbcTemplate jdbcTemplate) {
            return new CompraRepository(jdbcTemplate) {
                @Override
                public Compra save(Compra compra) {
                    super.save(compra);
                    throw new IllegalStateException(
                            "Fallo simulado después de registrar la compra");
                }
            };
        }
    }

    private Long crearFuncion(int cupos) {
        Pelicula pelicula = crearPelicula("Interestelar", "Ciencia ficción", 169, "PG-13");
        return insertarFuncion(pelicula.getId(), LocalDate.of(2026, 12, 1),
                LocalTime.of(18, 30), "Sala 1", new BigDecimal("15000.00"), cupos, cupos);
    }

    @Test
    @DisplayName("Un fallo después del INSERT revierte la compra y no descuenta cupos")
    void falloDespuesDelInsertRevierteTodo() throws Exception {
        Long funcionId = crearFuncion(10);

        mockMvc.perform(post("/api/compras")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "funcionId": %d,
                                  "nombreCliente": "Carlos Pérez",
                                  "cantidadBoletos": 3
                                }
                                """.formatted(funcionId)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500));

        Integer compras = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM compras WHERE funcion_id = ?", Integer.class, funcionId);
        Integer disponibles = jdbcTemplate.queryForObject(
                "SELECT cupos_disponibles FROM funciones WHERE id = ?", Integer.class, funcionId);

        assertThat(compras).isZero();
        assertThat(disponibles).isEqualTo(10);
    }
}
