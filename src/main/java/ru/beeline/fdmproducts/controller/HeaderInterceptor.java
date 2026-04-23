/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.HandlerInterceptor;
import ru.beeline.fdmproducts.exception.ForbiddenException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;
import java.util.stream.Collectors;

import static ru.beeline.fdmproducts.utils.Constant.*;

public class HeaderInterceptor implements HandlerInterceptor {
    private Logger logger = LoggerFactory.getLogger(HeaderInterceptor.class);

    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        try {
            if ("PUT".equals(request.getMethod()) && request.getRequestURI().equals("/api/v1/product")) {

            } else if (request.getRequestURI().contains("/tech")
                    || request.getRequestURI().contains("/api/v1/operation")
                    || request.getRequestURI().contains("/actuator")
                    || request.getRequestURI().contains("/favicon.ico")
                    || request.getRequestURI().contains("/swagger-ui")
                    || request.getRequestURI().contains("/v3/api-docs")
                    || request.getRequestURI().contains("/error")
                    || request.getRequestURI().contains("/api-docs")
                    || request.getRequestURI().matches("/api/v1/product/[^/]+/free")
                    || request.getRequestURI().matches("/api/v1/product/[^/]+/employee")
                    || (request.getRequestURI().contains("/api/v1/product") && !request.getRequestURI().contains(
                    "/structurizr-key") && !(request.getRequestURI().contains("/interface/arch") || request.getRequestURI().contains("/interface/mapic")))
                    || request.getRequestURI().contains("/api/v1/infra")
                    || request.getRequestURI().contains("/api/v1/user/product")
                    || request.getRequestURI().matches("/api/v1/user/\\w+/products")
                    || request.getRequestURI().contains("/api/v1/discovered-interfaces")
                    || request.getRequestURI().contains("/api/v1/discovered-interface")
                    || request.getRequestURI().contains("/api/v1/mapic")
                    || request.getRequestURI().contains("/influence")
                    || request.getRequestURI().contains("/tc-implementation")
                    || (request.getRequestURI().contains("/operation/tech-capability/") && request.getRequestURI().contains("/tree"))
                    || request.getRequestURI().contains("/source-metric")
                    || request.getRequestURI().contains("/dashboard/fitness-function")
                    || request.getRequestURI().contains("/api/v1/service")
                    || ("GET".equals(request.getMethod()) && request.getRequestURI().contains("/api/v1/chapter"))
                    || ("PATCH".equals(request.getMethod()) && request.getRequestURI().contains("/api/v1/chapter"))
                    || request.getRequestURI().contains("/api/v2/product")
                    || request.getRequestURI().contains("/api/v1/nfr/product")
                    || ("GET".equals(request.getMethod()) && request.getRequestURI().contains("/api/v1/nfr"))
                    || ("POST".equals(request.getMethod()) && request.getRequestURI().contains("/api/v1/nfr/pattern/"))
                    || ("DELETE".equals(request.getMethod()) && request.getRequestURI().matches("/api/v1/nfr/\\d+/product"))
                    || request.getRequestURI().contains("/api/v1/pattern/product")
                    || request.getRequestURI().contains("/api/v1/requirement/pattern")
                    || request.getRequestURI().contains("/api/v1/ff")
            ) {
                return true;
            }
            Map<String, Object> headers = new HashMap<>();
            Enumeration<String> headerNames = request.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                String headerValue = request.getHeader(headerName);
                logger.debug(headerName + ": " + headerValue);
            }
            logger.info(USER_ID_HEADER + request.getHeader(USER_ID_HEADER));
            headers.put(USER_ID_HEADER, request.getHeader(USER_ID_HEADER).toString());
            logger.info(USER_PERMISSION_HEADER + toList(request.getHeader(USER_PERMISSION_HEADER)));
            headers.put(USER_PERMISSION_HEADER, toList(request.getHeader(USER_PERMISSION_HEADER).toString()));
            logger.info(USER_PRODUCTS_IDS_HEADER + toList(request.getHeader(USER_PRODUCTS_IDS_HEADER)));
            headers.put(USER_PRODUCTS_IDS_HEADER, toList(request.getHeader(USER_PRODUCTS_IDS_HEADER).toString()));
            logger.info(USER_ROLES_HEADER + toList(request.getHeader(USER_ROLES_HEADER)));
            headers.put(USER_ROLES_HEADER, toList(request.getHeader(USER_ROLES_HEADER).toString()));
            RequestContext.setHeaders(headers);
            logger.info("Set headers complete");
            return true;
        } catch (Exception e) {
            logger.info("failed " + request.getRequestURI());
            throw new ForbiddenException("403 Forbidden.");
        }
    }

    private List<String> toList(String value) {
        return Arrays.stream(value.split(","))
                .map(str -> str.substring(0))
                .map(str -> str.replaceAll("\"", ""))
                .map(str -> str.replaceAll("]", ""))
                .map(str -> str.replaceAll("\\[", ""))
                .map(String::trim)
                .collect(Collectors.toList());
    }
}


