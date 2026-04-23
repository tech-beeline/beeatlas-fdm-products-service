package ru.beeline.fdmproducts.dto.nfr;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.beeline.fdmproducts.dto.ffunction.FitnessFunctionNfrDTO;
import ru.beeline.fdmproducts.dto.chapter.ChapterNfrDTO;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NfrDetailsDTO {

    private Integer id;
    private String code;
    private Integer version;
    private String name;
    private String description;
    private List<FitnessFunctionNfrDTO> fitnessFunctions;
    private List<ChapterNfrDTO> chapters;
    private List<NfrPatternDTO> patterns;
}