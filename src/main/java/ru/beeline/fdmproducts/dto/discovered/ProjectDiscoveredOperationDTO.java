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
@Schema(description = "Операция обнаруженного интерфейса в теле запроса от сервиса project")
public class ProjectDiscoveredOperationDTO {

    @Schema(description = "Имя операции (обязательное); часть ключа upsert вместе с type")
    private String name;

    @Schema(description = "Тип операции (обязательное); часть ключа upsert вместе с name")
    private String type;

    @Schema(description = "Описание ТС (discovered_operation.tc_description)")
    private String descriptionTc;

    @Schema(description = "Код ТС в TeamCenter (discovered_operation.tc_code)")
    private String tcCode;

    @Schema(description = "Описание самой операции (discovered_operation.description)")
    private String descriptionOperation;
}
