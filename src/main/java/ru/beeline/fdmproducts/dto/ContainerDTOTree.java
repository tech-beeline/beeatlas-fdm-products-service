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
public class ContainerDTOTree {

    @JsonProperty("containerId")
    private Integer containerId;

    @JsonProperty("containerName")
    private String containerName;

    @JsonProperty("containerCode")
    private String containerCode;

    @JsonProperty("interfaces")
    private List<InterfaceDTOTree> interfaces;

}
