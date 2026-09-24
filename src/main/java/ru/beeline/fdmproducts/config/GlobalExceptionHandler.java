package ru.beeline.fdmproducts.config;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import ru.beeline.fdmproducts.dto.ErrorResponse;

/**
 * Точечные обработчики, которые должны выиграть у catch-all.
 * <p>
 * Spring выбирает advice, а не самый специфичный обработчик среди всех: он идёт по advice по порядку
 * и останавливается на первом, где вообще нашлось совпадение. У CustomExceptionHandler есть
 * {@code @ExceptionHandler(RuntimeException.class)}, а MethodArgumentTypeMismatchException — потомок
 * RuntimeException, поэтому без явного порядка результат зависел бы от того, в каком порядке
 * сканирование положило бины: либо документированные 400 с message/timestamp, либо 500
 * «Внутренняя ошибка сервера».
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
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