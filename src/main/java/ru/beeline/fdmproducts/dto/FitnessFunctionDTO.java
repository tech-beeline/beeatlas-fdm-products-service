/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.dto;

import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class FitnessFunctionDTO {

    private String code;
    private Boolean isCheck;
    private String resultDetails;
    private String assessmentDescription;
    private List<AssessmentObjectDTO> assessmentObjects;
}
