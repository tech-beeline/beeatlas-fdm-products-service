/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.dto.e2e;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class E2eInfoDTO {

    private String uid;
    private String name;
    private String description;
    private String biStepCode;
    /**
     * Продукт-владелец (product.alias), POST /api/v2/e2e. Не передан или null — при создании владельца нет,
     * при обновлении не меняется; пустая строка снимает владельца.
     */
    private String productAlias;
}
