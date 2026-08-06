/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.dto.nfr;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.beeline.fdmproducts.dto.chapter.ChapterNfrDTO;
import ru.beeline.fdmproducts.dto.ffunction.FitnessFunctionNfrV2DTO;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NfrDetailsV2DTO {

    private Integer id;
    private String code;
    private Integer version;
    private String name;
    private String description;
    private List<FitnessFunctionNfrV2DTO> fitnessFunctions;
    private List<ChapterNfrDTO> chapters;
    private List<NfrPatternDTO> patterns;
}
