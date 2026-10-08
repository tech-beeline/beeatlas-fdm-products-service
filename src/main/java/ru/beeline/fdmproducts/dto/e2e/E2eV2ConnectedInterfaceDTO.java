/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.dto.e2e;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Интерфейс канонической операции (только id и code) с контейнером")
public class E2eV2ConnectedInterfaceDTO {

    private Integer id;
    private String code;
    private E2eV2ConnectedContainerDTO container;
}
