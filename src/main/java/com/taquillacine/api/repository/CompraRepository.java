package com.taquillacine.api.repository;

import com.taquillacine.api.model.Compra;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Acceso a la tabla {@code compras}.
 *
 * <p>Las compras son inmutables desde la API: solo se consultan y se registran.
 * No se implementan actualizaciones ni eliminaciones.</p>
 */
@Repository
public class CompraRepository {

    private static final RowMapper<Compra> COMPRA_ROW_MAPPER = new RowMapper<Compra>() {
        @Override
        public Compra mapRow(ResultSet rs, int rowNum) throws SQLException {
            Compra compra = new Compra();
            compra.setId(rs.getLong("id"));
            compra.setFuncionId(rs.getLong("funcion_id"));
            compra.setNombreCliente(rs.getString("nombre_cliente"));
            compra.setCantidadBoletos(rs.getInt("cantidad_boletos"));
            compra.setTotal(rs.getBigDecimal("total"));
            compra.setFechaCompra(rs.getObject("fecha_compra", OffsetDateTime.class));
            return compra;
        }
    };

    private final JdbcTemplate jdbcTemplate;

    public CompraRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Devuelve las compras desde la más reciente hasta la más antigua.
     * El ID se usa como criterio secundario para garantizar un orden estable
     * cuando dos compras comparten la misma marca de tiempo.
     */
    public List<Compra> findAll() {
        String sql = """
                SELECT id, funcion_id, nombre_cliente, cantidad_boletos, total, fecha_compra
                FROM compras
                ORDER BY fecha_compra DESC, id DESC
                """;
        return jdbcTemplate.query(sql, COMPRA_ROW_MAPPER);
    }

    /**
     * Registra una compra y recupera el ID generado por PostgreSQL.
     */
    public Compra save(Compra compra) {
        String sql = """
                INSERT INTO compras
                    (funcion_id, nombre_cliente, cantidad_boletos, total, fecha_compra)
                VALUES (?, ?, ?, ?, ?)
                """;
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, new String[]{"id"});
            ps.setLong(1, compra.getFuncionId());
            ps.setString(2, compra.getNombreCliente());
            ps.setInt(3, compra.getCantidadBoletos());
            ps.setBigDecimal(4, compra.getTotal());
            ps.setObject(5, compra.getFechaCompra());
            return ps;
        }, keyHolder);

        Number generatedId = keyHolder.getKey();
        compra.setId(generatedId != null ? generatedId.longValue() : null);
        return compra;
    }
}
