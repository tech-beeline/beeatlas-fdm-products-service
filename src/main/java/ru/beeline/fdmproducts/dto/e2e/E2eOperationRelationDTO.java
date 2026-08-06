/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.dto.e2e;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class E2eOperationRelationDTO {

    // Local reference ids, valid only within this request payload — see E2eProductDTO.id.
    private Long operationVersionId;
    private Long relatedOperationVersionId;
    private String operationId;
    private String relatedOperationId;
    private Integer order;
    private String stereoType;
}
