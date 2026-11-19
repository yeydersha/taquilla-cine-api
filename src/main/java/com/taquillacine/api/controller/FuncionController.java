package com.taquillacine.api.controller;

import com.taquillacine.api.dto.FuncionRequest;
import com.taquillacine.api.dto.FuncionResponse;
import com.taquillacine.api.model.Funcion;
import com.taquillacine.api.model.FuncionConTitulo;
import com.taquillacine.api.service.FuncionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/funciones")
public class FuncionController {

    private final FuncionService funcionService;

    public FuncionController(FuncionService funcionService) {
        this.funcionService = funcionService;
    }

    @GetMapping
    public ResponseEntity<List<FuncionResponse>> listarTodas() {
        List<FuncionResponse> response = funcionService.listarTodas().stream()
                .map(this::aRespuesta)
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<FuncionResponse> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(aRespuesta(funcionService.obtenerPorId(id)));
    }

    @PostMapping
    public ResponseEntity<FuncionResponse> crear(@Valid @RequestBody FuncionRequest request) {
        FuncionConTitulo creada = funcionService.crear(aModelo(request));

        URI location = URI.create("/api/funciones/" + creada.funcion().getId());
        return ResponseEntity.created(location).body(aRespuesta(creada));
    }

    @PutMapping("/{id}")
    public ResponseEntity<FuncionResponse> actualizar(
            @PathVariable Long id, @Valid @RequestBody FuncionRequest request) {
        FuncionConTitulo actualizada = funcionService.actualizar(id, aModelo(request));
        return ResponseEntity.ok(aRespuesta(actualizada));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        funcionService.eliminar(id);
    }

    private Funcion aModelo(FuncionRequest request) {
        Funcion funcion = new Funcion();
        funcion.setPeliculaId(request.getPeliculaId());
        funcion.setFecha(request.getFecha());
        funcion.setHora(request.getHora());
        funcion.setSala(request.getSala());
        funcion.setPrecio(request.getPrecio());
        funcion.setCuposTotales(request.getCuposTotales());
        return funcion;
    }

    private FuncionResponse aRespuesta(FuncionConTitulo resultado) {
        return FuncionResponse.from(resultado.funcion(), resultado.tituloPelicula());
    }
}
