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
@Schema(description = "Карточка sequence")
public class SequenceInfoDTO {

    @Schema(description = "Стабильный код sequence в ветке (product.sequence.code) (обязательное)")
    private String uid;

    @Schema(description = "Название sequence (обязательное)")
    private String name;

    @Schema(description = "Описание sequence")
    private String description;

    @Schema(description = "Код в TeamCenter (product.sequence.tc_code)")
    private String tcCode;
}
