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
public class E2eGetResponseDTO {

    private E2eCardResponseDTO e2e;
    private List<E2eRelationTreeNodeDTO> operationsRelations = new ArrayList<>();
    private List<E2eOperationCatalogItemDTO> operations = new ArrayList<>();
}
