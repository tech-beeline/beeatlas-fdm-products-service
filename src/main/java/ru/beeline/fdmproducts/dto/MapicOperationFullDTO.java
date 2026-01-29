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
public class MapicOperationFullDTO {

    private Integer id;
    private String name;
    private String description;
    private String type;
    private String context;
    private String contextApi;
}
