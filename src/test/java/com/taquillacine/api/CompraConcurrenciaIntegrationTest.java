package com.taquillacine.api;

import com.taquillacine.api.dto.CompraRequest;
import com.taquillacine.api.dto.FuncionRequest;
import com.taquillacine.api.model.Pelicula;
import com.taquillacine.api.support.PruebaIntegracionBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * Pruebas de concurrencia real sobre {@code POST /api/compras}.
 *
 * <p>Cada solicitud se ejecuta en su propio hilo, con su propia instancia de
 * {@link MockMvc}, por lo que Spring abre una conexión y una transacción
 * independiente para cada una. Las tareas se sincronizan con un
 * {@link CountDownLatch} para que salgan al mismo tiempo, y se esperan con
 * timeouts acotados. El control de sobreventa depende del bloqueo
 * {@code SELECT ... FOR UPDATE} de PostgreSQL, no de sincronización Java.</p>
 */
class CompraConcurrenciaIntegrationTest extends PruebaIntegracionBase {

    private static final LocalDate FECHA = LocalDate.of(2026, 12, 1);
    private static final LocalTime HORA = LocalTime.of(18, 30);
    private static final BigDecimal PRECIO = new BigDecimal("15000.00");
    private static final int ESPERA_SINCRONIZACION_SEGUNDOS = 10;
    private static final int ESPERA_RESULTADO_SEGUNDOS = 30;

    private record Resultado(int status, String cuerpo) {
    }

    private Long crearFuncion(String precio, int cupos) {
        Pelicula pelicula = crearPelicula("Interestelar", "Ciencia ficción", 169, "PG-13");
        return insertarFuncion(pelicula.getId(), FECHA, HORA, "Sala 1",
                new BigDecimal(precio), cupos, cupos);
    }

