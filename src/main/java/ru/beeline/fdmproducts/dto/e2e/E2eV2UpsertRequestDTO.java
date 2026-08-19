/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.dto.e2e;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class E2eV2UpsertRequestDTO {

    private E2eInfoDTO e2e;
    private List<E2eV2OperationRelationDTO> operationsRelations;
    private List<E2eProductDTO> products;
    private List<E2eV2InterfaceDTO> interfaces;
    private List<E2eV2OperationDTO> operations;
}
