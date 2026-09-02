/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.dto.e2e;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * PATCH /api/v2/e2e/{code}: every field is optional and independently applied — a field left
 * out of the request is left untouched, unlike POST upsert, which always fully replaces
 * operationsRelations and requires an existing e2e to be found (never creates one).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class E2eV2PatchRequestDTO {

    private String name;
    private String description;
    private String biStepCode;
    private List<E2eProductDTO> products;
    private List<E2eV2InterfaceDTO> interfaces;
    private List<E2eV2OperationDTO> operations;
    private List<E2eV2OperationRelationDTO> operationsRelations;
}
