/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.dto.e2e;

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
@Schema(description = "Карточка e2e с участвующими discovered-методами продукта")
public class E2eGroupedByE2eDTO {

    private Integer id;
    private String name;
    private String code;
    private String source;
    private List<E2eGroupedByE2eOperationDTO> operations;
}
