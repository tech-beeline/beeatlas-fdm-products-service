/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.dto.discovered;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Сохранённая операция: id для ссылок на стороне вызывающего сервиса")
public class ProjectDiscoveredOperationResultDTO {

    @Schema(description = "Идентификатор product.discovered_operation")
    private Integer discoveredOperationId;

    @Schema(description = "Имя операции — как в запросе")
    private String name;

    @Schema(description = "Тип операции — как в запросе")
    private String type;
}
