/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class DiscoveredOperationDTO {

    private Integer id;
    private String name;
    private String type;
    private ConnectionOperationDTO connectionOperation;
    private ConnectionOperationDTO connectionOperationDev;
    private ConnectionOperationDTO connectionOperationDesign;
    @JsonProperty("interface")
    private InterfaceSearchDTO interfaceObj;
    private ContainerSearchDTO container;
    private ProductSearchDTO product;
}
