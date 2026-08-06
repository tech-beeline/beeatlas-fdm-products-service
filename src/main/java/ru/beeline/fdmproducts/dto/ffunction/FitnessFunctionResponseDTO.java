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
public class FitnessFunctionResponseDTO {

    private Integer id;
    private String code;
    private String description;
    private Boolean isCheck;
    private String resultDetails;
    private String status;
    private String assessmentDescription;
    private String docLink;
    private List<AssessmentObjectDTO> details;
    private List<String> tableStruct;
}
