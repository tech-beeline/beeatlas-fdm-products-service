/*
 * Copyright (c) 2024 PJSC VimpelCom
 */
package ru.beeline.fdmproducts.dto.ffunction;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class FitnessFunctionNfrV2DTO {

    private Integer id;
    private String code;
    private String description;
}

