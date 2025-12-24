package ru.beeline.fdmproducts.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.HandlerInterceptor;
import ru.beeline.fdmproducts.exception.ForbiddenException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static ru.beeline.fdmproducts.utils.Constant.USER_ID_HEADER;
import static ru.beeline.fdmproducts.utils.Constant.USER_PERMISSION_HEADER;
import static ru.beeline.fdmproducts.utils.Constant.USER_PRODUCTS_IDS_HEADER;
import static ru.beeline.fdmproducts.utils.Constant.USER_ROLES_HEADER;

public class HeaderInterceptor implements HandlerInterceptor {
    private Logger logger = LoggerFactory.getLogger(HeaderInterceptor.class);

    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        try {
            if (request.getRequestURI().contains("/tech")
                    || request.getRequestURI().contains("/actuator")
                    || request.getRequestURI().contains("/swagger")
                    || request.getRequestURI().contains("/error")
                    || request.getRequestURI().contains("/api-docs")
                    || request.getRequestURI().matches("/api/v1/[^/]+/free")
                    || request.getRequestURI().matches("/api/v1/[^/]+/employee")
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
                    || request.getRequestURI().contains("/api/v1/service")) {
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


