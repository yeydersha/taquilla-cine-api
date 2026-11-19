package com.taquillacine.api.repository;

import com.taquillacine.api.model.DetalleVenta;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Consultas de solo lectura para el reporte de ventas.
 *
 * <p>No ejecuta inserciones, actualizaciones ni eliminaciones.</p>
 */
@Repository
public class ReporteRepository {

    private static final RowMapper<DetalleVenta> DETALLE_VENTA_ROW_MAPPER = (rs, rowNum) ->
            new DetalleVenta(
                    rs.getLong("id"),
                    rs.getLong("funcion_id"),
                    rs.getString("titulo_pelicula"),
                    rs.getObject("fecha_funcion", LocalDate.class),
                    rs.getObject("hora_funcion", LocalTime.class),
                    rs.getString("sala"),
                    rs.getString("nombre_cliente"),
                    rs.getInt("cantidad_boletos"),
                    rs.getBigDecimal("total"),
                    rs.getObject("fecha_compra", OffsetDateTime.class)
            );

    private final JdbcTemplate jdbcTemplate;

    public ReporteRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Recupera las ventas cuyo {@code fecha_compra} cae en el intervalo
     * semiabierto {@code [desde, hastaExclusivo)}.
     *
     * <p>No se usa {@code DATE(fecha_compra)} para filtrar: la comparación
     * directa contra la columna {@code TIMESTAMPTZ} mantiene la semántica de
     * instantes y permite aprovechar el índice existente sobre
     * {@code fecha_compra}.</p>
     */
    public List<DetalleVenta> findVentas(OffsetDateTime desde, OffsetDateTime hastaExclusivo) {
        String sql = """
                SELECT c.id,
                       c.funcion_id,
                       p.titulo AS titulo_pelicula,
                       f.fecha AS fecha_funcion,
                       f.hora AS hora_funcion,
                       f.sala,
                       c.nombre_cliente,
                       c.cantidad_boletos,
                       c.total,
                       c.fecha_compra
                FROM compras c
                JOIN funciones f ON f.id = c.funcion_id
                JOIN peliculas p ON p.id = f.pelicula_id
                WHERE c.fecha_compra >= ?
                  AND c.fecha_compra < ?
                ORDER BY c.fecha_compra DESC, c.id DESC
                """;
        return jdbcTemplate.query(sql, DETALLE_VENTA_ROW_MAPPER, desde, hastaExclusivo);
    }
}
