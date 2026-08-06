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
public class ProductTechRelationBatchDTO {

    @JsonProperty("cmdb_code")
    private String cmdbCode;

    @JsonProperty("tech_ids")
    private List<Integer> techIds;
}
