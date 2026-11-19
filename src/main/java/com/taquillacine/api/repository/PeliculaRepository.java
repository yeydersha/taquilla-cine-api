package com.taquillacine.api.repository;

import com.taquillacine.api.model.Pelicula;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
public class PeliculaRepository {

    private final JdbcTemplate jdbcTemplate;

    public PeliculaRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final RowMapper<Pelicula> PELICULA_ROW_MAPPER = new RowMapper<Pelicula>() {
        @Override
        public Pelicula mapRow(ResultSet rs, int rowNum) throws SQLException {
            Pelicula pelicula = new Pelicula();
            pelicula.setId(rs.getLong("id"));
            pelicula.setTitulo(rs.getString("titulo"));
            pelicula.setGenero(rs.getString("genero"));
            pelicula.setDuracionMin(rs.getInt("duracion_min"));
            pelicula.setClasificacion(rs.getString("clasificacion"));
            return pelicula;
        }
    };

    public List<Pelicula> findAll() {
        String sql = "SELECT id, titulo, genero, duracion_min, clasificacion FROM peliculas ORDER BY id ASC";
        return jdbcTemplate.query(sql, PELICULA_ROW_MAPPER);
    }

    public Optional<Pelicula> findById(Long id) {
        String sql = "SELECT id, titulo, genero, duracion_min, clasificacion FROM peliculas WHERE id = ?";
        List<Pelicula> result = jdbcTemplate.query(sql, PELICULA_ROW_MAPPER, id);
        return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
    }

    public Pelicula save(Pelicula pelicula) {
        String sql = "INSERT INTO peliculas (titulo, genero, duracion_min, clasificacion) VALUES (?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, new String[]{"id"});
            ps.setString(1, pelicula.getTitulo());
            ps.setString(2, pelicula.getGenero());
            ps.setInt(3, pelicula.getDuracionMin());
            ps.setString(4, pelicula.getClasificacion());
            return ps;
        }, keyHolder);

        Long generatedId = keyHolder.getKeyAs(Long.class);
        pelicula.setId(generatedId);
        return pelicula;
    }

    public int update(Pelicula pelicula) {
        String sql = "UPDATE peliculas SET titulo = ?, genero = ?, duracion_min = ?, clasificacion = ? WHERE id = ?";
        return jdbcTemplate.update(sql,
                pelicula.getTitulo(),
                pelicula.getGenero(),
                pelicula.getDuracionMin(),
                pelicula.getClasificacion(),
                pelicula.getId());
    }

    public int deleteById(Long id) {
        String sql = "DELETE FROM peliculas WHERE id = ?";
        return jdbcTemplate.update(sql, id);
    }

    }