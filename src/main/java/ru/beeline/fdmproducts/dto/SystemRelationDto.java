package ru.beeline.fdmproducts.dto;

import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class SystemRelationDto {
    private List<SystemInfoDTO> dependentSystems;
    private List<SystemInfoDTO> influencingSystems;

}
