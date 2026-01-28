/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class AssessmentResponseDTO {

    private Integer assessmentId;
    private SourceDTO source;
    private LocalDateTime createdDate;
    private Integer productId;
    private List<FitnessFunctionResponseDTO> fitnessFunctions;
}
