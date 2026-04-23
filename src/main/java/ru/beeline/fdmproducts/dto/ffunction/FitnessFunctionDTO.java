/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.dto.ffunction;

import lombok.*;
import ru.beeline.fdmproducts.dto.AssessmentObjectDTO;

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
