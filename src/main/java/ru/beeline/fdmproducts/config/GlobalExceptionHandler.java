package ru.beeline.fdmproducts.config;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import ru.beeline.fdmproducts.dto.ErrorResponse;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String fieldName = ex.getName() != null ? ex.getName() : "path parameter";
        String message = String.format("Неверный формат '%s' для параметра %s: ожидается целое число",
                                       ex.getValue(), fieldName);
        return ResponseEntity.badRequest().body(new ErrorResponse(message));
    }
}