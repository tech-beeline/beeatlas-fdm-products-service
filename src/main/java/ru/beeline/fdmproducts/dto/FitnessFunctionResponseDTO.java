package ru.beeline.fdmproducts.dto;

import lombok.*;

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
}
