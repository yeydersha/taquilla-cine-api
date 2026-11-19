package com.taquillacine.api.service;

import com.taquillacine.api.exception.ConflictoIntegridadException;
import com.taquillacine.api.exception.RecursoNoEncontradoException;
import com.taquillacine.api.model.Funcion;
import com.taquillacine.api.model.FuncionConTitulo;
import com.taquillacine.api.repository.FuncionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FuncionService {

    private final FuncionRepository funcionRepository;

    public FuncionService(FuncionRepository funcionRepository) {
        this.funcionRepository = funcionRepository;
    }

    public List<FuncionConTitulo> listarTodas() {
        return funcionRepository.findAllConTitulo();
    }

    public FuncionConTitulo obtenerPorId(Long id) {
        return funcionRepository.findByIdConTitulo(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Función no encontrada con id: " + id));
    }

    @Transactional
    public FuncionConTitulo crear(Funcion funcion) {
        verificarPelicula(funcion.getPeliculaId());

        // Los cupos disponibles se inicializan con la capacidad total.
        funcion.setCuposDisponibles(funcion.getCuposTotales());

        Funcion guardada = funcionRepository.save(funcion);
        return obtenerPorId(guardada.getId());
    }

    @Transactional
    public FuncionConTitulo actualizar(Long id, Funcion cambios) {
        Funcion actual = funcionRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Función no encontrada con id: " + id));

        verificarPelicula(cambios.getPeliculaId());

        int cuposVendidos = actual.getCuposTotales() - actual.getCuposDisponibles();
        int comprasRegistradas = funcionRepository.contarCompras(id);

        if (comprasRegistradas > 0 && !actual.getPeliculaId().equals(cambios.getPeliculaId())) {
            throw new ConflictoIntegridadException(
                    "No se puede cambiar la película de una función que tiene compras registradas");
        }

        if (cambios.getCuposTotales() < cuposVendidos) {
            throw new ConflictoIntegridadException(
                    "La nueva capacidad total (" + cambios.getCuposTotales()
                            + ") no puede ser inferior a los cupos ya vendidos (" + cuposVendidos + ")");
        }

        actual.setPeliculaId(cambios.getPeliculaId());
        actual.setFecha(cambios.getFecha());
        actual.setHora(cambios.getHora());
        actual.setSala(cambios.getSala());
        actual.setPrecio(cambios.getPrecio());
        actual.setCuposTotales(cambios.getCuposTotales());
        actual.setCuposDisponibles(cambios.getCuposTotales() - cuposVendidos);

        int filas = funcionRepository.update(actual);
        if (filas != 1) {
            throw new RecursoNoEncontradoException("Función no encontrada con id: " + id);
        }

        return obtenerPorId(id);
    }

    @Transactional
    public void eliminar(Long id) {
        funcionRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Función no encontrada con id: " + id));

        if (funcionRepository.contarCompras(id) > 0) {
            throw new ConflictoIntegridadException(
                    "No se puede eliminar la función porque tiene compras asociadas");
        }

        // La verificación anterior ocurre con la fila bloqueada, por lo que una
        // compra concurrente no puede aparecer entre el conteo y el DELETE. Si
        // PostgreSQL rechazara el DELETE por la FK, GlobalExceptionHandler
        // responde 409 con un mensaje controlado.
        int filas = funcionRepository.deleteById(id);
        if (filas != 1) {
            throw new RecursoNoEncontradoException("Función no encontrada con id: " + id);
        }
    }

    private void verificarPelicula(Long peliculaId) {
        if (!funcionRepository.existePelicula(peliculaId)) {
            throw new RecursoNoEncontradoException("Película no encontrada con id: " + peliculaId);
        }
    }
}
