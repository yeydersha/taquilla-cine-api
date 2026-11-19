package com.taquillacine.api.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;

/**
 * Modelo interno de lectura para una venta del reporte.
 *
 * <p>Se obtiene del JOIN entre {@code compras}, {@code funciones} y
 * {@code peliculas}. El importe se toma del total almacenado en la compra,
 * no del precio actual de la función.</p>
 */
public record DetalleVenta(
        Long id,
        Long funcionId,
        String tituloPelicula,
        LocalDate fechaFuncion,
        LocalTime horaFuncion,
        String sala,
        String nombreCliente,
        Integer cantidadBoletos,
        BigDecimal total,
        OffsetDateTime fechaCompra) {
}
