package com.taquillacine.api;

import com.taquillacine.api.dto.DetalleVentaResponse;
import com.taquillacine.api.dto.ReporteVentasResponse;
import com.taquillacine.api.model.Pelicula;
import com.taquillacine.api.support.PruebaIntegracionBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de integración de {@code GET /api/reportes/ventas}.
 *
 * <p>Todas las fechas de compra se insertan de forma explícita, por lo que los
 * resultados no dependen de la fecha real del equipo. La base autorizada se
 * verifica en {@code PruebaIntegracionBase} antes de cada prueba y la limpieza
 * solo elimina las compras, funciones y películas creadas por la propia prueba.</p>
 */
class ReporteControllerIntegrationTest extends PruebaIntegracionBase {

    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");
    private static final LocalTime HORA_FUNCION = LocalTime.of(18, 30);
    private static final BigDecimal CERO = new BigDecimal("0.00");

    // ------------------------------------------------------------------
    // Utilidades
    // ------------------------------------------------------------------

    private MockMvc nuevoMockMvc() {
        return MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    private Long crearFuncion(String titulo, String sala, LocalDate fechaFuncion, String precio) {
        Pelicula pelicula = crearPelicula(titulo, "Ciencia ficción", 120, "PG-13");
        return insertarFuncion(pelicula.getId(), fechaFuncion, HORA_FUNCION, sala,
                new BigDecimal(precio), 100, 100);
    }

    private void registrarVenta(Long funcionId, String nombre, int boletos, BigDecimal total,
                                OffsetDateTime fechaCompra) {
        String sql = """
                INSERT INTO compras
                    (funcion_id, nombre_cliente, cantidad_boletos, total, fecha_compra)
                VALUES (?, ?, ?, ?, ?)
                """;
        jdbcTemplate.update(sql, funcionId, nombre, boletos, total, fechaCompra);
    }

    private static OffsetDateTime enBogota(LocalDate fecha, int hora, int minuto) {
        return fecha.atTime(hora, minuto).atZone(BOGOTA).toOffsetDateTime();
    }

    private static BigDecimal monto(String valor) {
        return new BigDecimal(valor);
    }

    private MvcResult consultarCrudo(MockMvc mvc, LocalDate desde, LocalDate hasta)
            throws Exception {
        return mvc.perform(get("/api/reportes/ventas")
                        .param("fechaDesde", desde.toString())
                        .param("fechaHasta", hasta.toString()))
                .andExpect(status().isOk())
                .andReturn();
    }

    private ReporteVentasResponse consultar(MockMvc mvc, LocalDate desde, LocalDate hasta)
            throws Exception {
        MvcResult resultado = consultarCrudo(mvc, desde, hasta);
        return objectMapper.readValue(
                resultado.getResponse().getContentAsString(StandardCharsets.UTF_8),
                ReporteVentasResponse.class);
    }

    private ReporteVentasResponse consultar(LocalDate desde, LocalDate hasta) throws Exception {
        return consultar(mockMvc, desde, hasta);
    }

    /** Comprueba que los indicadores sean exactamente la suma del detalle devuelto. */
    private void verificarIndicadores(ReporteVentasResponse reporte) {
        long boletos = reporte.getCompras().stream()
                .mapToLong(d -> d.getCantidadBoletos())
                .sum();
        BigDecimal recaudado = reporte.getCompras().stream()
                .map(d -> d.getTotal())
                .reduce(new BigDecimal("0.00"), (a, b) -> a.add(b));

        assertThat(reporte.getCantidadCompras())
                .isEqualTo(reporte.getCompras().size());
        assertThat(reporte.getTotalBoletos()).isEqualTo(boletos);
        assertThat(reporte.getTotalRecaudado()).isEqualByComparingTo(recaudado);
    }

    // ------------------------------------------------------------------
    // 1-18: consultas y contenido del reporte
    // ------------------------------------------------------------------

    @Test
    @DisplayName("1. Consultar un período sin ventas")
    void periodoSinVentas() throws Exception {
        LocalDate fecha = LocalDate.of(2030, 1, 1);

        ReporteVentasResponse reporte = consultar(fecha, fecha);

        assertThat(reporte.getFechaDesde()).isEqualTo(fecha);
        assertThat(reporte.getFechaHasta()).isEqualTo(fecha);
        assertThat(reporte.getCantidadCompras()).isZero();
        assertThat(reporte.getTotalBoletos()).isZero();
        assertThat(reporte.getTotalRecaudado()).isEqualByComparingTo(CERO);
        assertThat(reporte.getCompras()).isEmpty();
    }

    @Test
    @DisplayName("2. Consultar ventas de un día específico")
    void ventasDeUnDia() throws Exception {
        LocalDate fecha = LocalDate.of(2030, 2, 10);
        Long funcionId = crearFuncion("Interestelar", "Sala 1", fecha.plusDays(20), "15000.00");
        registrarVenta(funcionId, "María Gómez", 2, monto("30000.00"), enBogota(fecha, 10, 0));
        registrarVenta(funcionId, "Carlos Pérez", 3, monto("45000.00"), enBogota(fecha, 20, 0));

        ReporteVentasResponse reporte = consultar(fecha, fecha);

        assertThat(reporte.getCompras()).hasSize(2);
        assertThat(reporte.getCompras())
                .extracting(d -> d.getNombreCliente())
                .containsExactlyInAnyOrder("María Gómez", "Carlos Pérez");
        verificarIndicadores(reporte);
    }

    @Test
    @DisplayName("3. Consultar ventas de varios días")
    void ventasDeVariosDias() throws Exception {
        LocalDate desde = LocalDate.of(2030, 3, 1);
        LocalDate hasta = LocalDate.of(2030, 3, 3);
        Long funcionId = crearFuncion("Dune", "Sala 2", hasta.plusDays(30), "12000.00");
        registrarVenta(funcionId, "Cliente 1", 1, monto("12000.00"), enBogota(desde, 9, 0));
        registrarVenta(funcionId, "Cliente 2", 1, monto("12000.00"), enBogota(desde.plusDays(1), 15, 0));
        registrarVenta(funcionId, "Cliente 3", 1, monto("12000.00"), enBogota(hasta, 21, 0));

        ReporteVentasResponse reporte = consultar(desde, hasta);

        assertThat(reporte.getCompras()).hasSize(3);
        assertThat(reporte.getFechaDesde()).isEqualTo(desde);
        assertThat(reporte.getFechaHasta()).isEqualTo(hasta);
        verificarIndicadores(reporte);
    }

    @Test
    @DisplayName("4. Verificar cantidadCompras")
    void verificarCantidadCompras() throws Exception {
        LocalDate desde = LocalDate.of(2030, 3, 10);
        LocalDate hasta = LocalDate.of(2030, 3, 12);
        Long funcionId = crearFuncion("Oppenheimer", "Sala 1", hasta.plusDays(30), "18000.00");
        registrarVenta(funcionId, "A", 1, monto("18000.00"), enBogota(desde, 8, 0));
        registrarVenta(funcionId, "B", 2, monto("36000.00"), enBogota(desde, 12, 0));
        registrarVenta(funcionId, "C", 1, monto("18000.00"), enBogota(desde.plusDays(1), 17, 0));
        registrarVenta(funcionId, "D", 3, monto("54000.00"), enBogota(hasta, 22, 0));

        ReporteVentasResponse reporte = consultar(desde, hasta);

        assertThat(reporte.getCantidadCompras()).isEqualTo(4);
        assertThat(reporte.getCantidadCompras()).isEqualTo(reporte.getCompras().size());
    }

    @Test
    @DisplayName("5. Verificar totalBoletos")
    void verificarTotalBoletos() throws Exception {
        LocalDate desde = LocalDate.of(2030, 3, 20);
        LocalDate hasta = LocalDate.of(2030, 3, 21);
        Long funcionId = crearFuncion("Barbie", "Sala 3", hasta.plusDays(30), "10000.00");
        registrarVenta(funcionId, "A", 2, monto("20000.00"), enBogota(desde, 9, 0));
        registrarVenta(funcionId, "B", 3, monto("30000.00"), enBogota(desde, 13, 0));
        registrarVenta(funcionId, "C", 1, monto("10000.00"), enBogota(hasta, 19, 0));

        ReporteVentasResponse reporte = consultar(desde, hasta);

        assertThat(reporte.getTotalBoletos()).isEqualTo(6);
    }

    @Test
    @DisplayName("6. Verificar totalRecaudado")
    void verificarTotalRecaudado() throws Exception {
        LocalDate fecha = LocalDate.of(2030, 4, 1);
        Long funcionId = crearFuncion("Interestelar", "Sala 1", fecha.plusDays(30), "15000.00");
        registrarVenta(funcionId, "A", 2, monto("30000.00"), enBogota(fecha, 10, 0));
        registrarVenta(funcionId, "B", 3, monto("45000.00"), enBogota(fecha, 11, 0));
        registrarVenta(funcionId, "C", 1, monto("15000.00"), enBogota(fecha, 12, 0));

        ReporteVentasResponse reporte = consultar(fecha, fecha);

        assertThat(reporte.getTotalRecaudado()).isEqualByComparingTo(monto("90000.00"));
    }

    @Test
    @DisplayName("7. Verificar precisión de valores monetarios decimales")
    void precisionMonetaria() throws Exception {
        LocalDate fecha = LocalDate.of(2030, 4, 10);
        Long funcionId = crearFuncion("Precisión", "Sala 4", fecha.plusDays(30), "12345.67");
        registrarVenta(funcionId, "A", 1, monto("12345.67"), enBogota(fecha, 9, 0));
        registrarVenta(funcionId, "B", 1, monto("0.01"), enBogota(fecha, 10, 0));
        registrarVenta(funcionId, "C", 1, monto("999.99"), enBogota(fecha, 11, 0));

        ReporteVentasResponse reporte = consultar(fecha, fecha);

        assertThat(reporte.getTotalRecaudado()).isEqualByComparingTo(monto("13345.67"));
        assertThat(reporte.getCompras())
                .extracting(d -> d.getTotal())
                .usingElementComparator((a, b) -> a.compareTo(b))
                .containsExactlyInAnyOrder(monto("12345.67"), monto("0.01"), monto("999.99"));
    }

    @Test
    @DisplayName("8. Verificar datos de películas mediante JOIN")
    void datosDePeliculasMedianteJoin() throws Exception {
        LocalDate fecha = LocalDate.of(2030, 5, 10);
        Long funcionId = crearFuncion("Interestelar", "Sala 1", fecha.plusDays(30), "15000.00");
        registrarVenta(funcionId, "María Gómez", 2, monto("30000.00"), enBogota(fecha, 19, 13));

        ReporteVentasResponse reporte = consultar(fecha, fecha);

        assertThat(reporte.getCompras()).hasSize(1);
        DetalleVentaResponse detalle = reporte.getCompras().get(0);
        assertThat(detalle.getTituloPelicula()).isEqualTo("Interestelar");
        assertThat(detalle.getFuncionId()).isEqualTo(funcionId);
    }

    @Test
    @DisplayName("9. Verificar datos de funciones")
    void datosDeFunciones() throws Exception {
        LocalDate fechaCompra = LocalDate.of(2030, 5, 11);
        LocalDate fechaFuncion = LocalDate.of(2030, 6, 15);
        Long funcionId = crearFuncion("Dune: Parte Dos", "Sala Premium", fechaFuncion, "22000.50");
        registrarVenta(funcionId, "Carlos Pérez", 1, monto("22000.50"), enBogota(fechaCompra, 18, 45));

        ReporteVentasResponse reporte = consultar(fechaCompra, fechaCompra);

        DetalleVentaResponse detalle = reporte.getCompras().get(0);
        assertThat(detalle.getFuncionId()).isEqualTo(funcionId);
        assertThat(detalle.getFechaFuncion()).isEqualTo(fechaFuncion);
        assertThat(detalle.getHoraFuncion()).isEqualTo(HORA_FUNCION);
        assertThat(detalle.getSala()).isEqualTo("Sala Premium");
        assertThat(detalle.getNombreCliente()).isEqualTo("Carlos Pérez");
        assertThat(detalle.getCantidadBoletos()).isEqualTo(1);
        assertThat(detalle.getTotal()).isEqualByComparingTo(monto("22000.50"));
    }

    @Test
    @DisplayName("10. Verificar orden descendente de las compras")
    void ordenDescendenteDeCompras() throws Exception {
        LocalDate fecha = LocalDate.of(2030, 5, 20);
        Long funcionId = crearFuncion("Orden", "Sala 5", fecha.plusDays(30), "10000.00");
        registrarVenta(funcionId, "Primera", 1, monto("10000.00"), enBogota(fecha, 10, 0));
        registrarVenta(funcionId, "Segunda", 1, monto("10000.00"), enBogota(fecha, 12, 0));
        registrarVenta(funcionId, "Tercera mismo instante", 1, monto("10000.00"),
                enBogota(fecha, 12, 0));

        List<Long> ids = jdbcTemplate.queryForList(
                "SELECT id FROM compras WHERE funcion_id = ? ORDER BY fecha_compra DESC, id DESC",
                Long.class, funcionId);

        ReporteVentasResponse reporte = consultar(fecha, fecha);

        assertThat(reporte.getCompras())
                .extracting(d -> d.getId())
                .containsExactlyElementsOf(ids);
    }

    @Test
    @DisplayName("11. Excluir ventas anteriores al rango")
    void excluirVentasAnteriores() throws Exception {
        LocalDate desde = LocalDate.of(2030, 6, 5);
        LocalDate hasta = LocalDate.of(2030, 6, 5);
        Long funcionId = crearFuncion("Anterior", "Sala 6", desde.plusDays(30), "10000.00");
        registrarVenta(funcionId, "Antes", 1, monto("10000.00"), enBogota(desde.minusDays(1), 23, 59));
        registrarVenta(funcionId, "Dentro", 1, monto("10000.00"), enBogota(desde, 8, 0));

        ReporteVentasResponse reporte = consultar(desde, hasta);

        assertThat(reporte.getCompras())
                .extracting(d -> d.getNombreCliente())
                .containsExactly("Dentro");
    }

    @Test
    @DisplayName("12. Excluir ventas posteriores al rango")
    void excluirVentasPosteriores() throws Exception {
        LocalDate desde = LocalDate.of(2030, 6, 6);
        LocalDate hasta = LocalDate.of(2030, 6, 6);
        Long funcionId = crearFuncion("Posterior", "Sala 6", desde.plusDays(30), "10000.00");
        registrarVenta(funcionId, "Dentro", 1, monto("10000.00"), enBogota(hasta, 18, 0));
        registrarVenta(funcionId, "Después", 1, monto("10000.00"), enBogota(hasta.plusDays(1), 0, 1));

        ReporteVentasResponse reporte = consultar(desde, hasta);

        assertThat(reporte.getCompras())
                .extracting(d -> d.getNombreCliente())
                .containsExactly("Dentro");
    }

    @Test
    @DisplayName("13. Incluir compras exactamente a medianoche del día inicial")
    void incluirMedianocheDelDiaInicial() throws Exception {
        LocalDate desde = LocalDate.of(2030, 7, 1);
        Long funcionId = crearFuncion("Medianoche inicial", "Sala 7", desde.plusDays(30), "10000.00");
        registrarVenta(funcionId, "Medianoche", 1, monto("10000.00"), enBogota(desde, 0, 0));

        ReporteVentasResponse reporte = consultar(desde, desde);

        assertThat(reporte.getCompras()).hasSize(1);
        assertThat(reporte.getCompras().get(0).getNombreCliente()).isEqualTo("Medianoche");
    }

    @Test
    @DisplayName("14. Incluir compras realizadas al final del último día")
    void incluirFinalDelUltimoDia() throws Exception {
        LocalDate fecha = LocalDate.of(2030, 7, 3);
        Long funcionId = crearFuncion("Fin del día", "Sala 7", fecha.plusDays(30), "10000.00");
        OffsetDateTime ultimoInstante = fecha.atTime(LocalTime.of(23, 59, 59, 999_000_000))
                .atZone(BOGOTA)
                .toOffsetDateTime();
        registrarVenta(funcionId, "Último instante", 1, monto("10000.00"), ultimoInstante);

        ReporteVentasResponse reporte = consultar(fecha, fecha);

        assertThat(reporte.getCompras()).hasSize(1);
    }

    @Test
    @DisplayName("15. Excluir compras a medianoche del día siguiente al rango")
    void excluirMedianocheDelDiaSiguiente() throws Exception {
        LocalDate desde = LocalDate.of(2030, 7, 4);
        LocalDate hasta = LocalDate.of(2030, 7, 5);
        Long funcionId = crearFuncion("Límite exclusivo", "Sala 7", hasta.plusDays(30), "10000.00");
        registrarVenta(funcionId, "Dentro", 1, monto("10000.00"), enBogota(hasta, 23, 59));
        registrarVenta(funcionId, "Siguiente medianoche", 1, monto("10000.00"),
                enBogota(hasta.plusDays(1), 0, 0));

        ReporteVentasResponse reporte = consultar(desde, hasta);

        assertThat(reporte.getCompras())
                .extracting(d -> d.getNombreCliente())
                .containsExactly("Dentro");
    }

    @Test
    @DisplayName("16. Verificar correctamente la diferencia entre UTC y America/Bogota")
    void diferenciaEntreUtcYAmericaBogota() throws Exception {
        LocalDate fechaBogota = LocalDate.of(2030, 8, 1);
        Long funcionId = crearFuncion("Zona horaria", "Sala 8", fechaBogota.plusDays(30), "10000.00");

        // 2030-08-02T03:00Z y 2030-08-02T04:30Z equivalen al 2030-08-01 en Bogotá (UTC-5).
        OffsetDateTime utcPrimero = OffsetDateTime.of(2030, 8, 2, 3, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime utcSegundo = OffsetDateTime.of(2030, 8, 2, 4, 30, 0, 0, ZoneOffset.UTC);
        registrarVenta(funcionId, "UTC-1", 1, monto("10000.00"), utcPrimero);
        registrarVenta(funcionId, "UTC-2", 1, monto("10000.00"), utcSegundo);

        ReporteVentasResponse primeroDeAgosto = consultar(fechaBogota, fechaBogota);
        ReporteVentasResponse dosDeAgosto = consultar(fechaBogota.plusDays(1), fechaBogota.plusDays(1));
        MvcResult crudo = consultarCrudo(mockMvc, fechaBogota, fechaBogota);
        String json = crudo.getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertThat(primeroDeAgosto.getCompras()).hasSize(2);
        assertThat(dosDeAgosto.getCompras()).isEmpty();
        assertThat(json)
                .as("El JSON debe expresar las fechas de compra en America/Bogota")
                .contains("\"fechaCompra\":\"2030-08-01T22:00:00-05:00\"")
                .contains("\"fechaCompra\":\"2030-08-01T23:30:00-05:00\"");
        assertThat(primeroDeAgosto.getCompras())
                .allSatisfy(detalle -> assertThat(
                        detalle.getFechaCompra().atZoneSameInstant(BOGOTA).toLocalDate())
                        .isEqualTo(fechaBogota));
    }

    @Test
    @DisplayName("17. Consultar compras cuya fecha de función sea diferente a la fecha de compra")
    void fechaDeFuncionDistintaALaFechaDeCompra() throws Exception {
        LocalDate fechaCompra = LocalDate.of(2030, 9, 10);
        LocalDate fechaFuncion = LocalDate.of(2031, 1, 15);
        Long funcionId = crearFuncion("Función futura", "Sala 1", fechaFuncion, "14000.00");
        registrarVenta(funcionId, "Compra anticipada", 2, monto("28000.00"),
                enBogota(fechaCompra, 16, 0));

        ReporteVentasResponse porFechaDeCompra = consultar(fechaCompra, fechaCompra);
        ReporteVentasResponse porFechaDeFuncion = consultar(fechaFuncion, fechaFuncion);

        assertThat(porFechaDeCompra.getCompras()).hasSize(1);
        assertThat(porFechaDeCompra.getCompras().get(0).getFechaFuncion()).isEqualTo(fechaFuncion);
        assertThat(porFechaDeFuncion.getCompras()).isEmpty();
    }

    @Test
    @DisplayName("18. Modificar el precio de una función no altera los ingresos históricos")
    void precioActualNoAlteraIngresosHistoricos() throws Exception {
        LocalDate fecha = LocalDate.of(2030, 10, 1);
        Long funcionId = crearFuncion("Precio histórico", "Sala 1", fecha.plusDays(30), "15000.00");
        registrarVenta(funcionId, "Compra histórica", 2, monto("30000.00"), enBogota(fecha, 11, 0));

        jdbcTemplate.update("UPDATE funciones SET precio = ? WHERE id = ?",
                monto("50000.00"), funcionId);

        ReporteVentasResponse reporte = consultar(fecha, fecha);

        assertThat(jdbcTemplate.queryForObject(
                "SELECT precio FROM funciones WHERE id = ?", BigDecimal.class, funcionId))
                .isEqualByComparingTo(monto("50000.00"));
        assertThat(reporte.getCompras()).hasSize(1);
        assertThat(reporte.getCompras().get(0).getTotal()).isEqualByComparingTo(monto("30000.00"));
        assertThat(reporte.getTotalRecaudado()).isEqualByComparingTo(monto("30000.00"));
    }

    // ------------------------------------------------------------------
    // 19-24: validación de parámetros
    // ------------------------------------------------------------------

    @Test
    @DisplayName("19. Solicitud sin fechaDesde")
    void solicitudSinFechaDesde() throws Exception {
        mockMvc.perform(get("/api/reportes/ventas")
                        .param("fechaHasta", "2030-11-17"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Parámetro faltante"));
    }

    @Test
    @DisplayName("20. Solicitud sin fechaHasta")
    void solicitudSinFechaHasta() throws Exception {
        mockMvc.perform(get("/api/reportes/ventas")
                        .param("fechaDesde", "2030-11-17"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Parámetro faltante"));
    }

    @Test
    @DisplayName("21. Solicitud sin ambos parámetros")
    void solicitudSinAmbosParametros() throws Exception {
        mockMvc.perform(get("/api/reportes/ventas"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Parámetro faltante"));
    }

    @Test
    @DisplayName("22. Solicitud con formato de fecha inválido")
    void solicitudConFormatoDeFechaInvalido() throws Exception {
        mockMvc.perform(get("/api/reportes/ventas")
                        .param("fechaDesde", "17-11-2030")
                        .param("fechaHasta", "2030-11-30"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Parámetro inválido"));
    }

    @Test
    @DisplayName("23. Solicitud con una fecha inexistente en el calendario")
    void solicitudConFechaInexistente() throws Exception {
        mockMvc.perform(get("/api/reportes/ventas")
                        .param("fechaDesde", "2030-02-30")
                        .param("fechaHasta", "2030-03-05"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Parámetro inválido"));
    }

    @Test
    @DisplayName("24. Solicitud con fechaDesde posterior a fechaHasta")
    void solicitudConRangoInvertido() throws Exception {
        mockMvc.perform(get("/api/reportes/ventas")
                        .param("fechaDesde", "2030-12-10")
                        .param("fechaHasta", "2030-12-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Rango de fechas inválido"));
    }

    // ------------------------------------------------------------------
    // 25-26: efecto nulo y concurrencia
    // ------------------------------------------------------------------

    @Test
    @DisplayName("25. Verificar que el reporte no modifique ningún dato")
    void elReporteNoModificaDatos() throws Exception {
        LocalDate fecha = LocalDate.of(2030, 10, 10);
        Long funcionId = crearFuncion("Solo lectura", "Sala 1", fecha.plusDays(30), "11000.00");
        registrarVenta(funcionId, "Cliente", 2, monto("22000.00"), enBogota(fecha, 14, 0));

        Map<String, Object> comprasAntes = jdbcTemplate.queryForMap(
                "SELECT COUNT(*) AS cantidad, COALESCE(SUM(total), 0) AS recaudado FROM compras");
        Map<String, Object> funcionAntes = jdbcTemplate.queryForMap(
                "SELECT precio, cupos_totales, cupos_disponibles FROM funciones WHERE id = ?",
                funcionId);
        Map<String, Object> peliculaAntes = jdbcTemplate.queryForMap(
                "SELECT titulo, genero, duracion_min, clasificacion FROM peliculas "
                        + "WHERE id = (SELECT pelicula_id FROM funciones WHERE id = ?)",
                funcionId);

        consultar(fecha, fecha);

        assertThat(jdbcTemplate.queryForMap(
                "SELECT COUNT(*) AS cantidad, COALESCE(SUM(total), 0) AS recaudado FROM compras"))
                .isEqualTo(comprasAntes);
        assertThat(jdbcTemplate.queryForMap(
                "SELECT precio, cupos_totales, cupos_disponibles FROM funciones WHERE id = ?",
                funcionId)).isEqualTo(funcionAntes);
        assertThat(jdbcTemplate.queryForMap(
                "SELECT titulo, genero, duracion_min, clasificacion FROM peliculas "
                        + "WHERE id = (SELECT pelicula_id FROM funciones WHERE id = ?)",
                funcionId)).isEqualTo(peliculaAntes);
    }

    @Test
    @DisplayName("26. Los indicadores coinciden con el detalle ante actividad concurrente")
    void indicadoresConsistentesAnteConcurrencia() throws Exception {
        LocalDate fecha = LocalDate.of(2030, 11, 15);
        Long funcionId = crearFuncion("Concurrente", "Sala 9", fecha.plusDays(30), "10000.00");
        for (int i = 0; i < 5; i++) {
            registrarVenta(funcionId, "Previo " + i, 1, monto("10000.00"),
                    enBogota(fecha, 8, i));
        }

        int escritores = 3;
        int comprasPorEscritor = 4;
        int lectores = 4;
        int consultasPorLector = 3;
        int participantes = escritores + lectores;

        ExecutorService ejecutor = Executors.newFixedThreadPool(participantes);
        CountDownLatch listos = new CountDownLatch(participantes);
        CountDownLatch salida = new CountDownLatch(1);
        List<Future<Void>> futuros = new ArrayList<>();

        try {
            for (int e = 0; e < escritores; e++) {
                int indice = e;
                futuros.add(ejecutor.submit((Callable<Void>) () -> {
                    listos.countDown();
                    if (!salida.await(10, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Los escritores no se sincronizaron");
                    }
                    for (int c = 0; c < comprasPorEscritor; c++) {
                        registrarVenta(funcionId, "Concurrente " + indice + "-" + c, 2,
                                monto("20000.00"), enBogota(fecha, 12 + indice, c));
                    }
                    return null;
                }));
            }

            for (int l = 0; l < lectores; l++) {
                futuros.add(ejecutor.submit((Callable<Void>) () -> {
                    MockMvc mvc = nuevoMockMvc();
                    listos.countDown();
                    if (!salida.await(10, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Los lectores no se sincronizaron");
                    }
                    for (int c = 0; c < consultasPorLector; c++) {
                        ReporteVentasResponse reporte = consultar(mvc, fecha, fecha);
                        verificarIndicadores(reporte);
                        assertThat(reporte.getCompras()).allSatisfy(detalle ->
                                assertThat(detalle.getFechaCompra()
                                        .atZoneSameInstant(BOGOTA)
                                        .toLocalDate())
                                        .isEqualTo(fecha));
                    }
                    return null;
                }));
            }

            assertThat(listos.await(10, TimeUnit.SECONDS))
                    .as("Todos los participantes deben estar listos")
                    .isTrue();
            salida.countDown();

            for (Future<Void> futuro : futuros) {
                futuro.get(30, TimeUnit.SECONDS);
            }
        } finally {
            salida.countDown();
            ejecutor.shutdownNow();
            ejecutor.awaitTermination(5, TimeUnit.SECONDS);
        }

        ReporteVentasResponse reporteFinal = consultar(fecha, fecha);
        verificarIndicadores(reporteFinal);
        assertThat(reporteFinal.getCantidadCompras())
                .isEqualTo(5L + (long) escritores * comprasPorEscritor);
        assertThat(reporteFinal.getTotalBoletos())
                .isEqualTo(5L + (long) escritores * comprasPorEscritor * 2);
        assertThat(reporteFinal.getTotalRecaudado())
                .isEqualByComparingTo(monto("290000.00"));
    }
}
