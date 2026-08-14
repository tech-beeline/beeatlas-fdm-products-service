/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.dto.e2e;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class E2eV2RelationTreeNodeDTO {

    private Integer order;
    private Integer relatedOperationId;
    private String stereotype;
    private String entityTypeRelatedOperation;
    private List<E2eV2RelationTreeNodeDTO> operationsRelations = new ArrayList<>();
}
