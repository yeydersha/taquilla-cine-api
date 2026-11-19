package com.taquillacine.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public class CompraRequest {
    @NotNull(message = "El ID de la función es obligatorio")
    @Positive(message = "El ID de la función debe ser positivo")
    private Long funcionId;

    @NotBlank(message = "El nombre del cliente es obligatorio")
    @Size(max = 150, message = "El nombre del cliente no debe exceder 150 caracteres")
    private String nombreCliente;

    @NotNull(message = "La cantidad de boletos es obligatoria")
    @Min(value = 1, message = "La cantidad de boletos debe ser mayor que cero")
    private Integer cantidadBoletos;

    public CompraRequest() {
    }

    public CompraRequest(Long funcionId, String nombreCliente, Integer cantidadBoletos) {
        this.funcionId = funcionId;
        this.nombreCliente = nombreCliente;
        this.cantidadBoletos = cantidadBoletos;
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
}
