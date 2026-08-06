/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.exception;

public class NotAdministratorException extends RuntimeException {
    public NotAdministratorException(String message) {
        super(message);
    }
}

