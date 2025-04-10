package ru.beeline.fdmproducts.dto;


import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class InfraRequestDTO {
    private List<InfraDTO> infra;
    private List<RelationDTO> relations;
}
