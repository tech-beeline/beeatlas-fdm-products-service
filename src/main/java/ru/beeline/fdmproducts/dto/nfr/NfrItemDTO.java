/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.dto.nfr;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NfrItemDTO {

    private Integer id;
    private String code;
    private Integer version;
    private String name;
    private String description;
    private String rule;
    private String source;
}
