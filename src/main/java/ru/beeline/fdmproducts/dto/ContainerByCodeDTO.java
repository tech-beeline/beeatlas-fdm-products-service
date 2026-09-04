/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class ContainerByCodeDTO {
    private Integer id;
    private String name;
    private String code;
    private String productAlias;
    private String productName;
}
