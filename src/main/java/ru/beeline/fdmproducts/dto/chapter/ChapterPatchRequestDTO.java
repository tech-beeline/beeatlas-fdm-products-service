package ru.beeline.fdmproducts.dto.chapter;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChapterPatchRequestDTO {
    private String name;
    private String description;
    private String docLink;
    private List<Integer> nfr;
    private List<Integer> patterns;
}
