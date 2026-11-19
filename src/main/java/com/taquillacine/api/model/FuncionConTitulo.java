package com.taquillacine.api.model;

/**
 * Resultado de la consulta de funciones con el título de la película asociada.
 * Se utiliza para construir las respuestas sin exponer detalles del JOIN.
 */
public record FuncionConTitulo(Funcion funcion, String tituloPelicula) {
}
