package com.taquillacine.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class PeliculaRequest {
    @NotBlank(message = "El título es obligatorio")
    @Size(max = 150, message = "El título no debe exceder 150 caracteres")
    private String titulo;

    @NotBlank(message = "El género es obligatorio")
    @Size(max = 80, message = "El género no debe exceder 80 caracteres")
    private String genero;

    @NotNull(message = "La duración es obligatoria")
    @Min(value = 1, message = "La duración debe ser mayor que cero")
    private Integer duracionMin;

    @NotBlank(message = "La clasificación es obligatoria")
    @Size(max = 20, message = "La clasificación no debe exceder 20 caracteres")
    private String clasificacion;

    public PeliculaRequest() {
    }

    public PeliculaRequest(String titulo, String genero, Integer duracionMin, String clasificacion) {
        this.titulo = titulo;
        this.genero = genero;
        this.duracionMin = duracionMin;
        this.clasificacion = clasificacion;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getGenero() {
        return genero;
    }

    public void setGenero(String genero) {
        this.genero = genero;
    }

    public Integer getDuracionMin() {
        return duracionMin;
    }

    public void setDuracionMin(Integer duracionMin) {
        this.duracionMin = duracionMin;
    }

    public String getClasificacion() {
        return clasificacion;
    }

    public void setClasificacion(String clasificacion) {
        this.clasificacion = clasificacion;
    }
}