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
public class OperationDTOTree {

    @JsonProperty("operationId")
    private Integer operationId;

    @JsonProperty("operationName")
    private String operationName;

    @JsonProperty("operationType")
    private String operationType;
}

