package com.taquillacine.api.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class Compra {
    private Long id;
    private Long funcionId;
    private String nombreCliente;
    private Integer cantidadBoletos;
    private BigDecimal total;
    private OffsetDateTime fechaCompra;

    public Compra() {
    }

    public Compra(Long id, Long funcionId, String nombreCliente, Integer cantidadBoletos,
                  BigDecimal total, OffsetDateTime fechaCompra) {
        this.id = id;
        this.funcionId = funcionId;
        this.nombreCliente = nombreCliente;
        this.cantidadBoletos = cantidadBoletos;
        this.total = total;
        this.fechaCompra = fechaCompra;
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

    @Override
    public String toString() {
        return "Compra{" +
                "id=" + id +
                ", funcionId=" + funcionId +
                ", nombreCliente='" + nombreCliente + '\'' +
                ", cantidadBoletos=" + cantidadBoletos +
                ", total=" + total +
                ", fechaCompra=" + fechaCompra +
                '}';
    }
}
