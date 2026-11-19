package com.taquillacine.api.repository;

import com.taquillacine.api.model.Funcion;
import com.taquillacine.api.model.FuncionConTitulo;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public class FuncionRepository {

    private static final RowMapper<Funcion> FUNCION_ROW_MAPPER = new RowMapper<Funcion>() {
        @Override
        public Funcion mapRow(ResultSet rs, int rowNum) throws SQLException {
            Funcion funcion = new Funcion();
            funcion.setId(rs.getLong("id"));
            funcion.setPeliculaId(rs.getLong("pelicula_id"));
            funcion.setFecha(rs.getObject("fecha", LocalDate.class));
            funcion.setHora(rs.getObject("hora", LocalTime.class));
            funcion.setSala(rs.getString("sala"));
            funcion.setPrecio(rs.getBigDecimal("precio"));
            funcion.setCuposTotales(rs.getInt("cupos_totales"));
            funcion.setCuposDisponibles(rs.getInt("cupos_disponibles"));
            return funcion;
        }
    };

    private static final RowMapper<FuncionConTitulo> FUNCION_CON_TITULO_ROW_MAPPER =
            new RowMapper<FuncionConTitulo>() {
                @Override
                public FuncionConTitulo mapRow(ResultSet rs, int rowNum) throws SQLException {
                    return new FuncionConTitulo(
                            FUNCION_ROW_MAPPER.mapRow(rs, rowNum),
                            rs.getString("titulo_pelicula"));
                }
            };

    private final JdbcTemplate jdbcTemplate;

    public FuncionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<FuncionConTitulo> findAllConTitulo() {
        String sql = """
                SELECT f.id, f.pelicula_id, f.fecha, f.hora, f.sala, f.precio,
                       f.cupos_totales, f.cupos_disponibles,
                       p.titulo AS titulo_pelicula
                FROM funciones f
                JOIN peliculas p ON p.id = f.pelicula_id
                ORDER BY f.fecha ASC, f.hora ASC, f.id ASC
                """;
        return jdbcTemplate.query(sql, FUNCION_CON_TITULO_ROW_MAPPER);
    }

    public Optional<FuncionConTitulo> findByIdConTitulo(Long id) {
        String sql = """
                SELECT f.id, f.pelicula_id, f.fecha, f.hora, f.sala, f.precio,
                       f.cupos_totales, f.cupos_disponibles,
                       p.titulo AS titulo_pelicula
                FROM funciones f
                JOIN peliculas p ON p.id = f.pelicula_id
                WHERE f.id = ?
                """;
        List<FuncionConTitulo> resultado = jdbcTemplate.query(sql, FUNCION_CON_TITULO_ROW_MAPPER, id);
        return resultado.isEmpty() ? Optional.empty() : Optional.of(resultado.get(0));
    }

    public Optional<Funcion> findById(Long id) {
        String sql = """
                SELECT id, pelicula_id, fecha, hora, sala, precio,
                       cupos_totales, cupos_disponibles
                FROM funciones
                WHERE id = ?
                """;
        List<Funcion> resultado = jdbcTemplate.query(sql, FUNCION_ROW_MAPPER, id);
        return resultado.isEmpty() ? Optional.empty() : Optional.of(resultado.get(0));
    }

    /**
     * Recupera la función bloqueando su fila hasta que termine la transacción.
     * Se utiliza en actualizaciones y eliminaciones para evitar condiciones de carrera
     * con futuras compras concurrentes.
     */
    public Optional<Funcion> findByIdForUpdate(Long id) {
        String sql = """
                SELECT id, pelicula_id, fecha, hora, sala, precio,
                       cupos_totales, cupos_disponibles
                FROM funciones
                WHERE id = ?
                FOR UPDATE
                """;
        List<Funcion> resultado = jdbcTemplate.query(sql, FUNCION_ROW_MAPPER, id);
        return resultado.isEmpty() ? Optional.empty() : Optional.of(resultado.get(0));
    }

    public Funcion save(Funcion funcion) {
        String sql = """
                INSERT INTO funciones
                    (pelicula_id, fecha, hora, sala, precio, cupos_totales, cupos_disponibles)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, new String[]{"id"});
            ps.setLong(1, funcion.getPeliculaId());
            ps.setObject(2, funcion.getFecha());
            ps.setObject(3, funcion.getHora());
            ps.setString(4, funcion.getSala());
            ps.setBigDecimal(5, funcion.getPrecio());
            ps.setInt(6, funcion.getCuposTotales());
            ps.setInt(7, funcion.getCuposDisponibles());
            return ps;
        }, keyHolder);

        Number generatedId = keyHolder.getKey();
        funcion.setId(generatedId != null ? generatedId.longValue() : null);
        return funcion;
    }

    /**
     * Descuenta cupos disponibles de forma segura. La condición
     * {@code cupos_disponibles >= cantidad} evita que una actualización deje
     * valores negativos incluso si se invoca sin haber bloqueado la fila antes.
     * Se espera que el llamador ya haya validado la disponibilidad dentro de la
     * misma transacción; el número de filas afectadas permite comprobarlo.
     */
    public int descontarCupos(Long funcionId, int cantidad) {
        String sql = """
                UPDATE funciones
                SET cupos_disponibles = cupos_disponibles - ?
                WHERE id = ? AND cupos_disponibles >= ?
                """;
        return jdbcTemplate.update(sql, cantidad, funcionId, cantidad);
    }

    public int update(Funcion funcion) {
        String sql = """
                UPDATE funciones
                SET pelicula_id = ?, fecha = ?, hora = ?, sala = ?, precio = ?,
                    cupos_totales = ?, cupos_disponibles = ?
                WHERE id = ?
                """;
        return jdbcTemplate.update(sql,
                funcion.getPeliculaId(),
                funcion.getFecha(),
                funcion.getHora(),
                funcion.getSala(),
                funcion.getPrecio(),
                funcion.getCuposTotales(),
                funcion.getCuposDisponibles(),
                funcion.getId());
    }

    public int deleteById(Long id) {
        String sql = "DELETE FROM funciones WHERE id = ?";
        return jdbcTemplate.update(sql, id);
    }

    public boolean existePelicula(Long peliculaId) {
        String sql = "SELECT EXISTS (SELECT 1 FROM peliculas WHERE id = ?)";
        Boolean existe = jdbcTemplate.queryForObject(sql, Boolean.class, peliculaId);
        return Boolean.TRUE.equals(existe);
    }

    public int contarCompras(Long funcionId) {
        String sql = "SELECT COUNT(*) FROM compras WHERE funcion_id = ?";
        Integer total = jdbcTemplate.queryForObject(sql, Integer.class, funcionId);
        return total != null ? total : 0;
    }
}
