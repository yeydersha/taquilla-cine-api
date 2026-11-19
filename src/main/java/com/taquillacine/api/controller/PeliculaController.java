package com.taquillacine.api.controller;

import com.taquillacine.api.dto.PeliculaRequest;
import com.taquillacine.api.dto.PeliculaResponse;
import com.taquillacine.api.model.Pelicula;
import com.taquillacine.api.service.PeliculaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/peliculas")
public class PeliculaController {

    private final PeliculaService peliculaService;

    public PeliculaController(PeliculaService peliculaService) {
        this.peliculaService = peliculaService;
    }

    @GetMapping
    public ResponseEntity<List<PeliculaResponse>> listarTodas() {
        List<Pelicula> peliculas = peliculaService.listarTodas();
        List<PeliculaResponse> response = peliculas.stream()
                .map(PeliculaResponse::from)
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PeliculaResponse> obtenerPorId(@PathVariable Long id) {
        Pelicula pelicula = peliculaService.obtenerPorId(id);
        return ResponseEntity.ok(PeliculaResponse.from(pelicula));
    }

    @PostMapping
    public ResponseEntity<PeliculaResponse> crear(@Valid @RequestBody PeliculaRequest request) {
        Pelicula pelicula = new Pelicula();
        pelicula.setTitulo(request.getTitulo());
        pelicula.setGenero(request.getGenero());
        pelicula.setDuracionMin(request.getDuracionMin());
        pelicula.setClasificacion(request.getClasificacion());

        Pelicula creada = peliculaService.crear(pelicula);
        PeliculaResponse response = PeliculaResponse.from(creada);

        URI location = URI.create("/api/peliculas/" + creada.getId());
        return ResponseEntity.created(location).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PeliculaResponse> actualizar(@PathVariable Long id, @Valid @RequestBody PeliculaRequest request) {
        Pelicula pelicula = new Pelicula();
        pelicula.setId(id);
        pelicula.setTitulo(request.getTitulo());
        pelicula.setGenero(request.getGenero());
        pelicula.setDuracionMin(request.getDuracionMin());
        pelicula.setClasificacion(request.getClasificacion());

        Pelicula actualizada = peliculaService.actualizar(id, pelicula);
        return ResponseEntity.ok(PeliculaResponse.from(actualizada));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        peliculaService.eliminar(id);
    }
}