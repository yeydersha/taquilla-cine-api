package com.taquillacine.api.dto;

import com.taquillacine.api.model.DetalleVenta;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;

public class DetalleVentaResponse {
    private Long id;
    private Long funcionId;
    private String tituloPelicula;
    private LocalDate fechaFuncion;
    private LocalTime horaFuncion;
    private String sala;
    private String nombreCliente;
    private Integer cantidadBoletos;
    private BigDecimal total;
    private OffsetDateTime fechaCompra;

    public DetalleVentaResponse() {
    }

    public DetalleVentaResponse(Long id, Long funcionId, String tituloPelicula,
                                LocalDate fechaFuncion, LocalTime horaFuncion, String sala,
                                String nombreCliente, Integer cantidadBoletos,
                                BigDecimal total, OffsetDateTime fechaCompra) {
        this.id = id;
        this.funcionId = funcionId;
        this.tituloPelicula = tituloPelicula;
        this.fechaFuncion = fechaFuncion;
        this.horaFuncion = horaFuncion;
        this.sala = sala;
        this.nombreCliente = nombreCliente;
        this.cantidadBoletos = cantidadBoletos;
        this.total = total;
        this.fechaCompra = fechaCompra;
    }

    public static DetalleVentaResponse from(DetalleVenta venta, ZoneId zonaReporte) {
        return new DetalleVentaResponse(
                venta.id(),
                venta.funcionId(),
                venta.tituloPelicula(),
                venta.fechaFuncion(),
                venta.horaFuncion(),
                venta.sala(),
                venta.nombreCliente(),
                venta.cantidadBoletos(),
                venta.total(),
                venta.fechaCompra() == null
                        ? null
                        : venta.fechaCompra().atZoneSameInstant(zonaReporte).toOffsetDateTime()
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getFuncionId() {
        return funcionId;
    }

    public void setFuncionId(Long funcionId) {
        this.funcionId = funcionId;
    }

    public String getTituloPelicula() {
        return tituloPelicula;
    }

    public void setTituloPelicula(String tituloPelicula) {
        this.tituloPelicula = tituloPelicula;
    }

    public LocalDate getFechaFuncion() {
        return fechaFuncion;
    }

    public void setFechaFuncion(LocalDate fechaFuncion) {
        this.fechaFuncion = fechaFuncion;
    }

    public LocalTime getHoraFuncion() {
        return horaFuncion;
    }

    public void setHoraFuncion(LocalTime horaFuncion) {
        this.horaFuncion = horaFuncion;
    }

    public String getSala() {
        return sala;
    }

    public void setSala(String sala) {
        this.sala = sala;
    }

    public String getNombreCliente() {
        return nombreCliente;
    }

    public void setNombreCliente(String nombreCliente) {
        this.nombreCliente = nombreCliente;
    }

    public Integer getCantidadBoletos() {
        return cantidadBoletos;
    }

    public void setCantidadBoletos(Integer cantidadBoletos) {
        this.cantidadBoletos = cantidadBoletos;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public OffsetDateTime getFechaCompra() {
        return fechaCompra;
    }

    public void setFechaCompra(OffsetDateTime fechaCompra) {
        this.fechaCompra = fechaCompra;
    }
}
