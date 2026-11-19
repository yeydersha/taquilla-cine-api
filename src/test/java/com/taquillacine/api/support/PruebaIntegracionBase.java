package com.taquillacine.api.support;

import com.taquillacine.api.model.Pelicula;
import com.taquillacine.api.repository.PeliculaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Base para las pruebas de integración.
 *
 * <p>No elimina datos de forma masiva. Cada prueba registra los identificadores
 * que crea y la limpieza se realiza únicamente sobre esos identificadores, en
 * orden inverso a las dependencias (compras -> funciones -> películas).</p>
 */
@SpringBootTest
@ActiveProfiles("test")
public abstract class PruebaIntegracionBase {

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Autowired
    protected PeliculaRepository peliculaRepository;

    @Autowired
    protected WebApplicationContext webApplicationContext;

    @Autowired
    protected JsonMapper objectMapper;

    protected MockMvc mockMvc;

    private final List<Long> peliculasCreadas = new ArrayList<>();
    private final List<Long> funcionesCreadas = new ArrayList<>();

    @BeforeEach
    void configurarEntornoDePrueba() {
        GuardiaBaseDatosPruebas.verificarEntornoAutorizado(jdbcTemplate);
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @AfterEach
    void limpiarRegistrosCreadosPorLaPrueba() {
        for (Long funcionId : funcionesCreadas) {
            jdbcTemplate.update("DELETE FROM compras WHERE funcion_id = ?", funcionId);
        }
        for (Long funcionId : funcionesCreadas) {
            jdbcTemplate.update("DELETE FROM funciones WHERE id = ?", funcionId);
        }
        for (Long peliculaId : peliculasCreadas) {
            jdbcTemplate.update("DELETE FROM peliculas WHERE id = ?", peliculaId);
        }
        funcionesCreadas.clear();
        peliculasCreadas.clear();
    }

    protected Pelicula crearPelicula(String titulo, String genero, int duracionMin, String clasificacion) {
        Pelicula guardada = peliculaRepository.save(
                new Pelicula(null, titulo, genero, duracionMin, clasificacion));
        registrarPelicula(guardada.getId());
        return guardada;
    }

    protected void registrarPelicula(Long id) {
        if (id != null) {
            peliculasCreadas.add(id);
        }
    }

    protected Long insertarFuncion(Long peliculaId, LocalDate fecha, LocalTime hora, String sala,
                                   BigDecimal precio, int cuposTotales, int cuposDisponibles) {
        String sql = """
                INSERT INTO funciones
                    (pelicula_id, fecha, hora, sala, precio, cupos_totales, cupos_disponibles)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                RETURNING id
                """;
        Long id = jdbcTemplate.queryForObject(sql, Long.class, peliculaId, fecha, hora, sala,
                precio, cuposTotales, cuposDisponibles);
        registrarFuncion(id);
        return id;
    }

    protected void registrarFuncion(Long id) {
        if (id != null) {
            funcionesCreadas.add(id);
        }
    }

    protected Long insertarCompra(Long funcionId, int cantidadBoletos, BigDecimal total) {
        String sql = """
                INSERT INTO compras
                    (funcion_id, nombre_cliente, cantidad_boletos, total, fecha_compra)
                VALUES (?, ?, ?, ?, now())
                RETURNING id
                """;
        return jdbcTemplate.queryForObject(sql, Long.class, funcionId,
                "Cliente de prueba", cantidadBoletos, total);
    }
}
