/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.dto.sequence;

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
@Schema(description = "Тело запроса на создание/обновление sequence")
public class SequenceUpsertRequestDTO {

    @Schema(description = "Карточка sequence (обязательное)")
    private SequenceInfoDTO sequence;

    @Schema(description = "Шаги sequence; ссылаются на operations[] по guid (обязательное)")
    private List<SequenceCallDTO> sequenceCall;

    @Schema(description = "Справочник методов, используемых шагами (обязательное)")
    private List<SequenceOperationDTO> operations;
}
