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
public class ProductE2eCardDTO {
    private Integer id;
    private String code;
    private String name;
    private String biStepCode;
    private String source;
    private Integer productId;
    private String productAlias;
}
