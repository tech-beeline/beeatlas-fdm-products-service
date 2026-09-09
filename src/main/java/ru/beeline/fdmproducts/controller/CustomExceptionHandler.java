/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;
import ru.beeline.fdmproducts.dto.ErrorResponse;
import ru.beeline.fdmproducts.dto.ErrorMessageDTO;
import ru.beeline.fdmproducts.exception.AuthServiceUnavailableException;
import ru.beeline.fdmproducts.exception.DatabaseConnectionException;
import ru.beeline.fdmproducts.exception.EntityNotFoundException;
import ru.beeline.fdmproducts.exception.ForbiddenException;
import ru.beeline.fdmproducts.exception.NotAdministratorException;
import ru.beeline.fdmproducts.exception.UnauthorizedException;
import ru.beeline.fdmproducts.exception.ValidationException;

import java.lang.IllegalArgumentException;

@ControllerAdvice
@Slf4j
public class CustomExceptionHandler {


    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorMessageDTO> handleException(ForbiddenException e) {
        log.error(e.getMessage());
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .header("content-type", MediaType.APPLICATION_JSON_VALUE)
                .body(new ErrorMessageDTO(e.getMessage()));
    }

    @ExceptionHandler(NotAdministratorException.class)
    public ResponseEntity<ErrorMessageDTO> handleException(NotAdministratorException e) {
        log.error(e.getMessage());
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .header("content-type", MediaType.APPLICATION_JSON_VALUE)
                .body(new ErrorMessageDTO(e.getMessage()));
    }

    /**
     * Catch-all for anything not covered by a more specific handler above — by construction that
     * means an unanticipated bug (NPE, ClassCastException, a broken query, ...), not a deliberate
     * business-rule signal (those all have their own handler with a purposeful message). The raw
     * message can carry internal details (class/method names, SQL, URLs) — log it in full, but never
     * forward it to the caller.
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorMessageDTO> handleException(RuntimeException e) {
        log.error(e.getMessage(), e);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .header("content-type", MediaType.APPLICATION_JSON_VALUE)
                .body(new ErrorMessageDTO("Внутренняя ошибка сервера"));
    }

    @ExceptionHandler(AuthServiceUnavailableException.class)
    public ResponseEntity<ErrorMessageDTO> handleException(AuthServiceUnavailableException e) {
        log.error(e.getMessage());
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .header("content-type", MediaType.APPLICATION_JSON_VALUE)
                .body(new ErrorMessageDTO(e.getMessage()));
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<Object> handleException(ValidationException e) {
        log.error(e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorMessageDTO(e.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorMessageDTO> handleException(IllegalArgumentException e) {
        log.error(e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorMessageDTO(e.getMessage()));
    }

    /**
     * Отсутствует обязательный query-параметр. Без этого обработчика исключение — checked
     * {@link javax.servlet.ServletException} — доходит до {@link #handleException(Exception)}
     * и клиент получает 500 вместо 400.
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorMessageDTO> handleMissingRequestParameter(MissingServletRequestParameterException e) {
        log.warn(e.getMessage());
        return badRequest("Не передан обязательный параметр запроса '" + e.getParameterName() + "'");
    }

    /** Отсутствует обязательный заголовок запроса — см. комментарий к обработчику выше. */
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorMessageDTO> handleMissingRequestHeader(MissingRequestHeaderException e) {
        log.warn(e.getMessage());
        return badRequest("Не передан обязательный заголовок запроса '" + e.getHeaderName() + "'");
    }

    /** Прочие ошибки привязки запроса (отсутствующая cookie и т. п.) — тоже вина клиента, а не сервера. */
    @ExceptionHandler(ServletRequestBindingException.class)
    public ResponseEntity<ErrorMessageDTO> handleRequestBinding(ServletRequestBindingException e) {
        log.warn(e.getMessage());
        return badRequest("Некорректные параметры запроса: " + e.getMessage());
    }

    private ResponseEntity<ErrorMessageDTO> badRequest(String message) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .header("content-type", MediaType.APPLICATION_JSON_VALUE)
                .body(new ErrorMessageDTO(message));
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Object> handleException(UnauthorizedException e) {
        log.error(e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Object> handleException(EntityNotFoundException e) {
        log.error(e.getMessage());
        ErrorMessageDTO error = ErrorMessageDTO.builder()
                .errorMessage(e.getMessage())
                .build();
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .header("content-type", MediaType.APPLICATION_JSON_VALUE)
                .body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<String> handleValidationExceptions(MethodArgumentNotValidException ex) {
        return ResponseEntity.badRequest().body("Validation error: " + ex.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorMessageDTO> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest().body(new ErrorMessageDTO("Неверные входные данные"));
    }

    @ExceptionHandler(DatabaseConnectionException.class)
    public ResponseEntity<String> handleDatabaseConnectionException(DatabaseConnectionException e) {
        log.error(e.getMessage());
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .header("content-type", MediaType.APPLICATION_JSON_VALUE)
                .body(e.getMessage());
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatus(ResponseStatusException ex) {
        return ResponseEntity.status(ex.getRawStatusCode()).body(new ErrorResponse(ex.getReason()));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorMessageDTO> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex) {
        return ResponseEntity
                .status(HttpStatus.METHOD_NOT_ALLOWED)
                .header("content-type", MediaType.APPLICATION_JSON_VALUE)
                .body(new ErrorMessageDTO("Метод не разрешён для данного ресурса."));
    }

    /** Last-resort net below {@link #handleException(RuntimeException)} — a checked exception. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorMessageDTO> handleException(Exception e) {
        log.error(e.getMessage(), e);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .header("content-type", MediaType.APPLICATION_JSON_VALUE)
                .body(new ErrorMessageDTO("Внутренняя ошибка сервера"));
    }
}