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
@Schema(description = "Обнаруженный интерфейс с составом операций, публикуемый из задачи проекта")
public class ProjectDiscoveredInterfaceDTO {

    @Schema(description = "Код интерфейса (обязательное); сохраняется в discovered_interface.external_id "
            + "и вместе с product образует ключ синхронизации")
    private String interfaceCode;

    @Schema(description = "Alias (cmdb) продукта, которому принадлежит интерфейс (обязательное)")
    private String product;

    @Schema(description = "Полный состав операций интерфейса (обязательное поле); пустой список "
            + "помечает удалёнными все операции интерфейса")
    private List<ProjectDiscoveredOperationDTO> operations;
}
