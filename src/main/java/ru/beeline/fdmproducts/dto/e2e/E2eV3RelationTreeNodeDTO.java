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

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Шаг дерева вызовов e2e: связь product.operation_relations, узел — id discovered_operation")
public class E2eV3RelationTreeNodeDTO {

    @Schema(description = "Порядок вызова (operation_relations.\"order\")")
    private Integer order;

    @Schema(description = "Вызываемый метод: id product.discovered_operation")
    private Integer relatedOperationId;

    @Schema(description = "Стереотип связи, пустой — null")
    private String stereotype;

    @Builder.Default
    @Schema(description = "Дочерние шаги: связи, у которых operation_id равен relatedOperationId узла")
    private List<E2eV3RelationTreeNodeDTO> operationsRelations = new ArrayList<>();
}
