package ru.beeline.fdmproducts.controller;


import java.util.Map;


public class RequestContext {
    private static final ThreadLocal<Map<String, Object>> headersThreadLocal = new ThreadLocal<>();
    public static void setHeaders(Map<String, Object> headers) {
        headersThreadLocal.set(headers);
    }
}
