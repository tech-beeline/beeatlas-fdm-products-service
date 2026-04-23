/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.dto.chapter;

import lombok.*;
import ru.beeline.fdmproducts.dto.nfr.NfrItemDTO;

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
    private String code;
    private List<NfrItemDTO> nfr;
}
