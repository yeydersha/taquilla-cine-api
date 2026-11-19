package com.taquillacine.api;

import com.taquillacine.api.dto.PeliculaRequest;
import com.taquillacine.api.dto.PeliculaResponse;
import com.taquillacine.api.model.Pelicula;
import com.taquillacine.api.support.PruebaIntegracionBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PeliculaControllerIntegrationTest extends PruebaIntegracionBase {

    private PeliculaResponse crearPeliculaPorApi(PeliculaRequest request) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/peliculas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        PeliculaResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(), PeliculaResponse.class);
        registrarPelicula(response.getId());
        return response;
    }

    @Test
    @DisplayName("1. Listar películas devuelve un arreglo JSON")
    void listarPeliculasDevuelveArreglo() throws Exception {
        mockMvc.perform(get("/api/peliculas"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("2. Registrar una película válida")
    void registrarPeliculaValida() throws Exception {
        PeliculaRequest request = new PeliculaRequest(
                "Interestelar",
                "Ciencia ficción",
                169,
                "PG-13"
        );

        MvcResult result = mockMvc.perform(post("/api/peliculas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.titulo").value("Interestelar"))
                .andExpect(jsonPath("$.genero").value("Ciencia ficción"))
                .andExpect(jsonPath("$.duracionMin").value(169))
                .andExpect(jsonPath("$.clasificacion").value("PG-13"))
                .andReturn();

        PeliculaResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(), PeliculaResponse.class);
        registrarPelicula(response.getId());

        assertThat(response.getId()).isNotNull();
    }

    @Test
    @DisplayName("3. Verificar almacenamiento real en PostgreSQL")
    void verificarAlmacenamientoReal() throws Exception {
        PeliculaResponse creada = crearPeliculaPorApi(new PeliculaRequest(
                "Interestelar", "Ciencia ficción", 169, "PG-13"));

        Map<String, Object> fila = jdbcTemplate.queryForMap(
                "SELECT titulo, genero, duracion_min, clasificacion FROM peliculas WHERE id = ?",
                creada.getId());

        assertThat(fila.get("titulo")).isEqualTo("Interestelar");
        assertThat(fila.get("genero")).isEqualTo("Ciencia ficción");
        assertThat(((Number) fila.get("duracion_min")).intValue()).isEqualTo(169);
        assertThat(fila.get("clasificacion")).isEqualTo("PG-13");
    }

    @Test
    @DisplayName("4. Consultar una película existente")
    void consultarPeliculaExistente() throws Exception {
        Pelicula pelicula = crearPelicula("Interestelar", "Ciencia ficción", 169, "PG-13");

        mockMvc.perform(get("/api/peliculas/{id}", pelicula.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(pelicula.getId()))
                .andExpect(jsonPath("$.titulo").value("Interestelar"));
    }

    @Test
    @DisplayName("5. Listar varias películas")
    void listarVariasPeliculas() throws Exception {
        long cantidadInicial = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM peliculas", Long.class);

        Pelicula primero = crearPelicula("El Padrino", "Drama", 175, "R");
        Pelicula segundo = crearPelicula("Matrix", "Acción", 136, "R");

        MvcResult result = mockMvc.perform(get("/api/peliculas"))
                .andExpect(status().isOk())
                .andReturn();

        PeliculaResponse[] peliculas = objectMapper.readValue(
                result.getResponse().getContentAsString(), PeliculaResponse[].class);

        assertThat(peliculas.length).isEqualTo((int) cantidadInicial + 2);
        assertThat(Arrays.stream(peliculas).map(p -> p.getId()))
                .contains(primero.getId(), segundo.getId());
    }

    @Test
    @DisplayName("6. Actualizar una película existente")
    void actualizarPeliculaExistente() throws Exception {
        Pelicula pelicula = crearPelicula("Interestelar", "Ciencia ficción", 169, "PG-13");

        PeliculaRequest request = new PeliculaRequest(
                "Interestelar (Edición Extendida)",
                "Ciencia ficción",
                175,
                "PG-13"
        );

        mockMvc.perform(put("/api/peliculas/{id}", pelicula.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(pelicula.getId()))
                .andExpect(jsonPath("$.titulo").value("Interestelar (Edición Extendida)"))
                .andExpect(jsonPath("$.duracionMin").value(175));
    }

    @Test
    @DisplayName("7. Verificar persistencia de la actualización")
    void verificarPersistenciaActualizacion() throws Exception {
        Pelicula pelicula = crearPelicula("Interestelar", "Ciencia ficción", 169, "PG-13");

        PeliculaRequest request = new PeliculaRequest(
                "Interestelar (Edición Extendida)", "Ciencia ficción", 175, "PG-13");

        mockMvc.perform(put("/api/peliculas/{id}", pelicula.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        Pelicula persistida = peliculaRepository.findById(pelicula.getId()).orElse(null);
        assertThat(persistida).isNotNull();
        assertThat(persistida.getTitulo()).isEqualTo("Interestelar (Edición Extendida)");
        assertThat(persistida.getDuracionMin()).isEqualTo(175);
    }

    @Test
    @DisplayName("8. Eliminar una película sin funciones asociadas")
    void eliminarPeliculaSinFunciones() throws Exception {
        Pelicula pelicula = crearPelicula("Para Eliminar", "Comedia", 90, "PG");

        mockMvc.perform(delete("/api/peliculas/{id}", pelicula.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/peliculas/{id}", pelicula.getId()))
                .andExpect(status().isNotFound());

        assertThat(peliculaRepository.findById(pelicula.getId())).isEmpty();
    }

    @Test
    @DisplayName("9. Consultar un ID inexistente")
    void consultarIdInexistente() throws Exception {
        mockMvc.perform(get("/api/peliculas/{id}", 999999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Recurso no encontrado"));
    }

    @Test
    @DisplayName("10. Actualizar un ID inexistente")
    void actualizarIdInexistente() throws Exception {
        PeliculaRequest request = new PeliculaRequest("Inexistente", "Drama", 100, "PG");

        mockMvc.perform(put("/api/peliculas/{id}", 999999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("11. Eliminar un ID inexistente")
    void eliminarIdInexistente() throws Exception {
        mockMvc.perform(delete("/api/peliculas/{id}", 999999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("12. Registrar una película sin título")
    void registrarSinTitulo() throws Exception {
        PeliculaRequest request = new PeliculaRequest(null, "Drama", 100, "PG");

        mockMvc.perform(post("/api/peliculas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errores.titulo").exists());
    }

    @Test
    @DisplayName("13. Registrar una película con título vacío")
    void registrarTituloVacio() throws Exception {
        PeliculaRequest request = new PeliculaRequest("", "Drama", 100, "PG");

        mockMvc.perform(post("/api/peliculas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.titulo").exists());
    }

    @Test
    @DisplayName("14. Registrar un título superior a 150 caracteres")
    void registrarTituloMuyLargo() throws Exception {
        PeliculaRequest request = new PeliculaRequest("a".repeat(151), "Drama", 100, "PG");

        mockMvc.perform(post("/api/peliculas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.titulo").exists());
    }

    @Test
    @DisplayName("15. Registrar una duración igual a cero")
    void registrarDuracionCero() throws Exception {
        PeliculaRequest request = new PeliculaRequest("Test", "Drama", 0, "PG");

        mockMvc.perform(post("/api/peliculas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.duracionMin").exists());
    }

    @Test
    @DisplayName("16. Registrar una duración negativa")
    void registrarDuracionNegativa() throws Exception {
        PeliculaRequest request = new PeliculaRequest("Test", "Drama", -10, "PG");

        mockMvc.perform(post("/api/peliculas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.duracionMin").exists());
    }

    @Test
    @DisplayName("17. Registrar una película sin género")
    void registrarSinGenero() throws Exception {
        PeliculaRequest request = new PeliculaRequest("Test", null, 100, "PG");

        mockMvc.perform(post("/api/peliculas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.genero").exists());
    }

    @Test
    @DisplayName("18. Registrar una película sin clasificación")
    void registrarSinClasificacion() throws Exception {
        PeliculaRequest request = new PeliculaRequest("Test", "Drama", 100, null);

        mockMvc.perform(post("/api/peliculas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.clasificacion").exists());
    }

    @Test
    @DisplayName("19. Enviar un JSON inválido")
    void enviarJsonInvalido() throws Exception {
        String jsonInvalido = "{ \"titulo\": \"Test\", \"genero\": \"Drama\", \"duracionMin\": 100, }";

        mockMvc.perform(post("/api/peliculas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonInvalido))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("JSON inválido"));
    }

    @Test
    @DisplayName("20. Eliminar una película con funciones asociadas (conflicto 409)")
    void eliminarPeliculaConFunciones() throws Exception {
        Pelicula pelicula = crearPelicula("Con Funciones", "Drama", 120, "PG-13");
        insertarFuncion(pelicula.getId(), LocalDate.of(2026, 12, 1), LocalTime.of(18, 0),
                "Sala 1", new BigDecimal("50.00"), 100, 100);

        mockMvc.perform(delete("/api/peliculas/{id}", pelicula.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflicto de integridad"));
    }

    @Test
    @DisplayName("21. Verificar que la película y sus funciones permanecen intactas después del conflicto")
    void verificarIntegridadDespuesConflicto() throws Exception {
        Pelicula pelicula = crearPelicula("Con Funciones", "Drama", 120, "PG-13");
        Long funcionId = insertarFuncion(pelicula.getId(), LocalDate.of(2026, 12, 1),
                LocalTime.of(18, 0), "Sala 1", new BigDecimal("50.00"), 100, 100);

        mockMvc.perform(delete("/api/peliculas/{id}", pelicula.getId()))
                .andExpect(status().isConflict());

        Pelicula persistida = peliculaRepository.findById(pelicula.getId()).orElse(null);
        assertThat(persistida).isNotNull();
        assertThat(persistida.getTitulo()).isEqualTo("Con Funciones");

        Integer funciones = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM funciones WHERE id = ? AND pelicula_id = ?",
                Integer.class, funcionId, pelicula.getId());
        assertThat(funciones).isEqualTo(1);
    }

    @Test
    @DisplayName("22. Validar longitud máxima de género (80)")
    void validarLongitudMaximaGenero() throws Exception {
        PeliculaRequest request = new PeliculaRequest("Test", "a".repeat(81), 100, "PG");

        mockMvc.perform(post("/api/peliculas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.genero").exists());
    }

    @Test
    @DisplayName("23. Validar longitud máxima de clasificación (20)")
    void validarLongitudMaximaClasificacion() throws Exception {
        PeliculaRequest request = new PeliculaRequest("Test", "Drama", 100, "a".repeat(21));

        mockMvc.perform(post("/api/peliculas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.clasificacion").exists());
    }

    @Test
    @DisplayName("24. Validar que duracionMin es obligatorio (null)")
    void validarDuracionMinNull() throws Exception {
        PeliculaRequest request = new PeliculaRequest("Test", "Drama", null, "PG");

        mockMvc.perform(post("/api/peliculas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.duracionMin").exists());
    }

    @Test
    @DisplayName("25. GET /api/peliculas devuelve lista ordenada por ID ascendente")
    void verificarOrdenamientoPorId() throws Exception {
        crearPelicula("Orden A", "Drama", 100, "PG");
        crearPelicula("Orden B", "Drama", 100, "PG");

        List<Pelicula> peliculas = peliculaRepository.findAll();
        assertThat(peliculas).isNotEmpty();

        for (int i = 1; i < peliculas.size(); i++) {
            assertThat(peliculas.get(i).getId()).isGreaterThan(peliculas.get(i - 1).getId());
        }
    }
}
