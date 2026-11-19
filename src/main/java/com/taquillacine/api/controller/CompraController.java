package com.taquillacine.api.controller;

import com.taquillacine.api.dto.CompraRequest;
import com.taquillacine.api.dto.CompraResponse;
import com.taquillacine.api.model.Compra;
import com.taquillacine.api.service.CompraService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Compras de boletos.
 *
 * <p>Solo se exponen el listado y el registro. Las compras no admiten
 * devoluciones, modificaciones ni eliminaciones, por lo que no existen
 * endpoints PUT, PATCH o DELETE.</p>
 */
@RestController
@RequestMapping("/api/compras")
public class CompraController {

    private final CompraService compraService;

    public CompraController(CompraService compraService) {
        this.compraService = compraService;
    }

    @GetMapping
    public ResponseEntity<List<CompraResponse>> listarTodas() {
        List<CompraResponse> response = compraService.listarTodas().stream()
                .map(CompraResponse::from)
                .toList();
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<CompraResponse> registrar(@Valid @RequestBody CompraRequest request) {
        Compra compra = compraService.registrar(
                request.getFuncionId(), request.getNombreCliente(), request.getCantidadBoletos());
        return ResponseEntity.status(201).body(CompraResponse.from(compra));
    }
}
