/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.config;

import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.WebRequest;

import java.util.Map;

@Component
public class CustomErrorAttributes extends DefaultErrorAttributes {

    @Override
    public Map<String, Object> getErrorAttributes(WebRequest webRequest, ErrorAttributeOptions options) {
        Map<String, Object> attrs = super.getErrorAttributes(webRequest, options);
        Integer status = (Integer) attrs.get("status");
        if (status != null && status == 404) {
            Object path = attrs.get("path");
            attrs.put("errorMessage", path == null
                    ? "Эндпоинт не найден"
                    : "Эндпоинт не найден: " + path);
        }
        return attrs;
    }
}
