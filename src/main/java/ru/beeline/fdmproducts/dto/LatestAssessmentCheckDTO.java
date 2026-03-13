package ru.beeline.fdmproducts.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class LatestAssessmentCheckDTO {
    private Integer productId;
    private Integer lacId;
    private Boolean isCheck;
    private Integer fitnessFunctionId;
}