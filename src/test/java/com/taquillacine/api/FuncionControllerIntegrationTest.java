package com.taquillacine.api;

import com.taquillacine.api.dto.FuncionRequest;
import com.taquillacine.api.dto.FuncionResponse;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FuncionControllerIntegrationTest extends PruebaIntegracionBase {

    private static final LocalDate FECHA = LocalDate.of(2026, 12, 1);
    private static final LocalTime HORA = LocalTime.of(18, 30);
    private static final BigDecimal PRECIO = new BigDecimal("15000.00");

    private FuncionRequest solicitudValida(Long peliculaId) {
        return new FuncionRequest(peliculaId, FECHA, HORA, "Sala 1", PRECIO, 100);
    }

    private FuncionResponse crearFuncionPorApi(FuncionRequest request) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/funciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        FuncionResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(), FuncionResponse.class);
        registrarFuncion(response.getId());
        return response;
    }

    private void simularVentas(Long funcionId, int cantidad) {
        jdbcTemplate.update(
                "UPDATE funciones SET cupos_disponibles = cupos_disponibles - ? WHERE id = ?",
                cantidad, funcionId);
        insertarCompra(funcionId, cantidad, PRECIO.multiply(BigDecimal.valueOf(cantidad)));
    }

    @Test
    @DisplayName("1. Listar funciones devuelve el arreglo actual")
    void listarFunciones() throws Exception {
        long cantidad = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM funciones", Long.class);

        MvcResult result = mockMvc.perform(get("/api/funciones"))
                .andExpect(status().isOk())
                .andReturn();

        FuncionResponse[] funciones = objectMapper.readValue(
                result.getResponse().getContentAsString(), FuncionResponse[].class);

        assertThat(funciones.length).isEqualTo((int) cantidad);
    }

    @Test
    @DisplayName("2. Crear una función para una película existente")
    void crearFuncionParaPeliculaExistente() throws Exception {
        Pelicula pelicula = crearPelicula("Interestelar", "Ciencia ficción", 169, "PG-13");

        MvcResult resultado = mockMvc.perform(post("/api/funciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(solicitudValida(pelicula.getId()))))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.peliculaId").value(pelicula.getId()))
                .andExpect(jsonPath("$.fecha").value("2026-12-01"))
                .andExpect(jsonPath("$.hora").value("18:30:00"))
                .andExpect(jsonPath("$.sala").value("Sala 1"))
                .andExpect(jsonPath("$.precio").value(15000.00))
                .andExpect(jsonPath("$.cuposTotales").value(100))
                .andReturn();

        FuncionResponse creada = objectMapper.readValue(
                resultado.getResponse().getContentAsString(), FuncionResponse.class);
        registrarFuncion(creada.getId());
    }

    @Test
    @DisplayName("3. Al crear, cuposDisponibles es igual a cuposTotales")
    void cuposDisponiblesIgualesAlCrear() throws Exception {
        Pelicula pelicula = crearPelicula("Interestelar", "Ciencia ficción", 169, "PG-13");

        FuncionResponse creada = crearFuncionPorApi(solicitudValida(pelicula.getId()));

        assertThat(creada.getCuposTotales()).isEqualTo(100);
        assertThat(creada.getCuposDisponibles()).isEqualTo(100);

        Integer disponibles = jdbcTemplate.queryForObject(
                "SELECT cupos_disponibles FROM funciones WHERE id = ?", Integer.class, creada.getId());
        assertThat(disponibles).isEqualTo(100);
    }

    @Test
    @DisplayName("4. Verificar persistencia real en PostgreSQL")
    void verificarPersistenciaEnPostgresql() throws Exception {
        Pelicula pelicula = crearPelicula("Interestelar", "Ciencia ficción", 169, "PG-13");
        FuncionResponse creada = crearFuncionPorApi(solicitudValida(pelicula.getId()));

        Map<String, Object> fila = jdbcTemplate.queryForMap(
                "SELECT pelicula_id, fecha, hora, sala, precio, cupos_totales, cupos_disponibles "
                        + "FROM funciones WHERE id = ?",
                creada.getId());

        assertThat(((Number) fila.get("pelicula_id")).longValue()).isEqualTo(pelicula.getId());
        assertThat(((java.sql.Date) fila.get("fecha")).toLocalDate()).isEqualTo(FECHA);
        assertThat(((java.sql.Time) fila.get("hora")).toLocalTime()).isEqualTo(HORA);
        assertThat(fila.get("sala")).isEqualTo("Sala 1");
        assertThat((BigDecimal) fila.get("precio")).isEqualByComparingTo("15000.00");
        assertThat(((Number) fila.get("cupos_totales")).intValue()).isEqualTo(100);
        assertThat(((Number) fila.get("cupos_disponibles")).intValue()).isEqualTo(100);
    }

    @Test
    @DisplayName("5. Consultar una función por ID")
    void consultarFuncionPorId() throws Exception {
        Pelicula pelicula = crearPelicula("Interestelar", "Ciencia ficción", 169, "PG-13");
        FuncionResponse creada = crearFuncionPorApi(solicitudValida(pelicula.getId()));

        mockMvc.perform(get("/api/funciones/{id}", creada.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(creada.getId()))
                .andExpect(jsonPath("$.peliculaId").value(pelicula.getId()))
                .andExpect(jsonPath("$.tituloPelicula").value("Interestelar"))
                .andExpect(jsonPath("$.sala").value("Sala 1"));
    }

    @Test
    @DisplayName("6. Listar múltiples funciones con orden estable por fecha, hora e ID")
    void listarMultiplesFunciones() throws Exception {
        Pelicula pelicula = crearPelicula("Interestelar", "Ciencia ficción", 169, "PG-13");

        FuncionResponse tercera = crearFuncionPorApi(new FuncionRequest(
                pelicula.getId(), LocalDate.of(2027, 1, 2), LocalTime.of(10, 0),
                "Sala 3", PRECIO, 80));
        FuncionResponse segunda = crearFuncionPorApi(new FuncionRequest(
                pelicula.getId(), LocalDate.of(2027, 1, 1), LocalTime.of(18, 0),
                "Sala 2", PRECIO, 80));
        FuncionResponse primera = crearFuncionPorApi(new FuncionRequest(
                pelicula.getId(), LocalDate.of(2027, 1, 1), LocalTime.of(9, 0),
                "Sala 1", PRECIO, 80));

        MvcResult result = mockMvc.perform(get("/api/funciones"))
                .andExpect(status().isOk())
                .andReturn();

        FuncionResponse[] funciones = objectMapper.readValue(
                result.getResponse().getContentAsString(), FuncionResponse[].class);

        List<Long> idsEsperados = List.of(primera.getId(), segunda.getId(), tercera.getId());
        List<Long> idsObtenidos = Arrays.stream(funciones)
                .map(f -> f.getId())
                .filter(idsEsperados::contains)
                .toList();

        assertThat(idsObtenidos).containsExactlyElementsOf(idsEsperados);
    }

    @Test
    @DisplayName("7. La respuesta incluye tituloPelicula mediante JOIN")
    void respuestaIncluyeTituloPelicula() throws Exception {
        Pelicula pelicula = crearPelicula("Interestelar", "Ciencia ficción", 169, "PG-13");

        MvcResult resultado = mockMvc.perform(post("/api/funciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(solicitudValida(pelicula.getId()))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tituloPelicula").value("Interestelar"))
                .andReturn();

        FuncionResponse creada = objectMapper.readValue(
                resultado.getResponse().getContentAsString(), FuncionResponse.class);
        registrarFuncion(creada.getId());
    }

    @Test
    @DisplayName("8. Actualizar una función existente")
    void actualizarFuncionExistente() throws Exception {
        Pelicula pelicula = crearPelicula("Interestelar", "Ciencia ficción", 169, "PG-13");
        FuncionResponse creada = crearFuncionPorApi(solicitudValida(pelicula.getId()));

        FuncionRequest cambios = new FuncionRequest(
                pelicula.getId(), LocalDate.of(2027, 2, 14), LocalTime.of(20, 45),
                "Sala Premium", new BigDecimal("18000.50"), 120);

        mockMvc.perform(put("/api/funciones/{id}", creada.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cambios)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(creada.getId()))
                .andExpect(jsonPath("$.fecha").value("2027-02-14"))
                .andExpect(jsonPath("$.hora").value("20:45:00"))
                .andExpect(jsonPath("$.sala").value("Sala Premium"))
                .andExpect(jsonPath("$.precio").value(18000.50))
                .andExpect(jsonPath("$.cuposTotales").value(120))
                .andExpect(jsonPath("$.cuposDisponibles").value(120));
    }

    @Test
    @DisplayName("9. Verificar persistencia de una actualización")
    void verificarPersistenciaActualizacion() throws Exception {
        Pelicula pelicula = crearPelicula("Interestelar", "Ciencia ficción", 169, "PG-13");
        FuncionResponse creada = crearFuncionPorApi(solicitudValida(pelicula.getId()));

        FuncionRequest cambios = new FuncionRequest(
                pelicula.getId(), LocalDate.of(2027, 2, 14), LocalTime.of(20, 45),
                "Sala Premium", new BigDecimal("18000.50"), 120);

        mockMvc.perform(put("/api/funciones/{id}", creada.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cambios)))
                .andExpect(status().isOk());

        Map<String, Object> fila = jdbcTemplate.queryForMap(
                "SELECT sala, precio, cupos_totales FROM funciones WHERE id = ?", creada.getId());

        assertThat(fila.get("sala")).isEqualTo("Sala Premium");
        assertThat((BigDecimal) fila.get("precio")).isEqualByComparingTo("18000.50");
        assertThat(((Number) fila.get("cupos_totales")).intValue()).isEqualTo(120);
    }

    @Test
    @DisplayName("10. Eliminar una función sin compras asociadas")
    void eliminarFuncionSinCompras() throws Exception {
        Pelicula pelicula = crearPelicula("Interestelar", "Ciencia ficción", 169, "PG-13");
        FuncionResponse creada = crearFuncionPorApi(solicitudValida(pelicula.getId()));

        mockMvc.perform(delete("/api/funciones/{id}", creada.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/funciones/{id}", creada.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("11. Aumentar la capacidad conservando las ventas")
    void aumentarCapacidad() throws Exception {
        Pelicula pelicula = crearPelicula("Interestelar", "Ciencia ficción", 169, "PG-13");
        FuncionResponse creada = crearFuncionPorApi(solicitudValida(pelicula.getId()));
        simularVentas(creada.getId(), 10);

        FuncionRequest cambios = new FuncionRequest(
                pelicula.getId(), FECHA, HORA, "Sala 1", PRECIO, 150);

        mockMvc.perform(put("/api/funciones/{id}", creada.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cambios)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cuposTotales").value(150))
                .andExpect(jsonPath("$.cuposDisponibles").value(140));
    }

    @Test
    @DisplayName("12. Reducir la capacidad respetando los cupos vendidos")
    void reducirCapacidadRespetandoVentas() throws Exception {
        Pelicula pelicula = crearPelicula("Interestelar", "Ciencia ficción", 169, "PG-13");
        FuncionResponse creada = crearFuncionPorApi(solicitudValida(pelicula.getId()));
        simularVentas(creada.getId(), 10);

        FuncionRequest cambios = new FuncionRequest(
                pelicula.getId(), FECHA, HORA, "Sala 1", PRECIO, 60);

        mockMvc.perform(put("/api/funciones/{id}", creada.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cambios)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cuposTotales").value(60))
                .andExpect(jsonPath("$.cuposDisponibles").value(50));
    }

    @Test
    @DisplayName("13. Crear una función con una película inexistente")
    void crearConPeliculaInexistente() throws Exception {
        mockMvc.perform(post("/api/funciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(solicitudValida(999999L))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("14. Consultar una función inexistente")
    void consultarFuncionInexistente() throws Exception {
        mockMvc.perform(get("/api/funciones/{id}", 999999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("15. Actualizar una función inexistente")
    void actualizarFuncionInexistente() throws Exception {
        Pelicula pelicula = crearPelicula("Interestelar", "Ciencia ficción", 169, "PG-13");

        mockMvc.perform(put("/api/funciones/{id}", 999999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(solicitudValida(pelicula.getId()))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("16. Eliminar una función inexistente")
    void eliminarFuncionInexistente() throws Exception {
        mockMvc.perform(delete("/api/funciones/{id}", 999999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("17. Crear una función sin peliculaId")
    void crearSinPeliculaId() throws Exception {
        FuncionRequest request = new FuncionRequest(null, FECHA, HORA, "Sala 1", PRECIO, 100);

        mockMvc.perform(post("/api/funciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.peliculaId").exists());
    }

    @Test
    @DisplayName("18. Crear una función sin fecha")
    void crearSinFecha() throws Exception {
        Pelicula pelicula = crearPelicula("Interestelar", "Ciencia ficción", 169, "PG-13");
        FuncionRequest request = new FuncionRequest(
                pelicula.getId(), null, HORA, "Sala 1", PRECIO, 100);

        mockMvc.perform(post("/api/funciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.fecha").exists());
    }

    @Test
    @DisplayName("19. Crear una función sin hora")
    void crearSinHora() throws Exception {
        Pelicula pelicula = crearPelicula("Interestelar", "Ciencia ficción", 169, "PG-13");
        FuncionRequest request = new FuncionRequest(
                pelicula.getId(), FECHA, null, "Sala 1", PRECIO, 100);

        mockMvc.perform(post("/api/funciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.hora").exists());
    }

    @Test
    @DisplayName("20. Crear una función sin sala")
    void crearSinSala() throws Exception {
        Pelicula pelicula = crearPelicula("Interestelar", "Ciencia ficción", 169, "PG-13");
        FuncionRequest request = new FuncionRequest(
                pelicula.getId(), FECHA, HORA, null, PRECIO, 100);

        mockMvc.perform(post("/api/funciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.sala").exists());
    }

    @Test
    @DisplayName("21. Crear una función con sala vacía")
    void crearConSalaVacia() throws Exception {
        Pelicula pelicula = crearPelicula("Interestelar", "Ciencia ficción", 169, "PG-13");
        FuncionRequest request = new FuncionRequest(
                pelicula.getId(), FECHA, HORA, "   ", PRECIO, 100);

        mockMvc.perform(post("/api/funciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.sala").exists());
    }

    @Test
    @DisplayName("22. Crear una función con sala superior a 50 caracteres")
    void crearConSalaMuyLarga() throws Exception {
        Pelicula pelicula = crearPelicula("Interestelar", "Ciencia ficción", 169, "PG-13");
        FuncionRequest request = new FuncionRequest(
                pelicula.getId(), FECHA, HORA, "a".repeat(51), PRECIO, 100);

        mockMvc.perform(post("/api/funciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.sala").exists());
    }

    @Test
    @DisplayName("23. Crear una función con precio cero")
    void crearConPrecioCero() throws Exception {
        Pelicula pelicula = crearPelicula("Interestelar", "Ciencia ficción", 169, "PG-13");
        FuncionRequest request = new FuncionRequest(
                pelicula.getId(), FECHA, HORA, "Sala 1", BigDecimal.ZERO, 100);

        mockMvc.perform(post("/api/funciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.precio").exists());
    }

    @Test
    @DisplayName("24. Crear una función con precio negativo")
    void crearConPrecioNegativo() throws Exception {
        Pelicula pelicula = crearPelicula("Interestelar", "Ciencia ficción", 169, "PG-13");
        FuncionRequest request = new FuncionRequest(
                pelicula.getId(), FECHA, HORA, "Sala 1", new BigDecimal("-10.00"), 100);

        mockMvc.perform(post("/api/funciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.precio").exists());
    }

    @Test
    @DisplayName("25. Crear una función con precio nulo")
    void crearConPrecioNulo() throws Exception {
        Pelicula pelicula = crearPelicula("Interestelar", "Ciencia ficción", 169, "PG-13");
        FuncionRequest request = new FuncionRequest(
                pelicula.getId(), FECHA, HORA, "Sala 1", null, 100);

        mockMvc.perform(post("/api/funciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.precio").exists());
    }

    @Test
    @DisplayName("26. Crear una función con precisión monetaria inválida")
    void crearConPrecisionMonetariaInvalida() throws Exception {
        Pelicula pelicula = crearPelicula("Interestelar", "Ciencia ficción", 169, "PG-13");
        FuncionRequest request = new FuncionRequest(
                pelicula.getId(), FECHA, HORA, "Sala 1", new BigDecimal("15000.123"), 100);

        mockMvc.perform(post("/api/funciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.precio").exists());
    }

    @Test
    @DisplayName("27. Crear una función con cero cupos")
    void crearConCeroCupos() throws Exception {
        Pelicula pelicula = crearPelicula("Interestelar", "Ciencia ficción", 169, "PG-13");
        FuncionRequest request = new FuncionRequest(
                pelicula.getId(), FECHA, HORA, "Sala 1", PRECIO, 0);

        mockMvc.perform(post("/api/funciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.cuposTotales").exists());
    }

    @Test
    @DisplayName("28. Crear una función con cupos negativos")
    void crearConCuposNegativos() throws Exception {
        Pelicula pelicula = crearPelicula("Interestelar", "Ciencia ficción", 169, "PG-13");
        FuncionRequest request = new FuncionRequest(
                pelicula.getId(), FECHA, HORA, "Sala 1", PRECIO, -5);

        mockMvc.perform(post("/api/funciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.cuposTotales").exists());
    }

    @Test
    @DisplayName("29. Crear una función sin cuposTotales")
    void crearSinCuposTotales() throws Exception {
        Pelicula pelicula = crearPelicula("Interestelar", "Ciencia ficción", 169, "PG-13");
        FuncionRequest request = new FuncionRequest(
                pelicula.getId(), FECHA, HORA, "Sala 1", PRECIO, null);

        mockMvc.perform(post("/api/funciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.cuposTotales").exists());
    }

    @Test
    @DisplayName("30. Enviar JSON malformado")
    void enviarJsonMalformado() throws Exception {
        String jsonInvalido = "{ \"peliculaId\": 1, \"fecha\": \"2026-12-01\", }";

        mockMvc.perform(post("/api/funciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonInvalido))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("JSON inválido"));
    }

    @Test
    @DisplayName("31. Reducir los cupos totales por debajo de los cupos vendidos")
    void reducirCuposPorDebajoDeVendidos() throws Exception {
        Pelicula pelicula = crearPelicula("Interestelar", "Ciencia ficción", 169, "PG-13");
        FuncionResponse creada = crearFuncionPorApi(solicitudValida(pelicula.getId()));
        simularVentas(creada.getId(), 10);

        FuncionRequest cambios = new FuncionRequest(
                pelicula.getId(), FECHA, HORA, "Sala 1", PRECIO, 9);

        mockMvc.perform(put("/api/funciones/{id}", creada.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cambios)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));

        Integer totales = jdbcTemplate.queryForObject(
                "SELECT cupos_totales FROM funciones WHERE id = ?", Integer.class, creada.getId());
        assertThat(totales).isEqualTo(100);
    }

    @Test
    @DisplayName("32. Eliminar una función con compras asociadas")
    void eliminarFuncionConCompras() throws Exception {
        Pelicula pelicula = crearPelicula("Interestelar", "Ciencia ficción", 169, "PG-13");
        FuncionResponse creada = crearFuncionPorApi(solicitudValida(pelicula.getId()));
        simularVentas(creada.getId(), 2);

        mockMvc.perform(delete("/api/funciones/{id}", creada.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("33. Verificar que la función y la compra permanecen intactas tras el conflicto")
    void verificarFuncionYCompraIntactas() throws Exception {
        Pelicula pelicula = crearPelicula("Interestelar", "Ciencia ficción", 169, "PG-13");
        FuncionResponse creada = crearFuncionPorApi(solicitudValida(pelicula.getId()));
        simularVentas(creada.getId(), 3);

        mockMvc.perform(delete("/api/funciones/{id}", creada.getId()))
                .andExpect(status().isConflict());

        Integer funciones = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM funciones WHERE id = ?", Integer.class, creada.getId());
        Integer compras = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM compras WHERE funcion_id = ?", Integer.class, creada.getId());

        assertThat(funciones).isEqualTo(1);
        assertThat(compras).isEqualTo(1);

        Integer disponibles = jdbcTemplate.queryForObject(
                "SELECT cupos_disponibles FROM funciones WHERE id = ?", Integer.class, creada.getId());
        assertThat(disponibles).isEqualTo(97);
    }

    @Test
    @DisplayName("34. Actualizar una función utilizando una película inexistente")
    void actualizarConPeliculaInexistente() throws Exception {
        Pelicula pelicula = crearPelicula("Interestelar", "Ciencia ficción", 169, "PG-13");
        FuncionResponse creada = crearFuncionPorApi(solicitudValida(pelicula.getId()));

        mockMvc.perform(put("/api/funciones/{id}", creada.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(solicitudValida(999999L))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("35. Intentar modificar la película de una función con compras registradas")
    void modificarPeliculaConCompras() throws Exception {
        Pelicula original = crearPelicula("Interestelar", "Ciencia ficción", 169, "PG-13");
        Pelicula nueva = crearPelicula("El Padrino", "Drama", 175, "R");

        FuncionResponse creada = crearFuncionPorApi(solicitudValida(original.getId()));
        simularVentas(creada.getId(), 1);

        mockMvc.perform(put("/api/funciones/{id}", creada.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(solicitudValida(nueva.getId()))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));

        Long peliculaPersistida = jdbcTemplate.queryForObject(
                "SELECT pelicula_id FROM funciones WHERE id = ?", Long.class, creada.getId());
        assertThat(peliculaPersistida).isEqualTo(original.getId());
    }

    @Test
    @DisplayName("36. El cliente no puede manipular cuposDisponibles")
    void clienteNoPuedeManipularCuposDisponibles() throws Exception {
        Pelicula pelicula = crearPelicula("Interestelar", "Ciencia ficción", 169, "PG-13");

        String cuerpo = """
                {
                  "peliculaId": %d,
                  "fecha": "2026-12-01",
                  "hora": "18:30:00",
                  "sala": "Sala 1",
                  "precio": 15000.00,
                  "cuposTotales": 100,
                  "cuposDisponibles": 1
                }
                """.formatted(pelicula.getId());

        MvcResult result = mockMvc.perform(post("/api/funciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cuposTotales").value(100))
                .andExpect(jsonPath("$.cuposDisponibles").value(100))
                .andReturn();

        FuncionResponse creada = objectMapper.readValue(
                result.getResponse().getContentAsString(), FuncionResponse.class);
        registrarFuncion(creada.getId());

        Integer disponibles = jdbcTemplate.queryForObject(
                "SELECT cupos_disponibles FROM funciones WHERE id = ?", Integer.class, creada.getId());
        assertThat(disponibles).isEqualTo(100);
    }
}
