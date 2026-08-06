/*
 * Copyright (c) 2024 PJSC VimpelCom
 */
package ru.beeline.fdmproducts.config;

import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import ru.beeline.fdmproducts.annotation.ApiErrorCodes;

@Component
public class ApiErrorCodesCustomizer implements OperationCustomizer {

    @Override
    public io.swagger.v3.oas.models.Operation customize(
            io.swagger.v3.oas.models.Operation operation,
            HandlerMethod handlerMethod) {

        ApiErrorCodes annotation = AnnotationUtils.findAnnotation(handlerMethod.getMethod(), ApiErrorCodes.class);
        if (annotation == null) {
            annotation = AnnotationUtils.findAnnotation(handlerMethod.getBeanType(), ApiErrorCodes.class);
        }

        if (annotation != null) {
            ApiResponses responses = operation.getResponses();
            if (responses == null) {
                responses = new ApiResponses();
                operation.setResponses(responses);
            }

            for (int code : annotation.value()) {
                String codeStr = String.valueOf(code);
                if (!responses.containsKey(codeStr)) {
                    ApiResponse resp = new ApiResponse()
                            .description(getMessage(code))
                            .content(new Content().addMediaType("application/json",
                                    new MediaType().schema(new io.swagger.v3.oas.models.media.Schema<>()
                                            .type("object")
                                            .addProperty("errorMessage", new StringSchema().description("Сообщение об ошибке")))));
                    responses.addApiResponse(codeStr, resp);
                } else if (code >= 400) {
                    ApiResponse existingResp = responses.get(codeStr);
                    if (existingResp != null) {
                        existingResp.setContent(new Content().addMediaType("application/json",
                                new MediaType().schema(new io.swagger.v3.oas.models.media.Schema<>()
                                        .type("object")
                                        .addProperty("errorMessage", new StringSchema().description("Сообщение об ошибке")))));
                    }
                }
            }
        }

        return operation;
    }

    private String getMessage(int code) {
        switch (code) {
            case 400: return "Неверные входные данные";
            case 401: return "Требуется аутентификация";
            case 403: return "Доступ запрещен";
            case 404: return "Ресурс не найден";
            case 409: return "Конфликт данных";
            case 500: return "Внутренняя ошибка сервера";
            default:  return "Error " + code;
        }
    }
}