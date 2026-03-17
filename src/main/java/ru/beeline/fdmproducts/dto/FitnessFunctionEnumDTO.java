package ru.beeline.fdmproducts.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class FitnessFunctionEnumDTO {
    private Integer id;
    private String code;
    private String description;
}