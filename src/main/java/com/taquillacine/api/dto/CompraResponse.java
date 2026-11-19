package com.taquillacine.api.dto;

import com.taquillacine.api.model.Compra;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class CompraResponse {
    private Long id;
    private Long funcionId;
    private String nombreCliente;
    private Integer cantidadBoletos;
    private BigDecimal total;
    private OffsetDateTime fechaCompra;

    public CompraResponse() {
    }

    public CompraResponse(Long id, Long funcionId, String nombreCliente, Integer cantidadBoletos,
                          BigDecimal total, OffsetDateTime fechaCompra) {
        this.id = id;
        this.funcionId = funcionId;
        this.nombreCliente = nombreCliente;
        this.cantidadBoletos = cantidadBoletos;
        this.total = total;
        this.fechaCompra = fechaCompra;
    }

    public static CompraResponse from(Compra compra) {
        return new CompraResponse(
                compra.getId(),
                compra.getFuncionId(),
                compra.getNombreCliente(),
                compra.getCantidadBoletos(),
                compra.getTotal(),
                compra.getFechaCompra()
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
