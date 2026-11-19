package com.taquillacine.api.dto;

public class PeliculaResponse {
    private Long id;
    private String titulo;
    private String genero;
    private Integer duracionMin;
    private String clasificacion;

    public PeliculaResponse() {
    }

    public PeliculaResponse(Long id, String titulo, String genero, Integer duracionMin, String clasificacion) {
        this.id = id;
        this.titulo = titulo;
        this.genero = genero;
        this.duracionMin = duracionMin;
        this.clasificacion = clasificacion;
    }

    public static PeliculaResponse from(com.taquillacine.api.model.Pelicula pelicula) {
        return new PeliculaResponse(
                pelicula.getId(),
                pelicula.getTitulo(),
                pelicula.getGenero(),
                pelicula.getDuracionMin(),
                pelicula.getClasificacion()
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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