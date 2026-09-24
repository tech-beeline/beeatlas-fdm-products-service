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
@Schema(description = "Результат создания/обновления sequence")
public class SequenceUpsertResponseDTO {

    @Schema(description = "Идентификатор sequence (product.sequence.id)")
    private Integer id;

    @Schema(description = "Код sequence")
    private String code;

    @Schema(description = "Идентификатор ветки продукта, в которой сохранён sequence")
    private Integer productBranchId;
}
