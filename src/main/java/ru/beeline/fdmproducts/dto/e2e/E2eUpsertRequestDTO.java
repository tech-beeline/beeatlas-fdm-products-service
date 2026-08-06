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
public class E2eUpsertRequestDTO {

    private E2eInfoDTO e2e;
    private List<E2eOperationRelationDTO> operationsRelations;
    private List<E2eProductDTO> products;
    private List<E2eContainerDTO> containers;
    private List<E2eInterfaceDTO> interfaces;
    private List<E2eOperationDTO> operations;
}
