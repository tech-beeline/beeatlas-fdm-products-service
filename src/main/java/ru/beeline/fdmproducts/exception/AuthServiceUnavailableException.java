/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.exception;

public class AuthServiceUnavailableException extends RuntimeException {
    public AuthServiceUnavailableException(String message) {
        super(message);
    }
}

