package com.taquillacine.api.service;

import com.taquillacine.api.exception.RecursoNoEncontradoException;
import com.taquillacine.api.model.Pelicula;
import com.taquillacine.api.repository.PeliculaRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PeliculaService {

    private final PeliculaRepository peliculaRepository;

    public PeliculaService(PeliculaRepository peliculaRepository) {
        this.peliculaRepository = peliculaRepository;
    }

    public List<Pelicula> listarTodas() {
        return peliculaRepository.findAll();
    }

    public Pelicula obtenerPorId(Long id) {
        return peliculaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Película no encontrada con id: " + id));
    }

    public Pelicula crear(Pelicula pelicula) {
        return peliculaRepository.save(pelicula);
    }

    public Pelicula actualizar(Long id, Pelicula pelicula) {
        Pelicula existente = obtenerPorId(id);
        existente.setTitulo(pelicula.getTitulo());
        existente.setGenero(pelicula.getGenero());
        existente.setDuracionMin(pelicula.getDuracionMin());
        existente.setClasificacion(pelicula.getClasificacion());

        int filas = peliculaRepository.update(existente);
        if (filas == 0) {
            throw new RecursoNoEncontradoException("Película no encontrada con id: " + id);
        }
        return existente;
    }

    public void eliminar(Long id) {
        obtenerPorId(id); // Verifica existencia

        // Si existen funciones asociadas, PostgreSQL rechaza el DELETE con
        // SQLSTATE 23503 y GlobalExceptionHandler responde 409 con un mensaje
        // controlado, sin exponer detalles internos del motor.
        int filas = peliculaRepository.deleteById(id);
        if (filas == 0) {
            throw new RecursoNoEncontradoException("Película no encontrada con id: " + id);
        }
    }
}