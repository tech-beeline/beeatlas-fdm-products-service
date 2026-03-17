package ru.beeline.fdmproducts.dto;

import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class MainResponseDTO {
    private List<FitnessFunctionEnumDTO> fitnessFunctionEnum;
    private List<DomainDTO> domain;
}