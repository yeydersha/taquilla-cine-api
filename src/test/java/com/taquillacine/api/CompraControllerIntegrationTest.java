package com.taquillacine.api;

import com.taquillacine.api.dto.CompraRequest;
import com.taquillacine.api.dto.CompraResponse;
import com.taquillacine.api.model.Pelicula;
import com.taquillacine.api.support.PruebaIntegracionBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CompraControllerIntegrationTest extends PruebaIntegracionBase {

    private static final LocalDate FECHA = LocalDate.of(2026, 12, 1);
    private static final LocalTime HORA = LocalTime.of(18, 30);
    private static final String PRECIO_ESTANDAR = "15000.00";

    private Long crearFuncionDisponible(String precio, int cupos) {
        Pelicula pelicula = crearPelicula("Interestelar", "Ciencia ficción", 169, "PG-13");
        return insertarFuncion(pelicula.getId(), FECHA, HORA, "Sala 1",
                new BigDecimal(precio), cupos, cupos);
    }

    private CompraRequest solicitud(Long funcionId, String nombreCliente, Integer cantidad) {
        return new CompraRequest(funcionId, nombreCliente, cantidad);
    }

    private CompraResponse crearCompraPorApi(Long funcionId, String nombreCliente, int cantidad)
            throws Exception {
        MvcResult resultado = mockMvc.perform(post("/api/compras")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                solicitud(funcionId, nombreCliente, cantidad))))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readValue(
                resultado.getResponse().getContentAsString(), CompraResponse.class);
    }

    private int cuposDisponibles(Long funcionId) {
        return jdbcTemplate.queryForObject(
                "SELECT cupos_disponibles FROM funciones WHERE id = ?", Integer.class, funcionId);
    }

    private int contarCompras(Long funcionId) {
        Integer total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM compras WHERE funcion_id = ?", Integer.class, funcionId);
        return total != null ? total : 0;
    }

    private int boletosVendidos(Long funcionId) {
        Integer total = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(cantidad_boletos), 0) FROM compras WHERE funcion_id = ?",
                Integer.class, funcionId);
        return total != null ? total : 0;
    }

    private Long insertarCompraConFecha(Long funcionId, String nombre, int cantidad,
                                        BigDecimal total, OffsetDateTime fecha) {
        String sql = """
                INSERT INTO compras
                    (funcion_id, nombre_cliente, cantidad_boletos, total, fecha_compra)
                VALUES (?, ?, ?, ?, ?)
                RETURNING id
                """;
        return jdbcTemplate.queryForObject(sql, Long.class, funcionId, nombre, cantidad, total, fecha);
    }

    @Test
    @DisplayName("1. Listar compras cuando no existen registros")
    void listarComprasSinRegistros() throws Exception {
        long cantidad = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM compras", Long.class);

        MvcResult resultado = mockMvc.perform(get("/api/compras"))
                .andExpect(status().isOk())
                .andReturn();

        CompraResponse[] compras = objectMapper.readValue(
                resultado.getResponse().getContentAsString(), CompraResponse[].class);

        assertThat(compras.length).isEqualTo((int) cantidad);
        if (cantidad == 0) {
            assertThat(compras).isEmpty();
        }
    }

    @Test
    @DisplayName("2. Crear una compra válida")
    void crearCompraValida() throws Exception {
        Long funcionId = crearFuncionDisponible(PRECIO_ESTANDAR, 100);

        mockMvc.perform(post("/api/compras")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                solicitud(funcionId, "Carlos Pérez", 3))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.funcionId").value(funcionId))
                .andExpect(jsonPath("$.nombreCliente").value("Carlos Pérez"))
                .andExpect(jsonPath("$.cantidadBoletos").value(3))
                .andExpect(jsonPath("$.total").value(45000.00))
                .andExpect(jsonPath("$.fechaCompra").isString());
    }

    @Test
    @DisplayName("3. La creación responde HTTP 201")
    void creacionResponde201() throws Exception {
        Long funcionId = crearFuncionDisponible(PRECIO_ESTANDAR, 100);

        mockMvc.perform(post("/api/compras")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                solicitud(funcionId, "Ana Torres", 1))))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("4. El servidor genera automáticamente el ID de la compra")
    void servidorGeneraIdAutomaticamente() throws Exception {
        Long funcionId = crearFuncionDisponible(PRECIO_ESTANDAR, 100);

        String cuerpo = """
                {
                  "id": 999999,
                  "funcionId": %d,
                  "nombreCliente": "Carlos Pérez",
                  "cantidadBoletos": 2
                }
                """.formatted(funcionId);

        MvcResult resultado = mockMvc.perform(post("/api/compras")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isCreated())
                .andReturn();

        CompraResponse creada = objectMapper.readValue(
                resultado.getResponse().getContentAsString(), CompraResponse.class);

        assertThat(creada.getId()).isNotNull().isPositive().isNotEqualTo(999999L);
        Long idPersistido = jdbcTemplate.queryForObject(
                "SELECT id FROM compras WHERE funcion_id = ?", Long.class, funcionId);
        assertThat(creada.getId()).isEqualTo(idPersistido);
    }

    @Test
    @DisplayName("5. El total se calcula exactamente como precio por cantidad")
    void calculoExactoDelTotal() throws Exception {
        Long funcionId = crearFuncionDisponible(PRECIO_ESTANDAR, 100);

        CompraResponse compra = crearCompraPorApi(funcionId, "Carlos Pérez", 3);

        assertThat(compra.getTotal()).isEqualByComparingTo("45000.00");

        BigDecimal totalPersistido = jdbcTemplate.queryForObject(
                "SELECT total FROM compras WHERE id = ?", BigDecimal.class, compra.getId());
        assertThat(totalPersistido).isEqualByComparingTo("45000.00");
    }

    @Test
    @DisplayName("6. La fecha de compra es generada por el servidor")
    void fechaGeneradaPorServidor() throws Exception {
        Long funcionId = crearFuncionDisponible(PRECIO_ESTANDAR, 100);
        OffsetDateTime antes = OffsetDateTime.now().minusSeconds(5);

        CompraResponse compra = crearCompraPorApi(funcionId, "Carlos Pérez", 1);

        OffsetDateTime despues = OffsetDateTime.now().plusSeconds(5);
        assertThat(compra.getFechaCompra()).isNotNull();
        assertThat(compra.getFechaCompra()).isBetween(antes, despues);
    }

    @Test
    @DisplayName("7. Verificar persistencia real en PostgreSQL")
    void persistenciaRealEnPostgresql() throws Exception {
        Long funcionId = crearFuncionDisponible(PRECIO_ESTANDAR, 100);

        CompraResponse compra = crearCompraPorApi(funcionId, "María Gómez", 4);

        Map<String, Object> fila = jdbcTemplate.queryForMap(
                "SELECT funcion_id, nombre_cliente, cantidad_boletos, total, fecha_compra "
                        + "FROM compras WHERE id = ?",
                compra.getId());

        assertThat(((Number) fila.get("funcion_id")).longValue()).isEqualTo(funcionId);
        assertThat(fila.get("nombre_cliente")).isEqualTo("María Gómez");
        assertThat(((Number) fila.get("cantidad_boletos")).intValue()).isEqualTo(4);
        assertThat((BigDecimal) fila.get("total")).isEqualByComparingTo("60000.00");
        OffsetDateTime fechaPersistida = jdbcTemplate.queryForObject(
                "SELECT fecha_compra FROM compras WHERE id = ?", OffsetDateTime.class, compra.getId());
        assertThat(fechaPersistida).isNotNull();
        assertThat(compra.getFechaCompra()).isEqualTo(fechaPersistida);
    }

    @Test
    @DisplayName("8. La compra descuenta correctamente los cupos disponibles")
    void descuentoCorrectoDeCupos() throws Exception {
        Long funcionId = crearFuncionDisponible(PRECIO_ESTANDAR, 100);

        crearCompraPorApi(funcionId, "Carlos Pérez", 3);

        assertThat(cuposDisponibles(funcionId)).isEqualTo(97);
    }

    @Test
    @DisplayName("9. Realizar múltiples compras para la misma función")
    void multiplesComprasMismaFuncion() throws Exception {
        Long funcionId = crearFuncionDisponible(PRECIO_ESTANDAR, 100);

        CompraResponse primera = crearCompraPorApi(funcionId, "Cliente A", 2);
        CompraResponse segunda = crearCompraPorApi(funcionId, "Cliente B", 4);

        assertThat(primera.getId()).isNotEqualTo(segunda.getId());
        assertThat(cuposDisponibles(funcionId)).isEqualTo(94);
        assertThat(contarCompras(funcionId)).isEqualTo(2);
        assertThat(boletosVendidos(funcionId)).isEqualTo(6);
    }

    @Test
    @DisplayName("10. Comprar exactamente todos los cupos disponibles")
    void comprarTodosLosCupos() throws Exception {
        Long funcionId = crearFuncionDisponible(PRECIO_ESTANDAR, 20);

        CompraResponse compra = crearCompraPorApi(funcionId, "Cliente Total", 20);

        assertThat(compra.getCantidadBoletos()).isEqualTo(20);
        assertThat(cuposDisponibles(funcionId)).isZero();
        assertThat(contarCompras(funcionId)).isEqualTo(1);
    }

    @Test
    @DisplayName("11. Listar múltiples compras")
    void listarMultiplesCompras() throws Exception {
        Long funcionId = crearFuncionDisponible(PRECIO_ESTANDAR, 100);
        CompraResponse primera = crearCompraPorApi(funcionId, "Cliente A", 1);
        CompraResponse segunda = crearCompraPorApi(funcionId, "Cliente B", 2);

        MvcResult resultado = mockMvc.perform(get("/api/compras"))
                .andExpect(status().isOk())
                .andReturn();

        CompraResponse[] compras = objectMapper.readValue(
                resultado.getResponse().getContentAsString(), CompraResponse[].class);

        List<Long> idsDeLaFuncion = Arrays.stream(compras)
                .filter(c -> funcionId.equals(c.getFuncionId()))
                .map(c -> c.getId())
                .toList();

        assertThat(idsDeLaFuncion).containsExactlyInAnyOrder(primera.getId(), segunda.getId());
    }

    @Test
    @DisplayName("12. El listado se ordena del más reciente al más antiguo y por ID descendente")
    void ordenDelListado() throws Exception {
        Long funcionId = crearFuncionDisponible(PRECIO_ESTANDAR, 100);
        OffsetDateTime comun = OffsetDateTime.now().minusHours(1);
        BigDecimal total = new BigDecimal(PRECIO_ESTANDAR);

        Long masAntigua = insertarCompraConFecha(funcionId, "Antigua", 1, total, comun.minusHours(2));
        Long primera = insertarCompraConFecha(funcionId, "Primera", 1, total, comun);
        Long segunda = insertarCompraConFecha(funcionId, "Segunda", 1, total, comun);

        MvcResult resultado = mockMvc.perform(get("/api/compras"))
                .andExpect(status().isOk())
                .andReturn();

        CompraResponse[] compras = objectMapper.readValue(
                resultado.getResponse().getContentAsString(), CompraResponse[].class);

        List<Long> idsDeLaFuncion = Arrays.stream(compras)
                .filter(c -> funcionId.equals(c.getFuncionId()))
                .map(c -> c.getId())
                .toList();

        assertThat(idsDeLaFuncion).containsExactly(segunda, primera, masAntigua);
    }

    @Test
    @DisplayName("13. El cálculo monetario conserva los decimales")
    void calculoMonetarioConDecimales() throws Exception {
        Long funcionId = crearFuncionDisponible("19999.99", 100);

        CompraResponse compra = crearCompraPorApi(funcionId, "Cliente Decimal", 3);

        assertThat(compra.getTotal()).isEqualByComparingTo("59999.97");
    }

    @Test
    @DisplayName("14. Comprar para una función inexistente")
    void comprarFuncionInexistente() throws Exception {
        mockMvc.perform(post("/api/compras")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                solicitud(999999L, "Carlos Pérez", 2))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Recurso no encontrado"));
    }

    @Test
    @DisplayName("15. Comprar sin funcionId")
    void comprarSinFuncionId() throws Exception {
        mockMvc.perform(post("/api/compras")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                solicitud(null, "Carlos Pérez", 2))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.funcionId").exists());
    }

    @Test
    @DisplayName("16. Comprar con funcionId negativo")
    void comprarConFuncionIdNegativo() throws Exception {
        mockMvc.perform(post("/api/compras")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                solicitud(-5L, "Carlos Pérez", 2))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.funcionId").exists());
    }

    @Test
    @DisplayName("17. Comprar sin nombreCliente")
    void comprarSinNombreCliente() throws Exception {
        Long funcionId = crearFuncionDisponible(PRECIO_ESTANDAR, 100);

        mockMvc.perform(post("/api/compras")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                solicitud(funcionId, null, 2))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.nombreCliente").exists());
    }

    @Test
    @DisplayName("18. Comprar con nombreCliente vacío")
    void comprarConNombreClienteVacio() throws Exception {
        Long funcionId = crearFuncionDisponible(PRECIO_ESTANDAR, 100);

        mockMvc.perform(post("/api/compras")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                solicitud(funcionId, "   ", 2))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.nombreCliente").exists());
    }

    @Test
    @DisplayName("19. Comprar con nombreCliente superior a 150 caracteres")
    void comprarConNombreClienteMuyLargo() throws Exception {
        Long funcionId = crearFuncionDisponible(PRECIO_ESTANDAR, 100);

        mockMvc.perform(post("/api/compras")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                solicitud(funcionId, "a".repeat(151), 2))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.nombreCliente").exists());
    }

    @Test
    @DisplayName("20. Comprar sin cantidadBoletos")
    void comprarSinCantidadBoletos() throws Exception {
        Long funcionId = crearFuncionDisponible(PRECIO_ESTANDAR, 100);

        mockMvc.perform(post("/api/compras")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                solicitud(funcionId, "Carlos Pérez", null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.cantidadBoletos").exists());
    }

    @Test
    @DisplayName("21. Comprar cero boletos")
    void comprarCeroBoletos() throws Exception {
        Long funcionId = crearFuncionDisponible(PRECIO_ESTANDAR, 100);

        mockMvc.perform(post("/api/compras")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                solicitud(funcionId, "Carlos Pérez", 0))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.cantidadBoletos").exists());
    }

    @Test
    @DisplayName("22. Comprar una cantidad negativa")
    void comprarCantidadNegativa() throws Exception {
        Long funcionId = crearFuncionDisponible(PRECIO_ESTANDAR, 100);

        mockMvc.perform(post("/api/compras")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                solicitud(funcionId, "Carlos Pérez", -3))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.cantidadBoletos").exists());
    }

    @Test
    @DisplayName("23. Enviar JSON malformado")
    void enviarJsonMalformado() throws Exception {
        String jsonInvalido = "{ \"funcionId\": 1, \"nombreCliente\": \"Carlos\", }";

        mockMvc.perform(post("/api/compras")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonInvalido))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("JSON inválido"));
    }

    @Test
    @DisplayName("24. Solicitar más boletos de los disponibles")
    void solicitarMasBoletosDeLosDisponibles() throws Exception {
        Long funcionId = crearFuncionDisponible(PRECIO_ESTANDAR, 5);

        mockMvc.perform(post("/api/compras")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                solicitud(funcionId, "Carlos Pérez", 6))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Cupos insuficientes"))
                .andExpect(jsonPath("$.mensaje").value(
                        "Se solicitaron 6 boletos, pero solo hay 5 disponibles"));
    }

    @Test
    @DisplayName("25. Comprar para una función agotada")
    void comprarFuncionAgotada() throws Exception {
        Pelicula pelicula = crearPelicula("Interestelar", "Ciencia ficción", 169, "PG-13");
        Long funcionId = insertarFuncion(pelicula.getId(), FECHA, HORA, "Sala 1",
                new BigDecimal(PRECIO_ESTANDAR), 10, 0);

        mockMvc.perform(post("/api/compras")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                solicitud(funcionId, "Carlos Pérez", 1))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("26. Una compra rechazada no modifica los cupos")
    void compraRechazadaNoModificaCupos() throws Exception {
        Long funcionId = crearFuncionDisponible(PRECIO_ESTANDAR, 5);

        mockMvc.perform(post("/api/compras")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                solicitud(funcionId, "Carlos Pérez", 30))))
                .andExpect(status().isConflict());

        assertThat(cuposDisponibles(funcionId)).isEqualTo(5);
    }

    @Test
    @DisplayName("27. Una compra rechazada no genera registros")
    void compraRechazadaNoGeneraRegistros() throws Exception {
        Long funcionId = crearFuncionDisponible(PRECIO_ESTANDAR, 5);

        mockMvc.perform(post("/api/compras")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                solicitud(funcionId, "Carlos Pérez", 30))))
                .andExpect(status().isConflict());

        assertThat(contarCompras(funcionId)).isZero();
    }

    @Test
    @DisplayName("28. El cliente no puede manipular el total")
    void clienteNoPuedeManipularTotal() throws Exception {
        Long funcionId = crearFuncionDisponible(PRECIO_ESTANDAR, 100);

        String cuerpo = """
                {
                  "funcionId": %d,
                  "nombreCliente": "Carlos Pérez",
                  "cantidadBoletos": 3,
                  "total": 1.00
                }
                """.formatted(funcionId);

        MvcResult resultado = mockMvc.perform(post("/api/compras")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isCreated())
                .andReturn();

        CompraResponse compra = objectMapper.readValue(
                resultado.getResponse().getContentAsString(), CompraResponse.class);
        assertThat(compra.getTotal()).isEqualByComparingTo("45000.00");
    }

    @Test
    @DisplayName("29. El cliente no puede manipular la fecha de compra")
    void clienteNoPuedeManipularFecha() throws Exception {
        Long funcionId = crearFuncionDisponible(PRECIO_ESTANDAR, 100);
        OffsetDateTime antes = OffsetDateTime.now().minusSeconds(5);

        String cuerpo = """
                {
                  "funcionId": %d,
                  "nombreCliente": "Carlos Pérez",
                  "cantidadBoletos": 1,
                  "fechaCompra": "2000-01-01T00:00:00Z"
                }
                """.formatted(funcionId);

        MvcResult resultado = mockMvc.perform(post("/api/compras")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isCreated())
                .andReturn();

        CompraResponse compra = objectMapper.readValue(
                resultado.getResponse().getContentAsString(), CompraResponse.class);
        assertThat(compra.getFechaCompra()).isAfter(antes);
    }

    @Test
    @DisplayName("30. Rechazo controlado de un total que excede NUMERIC(12,2)")
    void rechazoTotalFueraDeRango() throws Exception {
        Long funcionId = crearFuncionDisponible("9999999999.99", 10);

        mockMvc.perform(post("/api/compras")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                solicitud(funcionId, "Carlos Pérez", 2))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Monto fuera de rango"));

        assertThat(cuposDisponibles(funcionId)).isEqualTo(10);
        assertThat(contarCompras(funcionId)).isZero();
    }

    @Test
    @DisplayName("31. PUT, PATCH y DELETE no están disponibles para compras")
    void metodosNoPermitidos() throws Exception {
        mockMvc.perform(put("/api/compras"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405));

        mockMvc.perform(patch("/api/compras"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405));

        mockMvc.perform(delete("/api/compras"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405));
    }
}
