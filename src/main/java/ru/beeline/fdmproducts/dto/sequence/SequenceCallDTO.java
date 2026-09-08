/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.dto.sequence;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Шаг sequence: вызов метода relatedOperationGuid со стороны operationGuid")
public class SequenceCallDTO {

    @Schema(description = "guid вызывающего метода из operations[]; не задан у стартового шага")
    private String operationGuid;

    @Schema(description = "guid вызываемого метода из operations[] (обязательное)")
    private String relatedOperationGuid;

    @Schema(description = "Порядковый номер шага (обязательное)")
    private Integer order;

    @Schema(description = "Описание шага (seq_product_step.raw_description)")
    private String description;
}
