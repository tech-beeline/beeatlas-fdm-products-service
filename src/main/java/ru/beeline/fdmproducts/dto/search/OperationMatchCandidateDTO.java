/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.dto.search;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Кандидат на сопоставление: метод, для которого ищутся архитектурные операции")
public class OperationMatchCandidateDTO {

    @Schema(description = "Имя метода (обязательное); сравнивается с operation.name без учёта регистра",
            example = "/api/v1/orders")
    private String methodName;

    @Schema(description = "Тип операции; если не передан — поиск по имени без фильтра по типу", example = "GET")
    private String methodType;

    @Schema(description = "Протокол интерфейса; если передан, интерфейсы с protocol IS NULL не совпадают",
            example = "REST")
    private String protocol;

    @Schema(description = "Код продукта (обязательное) — уникальный product.alias", example = "orders-product")
    private String productCode;
}
