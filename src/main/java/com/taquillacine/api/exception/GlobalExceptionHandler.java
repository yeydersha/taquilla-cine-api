package com.taquillacine.api.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** SQLSTATE de PostgreSQL para violación de clave foránea. */
    private static final String SQLSTATE_CLAVE_FORANEA = "23503";

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ErrorResponse> handleRecursoNoEncontrado(
            RecursoNoEncontradoException ex, HttpServletRequest request) {

        ErrorResponse error = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                "Recurso no encontrado",
                ex.getMessage(),
                request.getRequestURI(),
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(ConflictoIntegridadException.class)
    public ResponseEntity<ErrorResponse> handleConflictoIntegridad(
            ConflictoIntegridadException ex, HttpServletRequest request) {

        ErrorResponse error = new ErrorResponse(
                HttpStatus.CONFLICT.value(),
                "Conflicto de integridad",
                ex.getMessage(),
                request.getRequestURI(),
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(CuposInsuficientesException.class)
    public ResponseEntity<ErrorResponse> handleCuposInsuficientes(
            CuposInsuficientesException ex, HttpServletRequest request) {

        ErrorResponse error = new ErrorResponse(
                HttpStatus.CONFLICT.value(),
                "Cupos insuficientes",
                ex.getMessage(),
                request.getRequestURI(),
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(TotalFueraDeRangoException.class)
    public ResponseEntity<ErrorResponse> handleTotalFueraDeRango(
            TotalFueraDeRangoException ex, HttpServletRequest request) {

        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Monto fuera de rango",
                ex.getMessage(),
                request.getRequestURI(),
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(RangoFechasInvalidoException.class)
    public ResponseEntity<ErrorResponse> handleRangoFechasInvalido(
            RangoFechasInvalidoException ex, HttpServletRequest request) {

        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Rango de fechas inválido",
                ex.getMessage(),
                request.getRequestURI(),
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleParametroFaltante(
            MissingServletRequestParameterException ex, HttpServletRequest request) {

        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Parámetro faltante",
                "El parámetro '" + ex.getParameterName() + "' es obligatorio",
                request.getRequestURI(),
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTipoParametroInvalido(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {

        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Parámetro inválido",
                "El parámetro '" + ex.getName() + "' tiene un valor o formato inválido",
                request.getRequestURI(),
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidationErrors(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        Map<String, String> errores = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errores.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        ValidationErrorResponse error = new ValidationErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Datos de entrada inválidos",
                "Validación fallida en uno o más campos",
                request.getRequestURI(),
                LocalDateTime.now(),
                errores
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex, HttpServletRequest request) {

        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Restricción violada",
                ex.getMessage(),
                request.getRequestURI(),
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleJsonParseError(
            HttpMessageNotReadableException ex, HttpServletRequest request) {

        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "JSON inválido",
                "El cuerpo de la solicitud contiene JSON malformado",
                request.getRequestURI(),
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMetodoNoSoportado(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {

        ErrorResponse error = new ErrorResponse(
                HttpStatus.METHOD_NOT_ALLOWED.value(),
                "Método no permitido",
                "El método " + ex.getMethod() + " no está permitido para este recurso",
                request.getRequestURI(),
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(error);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleRutaNoEncontrada(
            NoResourceFoundException ex, HttpServletRequest request) {

        ErrorResponse error = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                "Recurso no encontrado",
                "La ruta solicitada no existe",
                request.getRequestURI(),
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(
            DataIntegrityViolationException ex, HttpServletRequest request) {

        SQLException sqlException = buscarSqlException(ex);

        if (sqlException != null && SQLSTATE_CLAVE_FORANEA.equals(sqlException.getSQLState())) {
            String mensaje = mensajeClaveForanea(sqlException.getMessage());
            LOG.warn("Conflicto de integridad referencial en {}: {}",
                    request.getRequestURI(), mensaje);
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ErrorResponse(
                            HttpStatus.CONFLICT.value(),
                            "Conflicto de integridad",
                            mensaje,
                            request.getRequestURI(),
                            LocalDateTime.now()));
        }

        // Otras violaciones de integridad (CHECK, NOT NULL, UNIQUE, rango) no se
        // traducen a 409 ni exponen el mensaje original de PostgreSQL.
        LOG.error("Error de integridad no referencial en {}", request.getRequestURI(), ex);
        return errorInterno(request);
    }

    /**
     * Recorre la cadena de causas hasta encontrar la excepción JDBC que contiene
     * el SQLSTATE emitido por PostgreSQL.
     */
    private static SQLException buscarSqlException(Throwable error) {
        Throwable actual = error;
        while (actual != null) {
            if (actual instanceof SQLException sqlException) {
                return sqlException;
            }
            actual = actual.getCause();
        }
        return null;
    }

    private static String mensajeClaveForanea(String mensajePostgresql) {
        String detalle = mensajePostgresql != null ? mensajePostgresql : "";
        if (detalle.contains("fk_funciones_peliculas")) {
            return "No se puede eliminar la película porque tiene funciones asociadas";
        }
        if (detalle.contains("fk_compras_funciones")) {
            return "No se puede eliminar la función porque tiene compras asociadas";
        }
        return "La operación entra en conflicto con registros relacionados";
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleTipoContenidoNoSoportado(
            HttpMediaTypeNotSupportedException ex, HttpServletRequest request) {

        String tipo = ex.getContentType() != null
                ? ex.getContentType().toString()
                : "desconocido";

        ErrorResponse error = new ErrorResponse(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(),
                "Tipo de contenido no soportado",
                "El tipo de contenido '" + tipo + "' no está soportado; utilice application/json",
                request.getRequestURI(),
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(error);
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<ErrorResponse> handleFormatoRespuestaNoSoportado(
            HttpMediaTypeNotAcceptableException ex, HttpServletRequest request) {

        ErrorResponse error = new ErrorResponse(
                HttpStatus.NOT_ACCEPTABLE.value(),
                "Formato de respuesta no soportado",
                "No es posible generar la respuesta con el formato solicitado; utilice application/json",
                request.getRequestURI(),
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, HttpServletRequest request) {

        LOG.error("Error inesperado en {}", request.getRequestURI(), ex);
        return errorInterno(request);
    }

    private static ResponseEntity<ErrorResponse> errorInterno(HttpServletRequest request) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Error interno del servidor",
                "Ha ocurrido un error inesperado",
                request.getRequestURI(),
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    public static class ErrorResponse {
        private final int status;
        private final String error;
        private final String mensaje;
        private final String ruta;
        private final LocalDateTime timestamp;

        public ErrorResponse(int status, String error, String mensaje, String ruta, LocalDateTime timestamp) {
            this.status = status;
            this.error = error;
            this.mensaje = mensaje;
            this.ruta = ruta;
            this.timestamp = timestamp;
        }

        public int getStatus() { return status; }
        public String getError() { return error; }
        public String getMensaje() { return mensaje; }
        public String getRuta() { return ruta; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }

    public static class ValidationErrorResponse extends ErrorResponse {
        private final Map<String, String> errores;

        public ValidationErrorResponse(int status, String error, String mensaje, String ruta,
                                       LocalDateTime timestamp, Map<String, String> errores) {
            super(status, error, mensaje, ruta, timestamp);
            this.errores = errores;
        }

        public Map<String, String> getErrores() { return errores; }
    }
}