/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiscoveredInterfaceOperationDTO {
    private String name;
    private String context;
    private String description;
    private String type;
    private String returnType;
    private List<OperationParameterDTO> parameters;
}

