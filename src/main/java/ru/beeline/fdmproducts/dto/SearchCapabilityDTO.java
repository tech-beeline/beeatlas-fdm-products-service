/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.dto;

import lombok.*;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchCapabilityDTO {

    private Integer id;
    private String code;
    private String name;
    private String description;
    private String type;
}
