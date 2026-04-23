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
public class PublishedApiDTO {

    private Integer id;
    private String apiContext;
    private String statusName;
    private Integer apiId;
}