    private MockMvc nuevoMockMvc() {
        return MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    private Resultado comprar(MockMvc mvc, Long funcionId, String nombre, int cantidad) {
        try {
            String cuerpo = objectMapper.writeValueAsString(
                    new CompraRequest(funcionId, nombre, cantidad));
            MvcResult resultado = mvc.perform(post("/api/compras")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpo))
                    .andReturn();
            return new Resultado(resultado.getResponse().getStatus(),
                    resultado.getResponse().getContentAsString(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("Error ejecutando una compra concurrente", e);
        }
    }

    private Resultado actualizarCapacidad(MockMvc mvc, Long funcionId, Long peliculaId,
                                          int cuposTotales) {
        try {
            String cuerpo = objectMapper.writeValueAsString(new FuncionRequest(
                    peliculaId, FECHA, HORA, "Sala 1", PRECIO, cuposTotales));
            MvcResult resultado = mvc.perform(put("/api/funciones/{id}", funcionId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpo))
                    .andReturn();
            return new Resultado(resultado.getResponse().getStatus(),
                    resultado.getResponse().getContentAsString(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("Error actualizando la capacidad en concurrencia", e);
        }
    }

    private Resultado eliminarFuncion(MockMvc mvc, Long funcionId) {
        try {
            MvcResult resultado = mvc.perform(delete("/api/funciones/{id}", funcionId))
                    .andReturn();
            return new Resultado(resultado.getResponse().getStatus(),
                    resultado.getResponse().getContentAsString(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("Error eliminando la función en concurrencia", e);
        }
    }

    /**
     * Ejecuta todas las tareas en hilos independientes, las libera a la vez y
     * recoge sus resultados con timeout. Si alguna no termina, la prueba falla.
     */
    private List<Resultado> ejecutarConcurrentemente(List<Callable<Resultado>> tareas)
            throws Exception {
        ExecutorService ejecutor = Executors.newFixedThreadPool(tareas.size());
        CountDownLatch listos = new CountDownLatch(tareas.size());
        CountDownLatch salida = new CountDownLatch(1);
        List<Future<Resultado>> futuros = new ArrayList<>();

        try {
            for (Callable<Resultado> tarea : tareas) {
                futuros.add(ejecutor.submit(() -> {
                    listos.countDown();
                    if (!salida.await(ESPERA_SINCRONIZACION_SEGUNDOS, TimeUnit.SECONDS)) {
                        throw new IllegalStateException(
                                "Las tareas no se sincronizaron a tiempo");
                    }
                    return tarea.call();
                }));
            }

            assertThat(listos.await(ESPERA_SINCRONIZACION_SEGUNDOS, TimeUnit.SECONDS))
                    .as("Todas las tareas deben estar listas antes de iniciar")
                    .isTrue();

            salida.countDown();

            List<Resultado> resultados = new ArrayList<>();
            for (Future<Resultado> futuro : futuros) {
                resultados.add(futuro.get(ESPERA_RESULTADO_SEGUNDOS, TimeUnit.SECONDS));
            }
            return resultados;
        } finally {
            salida.countDown();
            ejecutor.shutdownNow();
            ejecutor.awaitTermination(5, TimeUnit.SECONDS);
        }
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

    private static long contarEstado(List<Resultado> resultados, int status) {
        return resultados.stream().filter(r -> r.status() == status).count();
    }

    @Test
    @DisplayName("1. Dos compras concurrentes cuya suma supera la disponibilidad")
    void dosComprasConcurrentesQueSuperanLaDisponibilidad() throws Exception {
        Long funcionId = crearFuncion("15000.00", 10);
        MockMvc clienteA = nuevoMockMvc();
        MockMvc clienteB = nuevoMockMvc();

        List<Resultado> resultados = ejecutarConcurrentemente(List.of(
                () -> comprar(clienteA, funcionId, "Cliente A", 7),
                () -> comprar(clienteB, funcionId, "Cliente B", 7)));

        assertThat(contarEstado(resultados, 201)).isEqualTo(1);
        assertThat(contarEstado(resultados, 409)).isEqualTo(1);
        assertThat(cuposDisponibles(funcionId)).isEqualTo(3);
        assertThat(contarCompras(funcionId)).isEqualTo(1);
        assertThat(boletosVendidos(funcionId)).isEqualTo(7);
    }

    @Test
    @DisplayName("2. Dos compras concurrentes que pueden completarse correctamente")
    void dosComprasConcurrentesQueCaben() throws Exception {
        Long funcionId = crearFuncion("15000.00", 10);
        MockMvc clienteA = nuevoMockMvc();
        MockMvc clienteB = nuevoMockMvc();

        List<Resultado> resultados = ejecutarConcurrentemente(List.of(
                () -> comprar(clienteA, funcionId, "Cliente A", 3),
                () -> comprar(clienteB, funcionId, "Cliente B", 3)));

        assertThat(contarEstado(resultados, 201)).isEqualTo(2);
        assertThat(cuposDisponibles(funcionId)).isEqualTo(4);
        assertThat(contarCompras(funcionId)).isEqualTo(2);
        assertThat(boletosVendidos(funcionId)).isEqualTo(6);
    }

    @Test
    @DisplayName("3. Múltiples solicitudes concurrentes hasta agotar los cupos")
    void multiplesSolicitudesHastaAgotarCupos() throws Exception {
        Long funcionId = crearFuncion("15000.00", 10);
        List<Callable<Resultado>> tareas = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            MockMvc cliente = nuevoMockMvc();
            int indice = i;
            tareas.add(() -> comprar(cliente, funcionId, "Cliente " + indice, 2));
        }

        List<Resultado> resultados = ejecutarConcurrentemente(tareas);

        assertThat(contarEstado(resultados, 201)).isEqualTo(5);
        assertThat(contarEstado(resultados, 409)).isEqualTo(3);
        assertThat(cuposDisponibles(funcionId)).isZero();
        assertThat(contarCompras(funcionId)).isEqualTo(5);
        assertThat(boletosVendidos(funcionId)).isEqualTo(10);
    }

    @Test
    @DisplayName("4. Los cupos nunca quedan negativos tras ráfagas concurrentes")
    void cuposNuncaNegativos() throws Exception {
        Long funcionId = crearFuncion("15000.00", 7);
        List<Callable<Resultado>> tareas = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            MockMvc cliente = nuevoMockMvc();
            int indice = i;
            tareas.add(() -> comprar(cliente, funcionId, "Cliente " + indice, 1));
        }

        List<Resultado> primeraRafaga = ejecutarConcurrentemente(tareas);

        assertThat(contarEstado(primeraRafaga, 201)).isEqualTo(7);
        assertThat(contarEstado(primeraRafaga, 409)).isEqualTo(3);
        assertThat(cuposDisponibles(funcionId)).isZero();

        List<Callable<Resultado>> segundaRafaga = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            MockMvc cliente = nuevoMockMvc();
            int indice = i;
            segundaRafaga.add(() -> comprar(cliente, funcionId, "Tarde " + indice, 1));
        }

        List<Resultado> resultadosTarde = ejecutarConcurrentemente(segundaRafaga);

        assertThat(contarEstado(resultadosTarde, 409)).isEqualTo(3);
        assertThat(cuposDisponibles(funcionId)).isZero();
        assertThat(contarCompras(funcionId)).isEqualTo(7);
    }

    @Test
    @DisplayName("5. Los boletos vendidos coinciden con el descuento de cupos")
    void boletosVendidosCoincidenConDescuento() throws Exception {
        Long funcionId = crearFuncion("15000.00", 12);
        List<Callable<Resultado>> tareas = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            MockMvc cliente = nuevoMockMvc();
            int indice = i;
            tareas.add(() -> comprar(cliente, funcionId, "Cliente " + indice, 2));
        }

        List<Resultado> resultados = ejecutarConcurrentemente(tareas);

        assertThat(contarEstado(resultados, 201)).isEqualTo(5);
        assertThat(cuposDisponibles(funcionId)).isEqualTo(2);
        assertThat(boletosVendidos(funcionId)).isEqualTo(10);

        Integer totales = jdbcTemplate.queryForObject(
                "SELECT cupos_totales FROM funciones WHERE id = ?", Integer.class, funcionId);
        assertThat(totales - cuposDisponibles(funcionId)).isEqualTo(boletosVendidos(funcionId));
    }

    @Test
    @DisplayName("6. Consistencia frente a una actualización concurrente de la capacidad")
    void actualizacionConcurrenteDeCapacidad() throws Exception {
        Long funcionId = crearFuncion("15000.00", 100);
        Long peliculaId = jdbcTemplate.queryForObject(
                "SELECT pelicula_id FROM funciones WHERE id = ?", Long.class, funcionId);
        MockMvc cliente = nuevoMockMvc();
        MockMvc taquilla = nuevoMockMvc();

        List<Resultado> resultados = ejecutarConcurrentemente(List.of(
                () -> comprar(cliente, funcionId, "Cliente A", 10),
                () -> actualizarCapacidad(taquilla, funcionId, peliculaId, 120)));

        assertThat(contarEstado(resultados, 201)).isEqualTo(1);
        assertThat(contarEstado(resultados, 200)).isEqualTo(1);

        Integer totales = jdbcTemplate.queryForObject(
                "SELECT cupos_totales FROM funciones WHERE id = ?", Integer.class, funcionId);
        assertThat(totales).isEqualTo(120);
        assertThat(cuposDisponibles(funcionId)).isEqualTo(110);
        assertThat(contarCompras(funcionId)).isEqualTo(1);
        assertThat(boletosVendidos(funcionId)).isEqualTo(10);
    }

    @Test
    @DisplayName("7. Consistencia frente a una eliminación concurrente de la función")
    void eliminacionConcurrenteDeFuncion() throws Exception {
        Long funcionId = crearFuncion("15000.00", 20);
        MockMvc cliente = nuevoMockMvc();
        MockMvc taquilla = nuevoMockMvc();

        List<Resultado> resultados = ejecutarConcurrentemente(List.of(
                () -> comprar(cliente, funcionId, "Cliente A", 2),
                () -> eliminarFuncion(taquilla, funcionId)));

        int estadoCompra = resultados.get(0).status();
        int estadoEliminacion = resultados.get(1).status();

        boolean ganoLaCompra = estadoCompra == 201 && estadoEliminacion == 409;
        boolean ganoLaEliminacion = estadoCompra == 404 && estadoEliminacion == 204;

        assertThat(ganoLaCompra || ganoLaEliminacion)
                .as("Resultados inesperados: compra=%s, eliminación=%s",
                        estadoCompra, estadoEliminacion)
                .isTrue();

        if (ganoLaCompra) {
            assertThat(cuposDisponibles(funcionId)).isEqualTo(18);
            assertThat(contarCompras(funcionId)).isEqualTo(1);
            assertThat(boletosVendidos(funcionId)).isEqualTo(2);
        } else {
            Integer funciones = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM funciones WHERE id = ?", Integer.class, funcionId);
            assertThat(funciones).isZero();
            assertThat(contarCompras(funcionId)).isZero();
        }
    }
}
