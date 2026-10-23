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
public class E2eCardResponseDTO {

    private Integer id;
    private String code;
    private String name;
    private String description;
    private String biStepCode;
    private String source;
    /** Продукт-владелец: product.e2e.product_id, без владельца — null. */
    private Integer productId;
    /** product.alias владельца как записан в таблице, без владельца — null. */
    private String productAlias;
}
