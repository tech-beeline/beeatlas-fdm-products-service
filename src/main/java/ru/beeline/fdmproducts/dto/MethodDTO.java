package ru.beeline.fdmproducts.dto;

import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class MethodDTO {

    private String name;
    private String description;
    private String returnType;
    private List<ParameterDTO> parameters;
    private SlaDTO sla;
    private String capabilityCode;
    private String type;
}
