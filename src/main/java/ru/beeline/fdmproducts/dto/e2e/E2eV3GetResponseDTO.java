/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.dto.e2e;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/** GET /api/v3/e2e/{code} (SFDM-4239): карточка, дерево вызовов и справочник discoveredOperations, без operations. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "e2e с деревом вызовов по discovered_operation")
public class E2eV3GetResponseDTO {

    @Schema(description = "Карточка e2e с продуктом-владельцем (productId, productAlias — null без владельца)")
    private E2eCardResponseDTO e2e;

    @Builder.Default
    @Schema(description = "Корни дерева — связи с пустым operation_id; порядок по order, затем по id связи")
    private List<E2eV3RelationTreeNodeDTO> operationsRelations = new ArrayList<>();

    @Builder.Default
    @Schema(description = "Методы всех узлов дерева без повторов, в порядке первого появления. "
            + "operation — сопоставленная операция архитектуры по connection_operation_id или null")
    private List<E2eV2DiscoveredOperationCatalogItemDTO> discoveredOperations = new ArrayList<>();
}
