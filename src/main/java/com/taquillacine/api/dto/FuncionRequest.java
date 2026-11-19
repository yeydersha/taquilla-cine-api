package com.taquillacine.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

public class FuncionRequest {
    @NotNull(message = "El ID de la película es obligatorio")
    @Positive(message = "El ID de la película debe ser positivo")
    private Long peliculaId;

    @NotNull(message = "La fecha es obligatoria")
    private LocalDate fecha;

    @NotNull(message = "La hora es obligatoria")
    private LocalTime hora;

    @NotBlank(message = "La sala es obligatoria")
    @Size(max = 50, message = "La sala no debe exceder 50 caracteres")
    private String sala;

    @NotNull(message = "El precio es obligatorio")
    @DecimalMin(value = "0.01", message = "El precio debe ser mayor que cero")
    @Digits(integer = 10, fraction = 2, message = "El precio debe ser compatible con NUMERIC(12,2)")
    private BigDecimal precio;

    @NotNull(message = "Los cupos totales son obligatorios")
    @Min(value = 1, message = "Los cupos totales deben ser mayores que cero")
    private Integer cuposTotales;

    public FuncionRequest() {
    }

    public FuncionRequest(Long peliculaId, LocalDate fecha, LocalTime hora, String sala,
                          BigDecimal precio, Integer cuposTotales) {
        this.peliculaId = peliculaId;
        this.fecha = fecha;
        this.hora = hora;
        this.sala = sala;
        this.precio = precio;
        this.cuposTotales = cuposTotales;
    }

    public Long getPeliculaId() {
        return peliculaId;
    }

    public void setPeliculaId(Long peliculaId) {
        this.peliculaId = peliculaId;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public LocalTime getHora() {
        return hora;
    }

    public void setHora(LocalTime hora) {
        this.hora = hora;
    }

    public String getSala() {
        return sala;
    }

    public void setSala(String sala) {
        this.sala = sala;
    }

    public BigDecimal getPrecio() {
        return precio;
    }

    public void setPrecio(BigDecimal precio) {
        this.precio = precio;
    }

    public Integer getCuposTotales() {
        return cuposTotales;
    }

    public void setCuposTotales(Integer cuposTotales) {
        this.cuposTotales = cuposTotales;
    }
}
