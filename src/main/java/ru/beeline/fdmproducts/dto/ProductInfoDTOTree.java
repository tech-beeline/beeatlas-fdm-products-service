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
public class ProductInfoDTOTree {

    @JsonProperty("productId")
    private Integer productId;

    @JsonProperty("productName")
    private String productName;

    @JsonProperty("productCode")
    private String productCode;

    @JsonProperty("containers")
    private List<ContainerDTOTree> containers;
}
