package ru.beeline.fdmproducts.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ValidationErrorResponse {

    private List<String> containerError = new ArrayList<>();
    private List<String> interfaceError = new ArrayList<>();
    private List<String> methodError = new ArrayList<>();
    private List<String> parameterError = new ArrayList<>();

    public boolean hasErrors() {
        return !(containerError.isEmpty() && interfaceError.isEmpty()
                && methodError.isEmpty() && parameterError.isEmpty());
    }
}