package com.taquillacine.api.service;

import com.taquillacine.api.exception.ConflictoIntegridadException;
import com.taquillacine.api.exception.CuposInsuficientesException;
import com.taquillacine.api.exception.RecursoNoEncontradoException;
import com.taquillacine.api.exception.TotalFueraDeRangoException;
import com.taquillacine.api.model.Compra;
import com.taquillacine.api.model.Funcion;
import com.taquillacine.api.repository.CompraRepository;
import com.taquillacine.api.repository.FuncionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class CompraService {

    /**
     * Máximo valor representable por {@code NUMERIC(12,2)}: 10 dígitos enteros
     * y 2 decimales. Se valida antes de enviar el total a PostgreSQL para
     * producir un error controlado en lugar de una violación de rango.
     */
    private static final BigDecimal TOTAL_MAXIMO = new BigDecimal("9999999999.99");
    private static final int ESCALA_MONETARIA = 2;

    private final CompraRepository compraRepository;
    private final FuncionRepository funcionRepository;

    public CompraService(CompraRepository compraRepository, FuncionRepository funcionRepository) {
        this.compraRepository = compraRepository;
        this.funcionRepository = funcionRepository;
    }

    public List<Compra> listarTodas() {
        return compraRepository.findAll();
    }

    /**
     * Registra una compra dentro de una única transacción.
     *
     * <p>La fila de la función se bloquea con {@code SELECT ... FOR UPDATE} antes
     * de leer los cupos y el precio. El bloqueo se mantiene hasta el commit, de
     * modo que dos compras concurrentes sobre la misma función se serializan y
     * no pueden sobrevender. El mismo bloqueo es utilizado por
     * {@code FuncionService} al actualizar y eliminar funciones.</p>
     *
     * <p>Si cualquier paso falla, la excepción no controlada revierte la
     * transacción completa: no quedan compras parciales ni descuentos de cupos.</p>
     */
    @Transactional
    public Compra registrar(Long funcionId, String nombreCliente, int cantidadBoletos) {
        Funcion funcion = funcionRepository.findByIdForUpdate(funcionId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Función no encontrada con id: " + funcionId));

        int disponibles = funcion.getCuposDisponibles();
        if (cantidadBoletos > disponibles) {
            throw new CuposInsuficientesException(
                    "Se solicitaron " + cantidadBoletos + " boletos, pero solo hay "
                            + disponibles + " disponibles");
        }

        BigDecimal total = funcion.getPrecio()
                .multiply(BigDecimal.valueOf(cantidadBoletos))
                .setScale(ESCALA_MONETARIA, RoundingMode.HALF_UP);

        if (total.compareTo(TOTAL_MAXIMO) > 0) {
            throw new TotalFueraDeRangoException(
                    "El total calculado (" + total.toPlainString()
                            + ") excede el máximo permitido por la base de datos ("
                            + TOTAL_MAXIMO.toPlainString() + ")");
        }

        Compra compra = new Compra();
        compra.setFuncionId(funcionId);
        compra.setNombreCliente(nombreCliente);
        compra.setCantidadBoletos(cantidadBoletos);
        compra.setTotal(total);
        // PostgreSQL TIMESTAMPTZ almacena microsegundos. Truncar antes del INSERT
        // evita que la respuesta difiera del valor realmente persistido.
        compra.setFechaCompra(OffsetDateTime.now().truncatedTo(ChronoUnit.MICROS));

        Compra guardada = compraRepository.save(compra);

        int filas = funcionRepository.descontarCupos(funcionId, cantidadBoletos);
        if (filas != 1) {
            throw new ConflictoIntegridadException(
                    "No se pudo descontar los cupos de la función " + funcionId);
        }

        return guardada;
    }
}
