package com.taquillacine.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

public class FuncionResponse {
    private Long id;
    private Long peliculaId;
    private String tituloPelicula;
    private LocalDate fecha;
    private LocalTime hora;
    private String sala;
    private BigDecimal precio;
    private Integer cuposTotales;
    private Integer cuposDisponibles;

    public FuncionResponse() {
    }

    public FuncionResponse(Long id, Long peliculaId, String tituloPelicula, LocalDate fecha,
                           LocalTime hora, String sala, BigDecimal precio,
                           Integer cuposTotales, Integer cuposDisponibles) {
        this.id = id;
        this.peliculaId = peliculaId;
        this.tituloPelicula = tituloPelicula;
        this.fecha = fecha;
        this.hora = hora;
        this.sala = sala;
        this.precio = precio;
        this.cuposTotales = cuposTotales;
        this.cuposDisponibles = cuposDisponibles;
    }

    public static FuncionResponse from(com.taquillacine.api.model.Funcion funcion, String tituloPelicula) {
        return new FuncionResponse(
                funcion.getId(),
                funcion.getPeliculaId(),
                tituloPelicula,
                funcion.getFecha(),
                funcion.getHora(),
                funcion.getSala(),
                funcion.getPrecio(),
                funcion.getCuposTotales(),
                funcion.getCuposDisponibles()
        );
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

    public String getTituloPelicula() {
        return tituloPelicula;
    }

    public void setTituloPelicula(String tituloPelicula) {
        this.tituloPelicula = tituloPelicula;
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
}