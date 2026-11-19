package com.taquillacine.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class ReporteVentasResponse {
    private LocalDate fechaDesde;
    private LocalDate fechaHasta;
    private long cantidadCompras;
    private long totalBoletos;
    private BigDecimal totalRecaudado;
    private List<DetalleVentaResponse> compras;

    public ReporteVentasResponse() {
    }

    public ReporteVentasResponse(LocalDate fechaDesde, LocalDate fechaHasta,
                                 long cantidadCompras, long totalBoletos,
                                 BigDecimal totalRecaudado, List<DetalleVentaResponse> compras) {
        this.fechaDesde = fechaDesde;
        this.fechaHasta = fechaHasta;
        this.cantidadCompras = cantidadCompras;
        this.totalBoletos = totalBoletos;
        this.totalRecaudado = totalRecaudado;
        this.compras = compras;
    }

    public LocalDate getFechaDesde() {
        return fechaDesde;
    }

    public void setFechaDesde(LocalDate fechaDesde) {
        this.fechaDesde = fechaDesde;
    }

    public LocalDate getFechaHasta() {
        return fechaHasta;
    }

    public void setFechaHasta(LocalDate fechaHasta) {
        this.fechaHasta = fechaHasta;
    }

    public long getCantidadCompras() {
        return cantidadCompras;
    }

    public void setCantidadCompras(long cantidadCompras) {
        this.cantidadCompras = cantidadCompras;
    }

    public long getTotalBoletos() {
        return totalBoletos;
    }

    public void setTotalBoletos(long totalBoletos) {
        this.totalBoletos = totalBoletos;
    }

    public BigDecimal getTotalRecaudado() {
        return totalRecaudado;
    }

    public void setTotalRecaudado(BigDecimal totalRecaudado) {
        this.totalRecaudado = totalRecaudado;
    }

    public List<DetalleVentaResponse> getCompras() {
        return compras;
    }

    public void setCompras(List<DetalleVentaResponse> compras) {
        this.compras = compras;
    }
}
