/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.dto.usecase;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.beeline.fdmproducts.dto.e2e.E2eGroupedByE2eOperationDTO;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Карточка use case с участвующими discovered-методами продукта")
public class UseCaseGroupedByUseCaseDTO {

    private Integer id;
    private String name;
    private String code;
    private List<E2eGroupedByE2eOperationDTO> operations;
}
