package com.taquillacine.api.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

public class Funcion {
    private Long id;
    private Long peliculaId;
    private LocalDate fecha;
    private LocalTime hora;
    private String sala;
    private BigDecimal precio;
    private Integer cuposTotales;
    private Integer cuposDisponibles;

    public Funcion() {
    }

    public Funcion(Long id, Long peliculaId, LocalDate fecha, LocalTime hora, String sala,
                   BigDecimal precio, Integer cuposTotales, Integer cuposDisponibles) {
        this.id = id;
        this.peliculaId = peliculaId;
        this.fecha = fecha;
        this.hora = hora;
        this.sala = sala;
        this.precio = precio;
        this.cuposTotales = cuposTotales;
        this.cuposDisponibles = cuposDisponibles;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Integer getCuposDisponibles() {
        return cuposDisponibles;
    }

    public void setCuposDisponibles(Integer cuposDisponibles) {
        this.cuposDisponibles = cuposDisponibles;
    }

    @Override
    public String toString() {
        return "Funcion{" +
                "id=" + id +
                ", peliculaId=" + peliculaId +
                ", fecha=" + fecha +
                ", hora=" + hora +
                ", sala='" + sala + '\'' +
                ", precio=" + precio +
                ", cuposTotales=" + cuposTotales +
                ", cuposDisponibles=" + cuposDisponibles +
                '}';
    }
}