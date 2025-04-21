package ru.beeline.fdmproducts.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class FitnessFunctionDTO {

    private String code;
    private Boolean isCheck;
    private String resultDetails;
}
