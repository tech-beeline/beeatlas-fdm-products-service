/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.dto.discovered;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Результат upsert интерфейса: ключ (interfaceCode, product) и id сохранённых операций")
public class ProjectDiscoveredInterfaceResultDTO {

    @Schema(description = "Идентификатор product.discovered_interface")
    private Integer id;

    @Schema(description = "Код интерфейса — как в запросе")
    private String interfaceCode;

    @Schema(description = "Alias продукта — как в запросе")
    private String product;

    @Schema(description = "Сохранённые операции интерфейса, в порядке запроса")
    private List<ProjectDiscoveredOperationResultDTO> operations;
}
