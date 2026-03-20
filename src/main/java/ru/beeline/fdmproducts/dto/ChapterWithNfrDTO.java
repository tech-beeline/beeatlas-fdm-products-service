/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.dto;

import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChapterWithNfrDTO {

    private Integer id;
    private String name;
    private String description;
    private String docLink;
    private List<NfrItemDTO> nfr;
}
