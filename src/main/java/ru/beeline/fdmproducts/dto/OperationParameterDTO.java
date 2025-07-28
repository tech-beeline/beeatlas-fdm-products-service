package ru.beeline.fdmproducts.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OperationParameterDTO {
    private String parameterName;
    private String parameterType;
}

