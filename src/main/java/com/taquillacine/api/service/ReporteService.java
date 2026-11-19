package com.taquillacine.api.service;

import com.taquillacine.api.dto.DetalleVentaResponse;
import com.taquillacine.api.dto.ReporteVentasResponse;
import com.taquillacine.api.exception.RangoFechasInvalidoException;
import com.taquillacine.api.model.DetalleVenta;
import com.taquillacine.api.repository.ReporteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
public class ReporteService {

    /**
     * Zona horaria de negocio del reporte. Es independiente de la zona horaria
     * del servidor Java y de la sesión de PostgreSQL.
     */
    public static final ZoneId ZONA_REPORTE = ZoneId.of("America/Bogota");

    private static final BigDecimal CERO_MONETARIO = new BigDecimal("0.00");

    private final ReporteRepository reporteRepository;

    public ReporteService(ReporteRepository reporteRepository) {
        this.reporteRepository = reporteRepository;
    }

    /**
     * Genera el reporte de ventas para el rango inclusivo de días indicado.
     *
     * <p>El filtro se traduce a un intervalo semiabierto en America/Bogota:
     * desde {@code fechaDesde 00:00:00} hasta {@code fechaHasta + 1 día 00:00:00}.
     * Los totales se calculan sobre la misma lista de ventas devuelta, de modo
     * que los indicadores siempre coinciden con el detalle, incluso si hay
     * compras concurrentes.</p>
     */
    @Transactional(readOnly = true)
    public ReporteVentasResponse generar(LocalDate fechaDesde, LocalDate fechaHasta) {
        if (fechaDesde == null || fechaHasta == null) {
            throw new RangoFechasInvalidoException(
                    "Las fechas fechaDesde y fechaHasta son obligatorias");
        }
        if (fechaDesde.isAfter(fechaHasta)) {
            throw new RangoFechasInvalidoException(
                    "La fecha inicial (" + fechaDesde + ") no puede ser posterior a la fecha final ("
                            + fechaHasta + ")");
        }

        OffsetDateTime desde = fechaDesde.atStartOfDay(ZONA_REPORTE).toOffsetDateTime();
        OffsetDateTime hastaExclusivo = fechaHasta.plusDays(1)
                .atStartOfDay(ZONA_REPORTE)
                .toOffsetDateTime();

        List<DetalleVenta> ventas = reporteRepository.findVentas(desde, hastaExclusivo);

        long totalBoletos = 0;
        BigDecimal totalRecaudado = CERO_MONETARIO;
        List<DetalleVentaResponse> compras = new ArrayList<>(ventas.size());

        for (DetalleVenta venta : ventas) {
            totalBoletos += venta.cantidadBoletos();
            totalRecaudado = totalRecaudado.add(venta.total());
            compras.add(DetalleVentaResponse.from(venta, ZONA_REPORTE));
        }

        return new ReporteVentasResponse(
                fechaDesde,
                fechaHasta,
                compras.size(),
                totalBoletos,
                totalRecaudado,
                compras
        );
    }
}
