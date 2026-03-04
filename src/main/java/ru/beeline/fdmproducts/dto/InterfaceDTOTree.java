/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class InterfaceDTOTree {


    @JsonProperty("interfaceId")
    private Integer interfaceId;

    @JsonProperty("interfaceName")
    private String interfaceName;

    @JsonProperty("interfaceCode")
    private String interfaceCode;

    @JsonProperty("operations")
    private List<OperationDTOTree> operations;
}

