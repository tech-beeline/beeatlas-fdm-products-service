/*
 * Copyright (c) 2026 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatternUsageDTO {

    private List<ChapterItem> chapters;
    private List<NfrItem> nfr;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChapterItem {
        private Integer id;
        private String code;
        private String name;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NfrItem {
        private Integer id;
        private String code;
        private Integer version;
        private String name;
    }
}
