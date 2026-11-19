package com.taquillacine.api;

import com.taquillacine.api.dto.PeliculaRequest;
import com.taquillacine.api.dto.PeliculaResponse;
import com.taquillacine.api.model.Pelicula;
import com.taquillacine.api.support.PruebaIntegracionBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas adicionales de la fase 2.6: CORS, formato de errores, rutas y
 * conservación del contrato. No repiten los casos ya cubiertos por las pruebas
 * de cada módulo.
 */
class AuditoriaApiIntegrationTest extends PruebaIntegracionBase {

    private static final String ORIGEN_ANGULAR = "http://localhost:4200";
    private static final String ORIGEN_NO_AUTORIZADO = "http://origen-no-autorizado.example";

    private PeliculaRequest solicitudPelicula(String titulo) {
        return new PeliculaRequest(titulo, "Ciencia ficción", 169, "PG-13");
    }

    @Test
    @DisplayName("1. CORS: GET desde el origen de Angular recibe el encabezado de autorización")
    void corsGetDesdeOrigenPermitido() throws Exception {
        mockMvc.perform(get("/api/peliculas").header(HttpHeaders.ORIGIN, ORIGEN_ANGULAR))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ORIGEN_ANGULAR))
                .andExpect(header().string(
                        HttpHeaders.VARY, containsString(HttpHeaders.ORIGIN)));
    }

    @Test
    @DisplayName("2. CORS: preflight OPTIONS permitido para JSON")
    void corsPreflightPermitido() throws Exception {
        mockMvc.perform(options("/api/peliculas")
                        .header(HttpHeaders.ORIGIN, ORIGEN_ANGULAR)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ORIGEN_ANGULAR))
                .andExpect(header().string(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS,
                        allOf(
                                containsString("GET"),
                                containsString("POST"),
                                containsString("PUT"),
                                containsString("DELETE"),
                                containsString("OPTIONS"))))
                .andExpect(header().string(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS,
                        containsString("Content-Type")));
    }

    @Test
    @DisplayName("3. CORS: preflight desde un origen no autorizado es rechazado")
    void corsPreflightOrigenNoAutorizado() throws Exception {
        mockMvc.perform(options("/api/peliculas")
                        .header(HttpHeaders.ORIGIN, ORIGEN_NO_AUTORIZADO)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    @DisplayName("4. CORS: una solicitud real de origen no autorizado no crea datos")
    void corsSolicitudRealOrigenNoAutorizadoNoCreaDatos() throws Exception {
        long antes = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM peliculas", Long.class);

        mockMvc.perform(post("/api/peliculas")
                        .header(HttpHeaders.ORIGIN, ORIGEN_NO_AUTORIZADO)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                solicitudPelicula("Origen no autorizado"))))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));

        long despues = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM peliculas", Long.class);
        assertThat(despues).isEqualTo(antes);
    }

    @Test
    @DisplayName("5. CORS: POST autorizado expone Location a Angular")
    void corsPostExponeLocation() throws Exception {
        MvcResult resultado = mockMvc.perform(post("/api/peliculas")
                        .header(HttpHeaders.ORIGIN, ORIGEN_ANGULAR)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                solicitudPelicula("Location Angular"))))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ORIGEN_ANGULAR))
                .andExpect(header().exists(HttpHeaders.LOCATION))
                .andExpect(header().string(
                        HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS,
                        containsString(HttpHeaders.LOCATION)))
                .andReturn();

        PeliculaResponse creada = objectMapper.readValue(
                resultado.getResponse().getContentAsString(), PeliculaResponse.class);
        registrarPelicula(creada.getId());

        assertThat(resultado.getResponse().getHeader(HttpHeaders.LOCATION))
                .isEqualTo("/api/peliculas/" + creada.getId());
    }

    @Test
    @DisplayName("6. CORS: PUT con JSON desde el origen autorizado")
    void corsPutDesdeOrigenPermitido() throws Exception {
        Pelicula pelicula = crearPelicula("Antes de Actualizar", "Drama", 120, "PG-13");

        mockMvc.perform(put("/api/peliculas/{id}", pelicula.getId())
                        .header(HttpHeaders.ORIGIN, ORIGEN_ANGULAR)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                solicitudPelicula("Después de Actualizar"))))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ORIGEN_ANGULAR))
                .andExpect(jsonPath("$.titulo").value("Después de Actualizar"));
    }

    @Test
    @DisplayName("7. CORS: DELETE autorizado responde 204")
    void corsDeleteDesdeOrigenPermitido() throws Exception {
        Pelicula pelicula = crearPelicula("Para Eliminar", "Drama", 120, "PG-13");

        mockMvc.perform(delete("/api/peliculas/{id}", pelicula.getId())
                        .header(HttpHeaders.ORIGIN, ORIGEN_ANGULAR))
                .andExpect(status().isNoContent())
                .andExpect(header().string(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ORIGEN_ANGULAR));

        assertThat(peliculaRepository.findById(pelicula.getId())).isEmpty();
    }

    @Test
    @DisplayName("8. Los cuerpos exitosos se envían como application/json")
    void respuestasExitosasSonJson() throws Exception {
        MvcResult resultado = mockMvc.perform(post("/api/peliculas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                solicitudPelicula("Content Type"))))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").exists())
                .andReturn();

        PeliculaResponse creada = objectMapper.readValue(
                resultado.getResponse().getContentAsString(), PeliculaResponse.class);
        registrarPelicula(creada.getId());
    }

    @Test
    @DisplayName("9. El error 404 conserva el formato global de cinco campos")
    void formatoGlobalDeError() throws Exception {
        long idInexistente = 999_999_999L;

        mockMvc.perform(get("/api/peliculas/{id}", idInexistente))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.*", hasSize(5)))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Recurso no encontrado"))
                .andExpect(jsonPath("$.mensaje").isString())
                .andExpect(jsonPath("$.ruta").value("/api/peliculas/" + idInexistente))
                .andExpect(jsonPath("$.timestamp").isString());
    }

    @Test
    @DisplayName("10. Un parámetro de ruta con tipo inválido responde 400")
    void parametroDeRutaConTipoInvalido() throws Exception {
        mockMvc.perform(get("/api/funciones/{id}", "no-es-un-numero"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Parámetro inválido"))
                .andExpect(jsonPath("$.ruta").value("/api/funciones/no-es-un-numero"));
    }

    @Test
    @DisplayName("11. Una ruta inexistente responde 404 con formato JSON")
    void rutaInexistenteResponde404() throws Exception {
        mockMvc.perform(get("/api/ruta-inexistente"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.mensaje").value("La ruta solicitada no existe"));
    }

    @Test
    @DisplayName("12. Un método no permitido responde 405 con formato JSON")
    void metodoNoPermitidoResponde405() throws Exception {
        mockMvc.perform(post("/api/peliculas/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405))
                .andExpect(jsonPath("$.error").value("Método no permitido"));
    }

    @Test
    @DisplayName("13. Los errores no exponen SQL, trazas ni detalles internos")
    void erroresNoExponenInformacionSensible() throws Exception {
        Pelicula pelicula = crearPelicula("Con Funciones", "Drama", 120, "PG-13");
        insertarFuncion(pelicula.getId(), LocalDate.of(2026, 12, 1), LocalTime.of(18, 0),
                "Sala 1", new BigDecimal("15000.00"), 100, 100);

        String conflicto = mockMvc.perform(delete("/api/peliculas/{id}", pelicula.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflicto de integridad"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String validacion = mockMvc.perform(post("/api/peliculas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new PeliculaRequest("Test", "Drama", null, "PG"))))
                .andExpect(status().isBadRequest())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(conflicto)
                .contains("No se puede eliminar la película porque tiene funciones asociadas")
                .doesNotContain("fk_funciones_peliculas");

        for (String cuerpo : List.of(conflicto, validacion)) {
            assertThat(cuerpo)
                    .doesNotContain("org.postgresql")
                    .doesNotContain("PSQLException")
                    .doesNotContain("SELECT ")
                    .doesNotContain("INSERT INTO")
                    .doesNotContain("DELETE FROM")
                    .doesNotContain("jdbc:")
                    .doesNotContain("\"trace\"")
                    .doesNotContain("\"exception\"")
                    .doesNotContain(" at com.")
                    .doesNotContainIgnoringCase("password");
        }
    }

    @Test
    @DisplayName("14. Actuator health está disponible sin detalles internos")
    void healthDisponibleSinDetalles() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.parseMediaType("application/vnd.spring-boot.actuator.v3+json")))
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components").doesNotExist())
                .andExpect(jsonPath("$.details").doesNotExist());
    }

    @Test
    @DisplayName("15. El contrato de FunctionResponse conserva sus nueve campos")
    void contratoFuncionResponse() throws Exception {
        Pelicula pelicula = crearPelicula("Contrato Funcion", "Ciencia ficción", 169, "PG-13");
        Long funcionId = insertarFuncion(pelicula.getId(), LocalDate.of(2026, 12, 1),
                LocalTime.of(18, 30), "Sala 1", new BigDecimal("15000.00"), 100, 100);

        mockMvc.perform(get("/api/funciones/{id}", funcionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.*", hasSize(9)))
                .andExpect(jsonPath("$.id").value(funcionId))
                .andExpect(jsonPath("$.peliculaId").value(pelicula.getId()))
                .andExpect(jsonPath("$.tituloPelicula").value("Contrato Funcion"))
                .andExpect(jsonPath("$.fecha").value("2026-12-01"))
                .andExpect(jsonPath("$.hora").value("18:30:00"))
                .andExpect(jsonPath("$.sala").value("Sala 1"))
                .andExpect(jsonPath("$.precio").value(15000.00))
                .andExpect(jsonPath("$.cuposTotales").value(100))
                .andExpect(jsonPath("$.cuposDisponibles").value(100));
    }

    @Test
    @DisplayName("16. Un Content-Type no soportado responde 415 y no 500")
    void tipoDeContenidoNoSoportado() throws Exception {
        mockMvc.perform(post("/api/peliculas")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("texto plano"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.status").value(415))
                .andExpect(jsonPath("$.error").value("Tipo de contenido no soportado"));
    }
}
