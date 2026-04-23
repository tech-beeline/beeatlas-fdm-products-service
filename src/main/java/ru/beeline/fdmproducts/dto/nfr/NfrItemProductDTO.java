/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.dto.nfr;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.beeline.fdmproducts.dto.chapter.ChapterNfrDTO;
import ru.beeline.fdmproducts.dto.ffunction.FitnessFunctionNfrDTO;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NfrItemProductDTO {

    private Integer id;
    private String code;
    private Integer version;
    private String name;
    private String description;
    private LocalDateTime createdDate;
    private List<FitnessFunctionNfrDTO> fitnessFunctions;
    private List<ChapterNfrDTO> chapters;
    private List<NfrPatternDTO> patterns;
    private String source;
    private String sourcePurpose;
}
