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
@Schema(description = "Элемент справочника методов запроса: атрибуты для резолва и для сохранения в sequence_step_operation")
public class SequenceOperationDTO {

    @Schema(description = "Уникальный в рамках запроса идентификатор метода (обязательное)")
    private String guid;

    @Schema(description = "Alias продукта, которому принадлежит метод (обязательное)")
    private String productAlias;

    @Schema(description = "Код контейнера; если не задан — резолв идёт по всем контейнерам ветки продукта")
    private String containerCode;

    @Schema(description = "Код интерфейса; если не задан — резолв идёт по всем интерфейсам контейнера")
    private String interfaceCode;

    @Schema(description = "Имя операции (обязательное)")
    private String name;

    @Schema(description = "Тип операции (обязательное)")
    private String type;

    @Schema(description = "Описание метода; не сохраняется — описание шага берётся из sequenceCall.description")
    private String description;
}
