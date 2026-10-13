/*
 * Copyright (c) 2026 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.beeline.fdmproducts.domain.ChapterPattern;
import ru.beeline.fdmproducts.dto.PatternUsageDTO;
import ru.beeline.fdmproducts.repository.ChapterPatternRepository;
import ru.beeline.fdmproducts.repository.ChapterRepository;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class PatternUsageService {

    private static final String INVALID_ID_MESSAGE = "Некорректный id";

    private final ChapterPatternRepository chapterPatternRepository;
    private final ChapterRepository chapterRepository;
    private final PatternRequirementService patternRequirementService;

    @Transactional(readOnly = true)
    public PatternUsageDTO getUsage(String rawId) {
        Integer patternId = parseId(rawId);
        return PatternUsageDTO.builder()
                .chapters(findChapters(patternId))
                .nfr(findActualNfr(patternId))
                .build();
    }

    private List<PatternUsageDTO.ChapterItem> findChapters(Integer patternId) {
        List<Integer> chapterIds = chapterPatternRepository.findAllByPatternId(patternId).stream()
                .map(ChapterPattern::getChapterId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (chapterIds.isEmpty()) {
            return List.of();
        }
        return chapterRepository.findAllById(chapterIds).stream()
                .map(chapter -> PatternUsageDTO.ChapterItem.builder()
                        .id(chapter.getId())
                        .code(chapter.getCode())
                        .name(chapter.getName())
                        .build())
                .sorted(Comparator.comparing(PatternUsageDTO.ChapterItem::getCode,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    private List<PatternUsageDTO.NfrItem> findActualNfr(Integer patternId) {
        return patternRequirementService.getNfrByPatternId(patternId).stream()
                .map(nfr -> PatternUsageDTO.NfrItem.builder()
                        .id(nfr.getId())
                        .code(nfr.getCode())
                        .version(nfr.getVersion())
                        .name(nfr.getName())
                        .build())
                .sorted(Comparator.comparing(PatternUsageDTO.NfrItem::getCode,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    private Integer parseId(String rawId) {
        if (rawId == null || !rawId.matches("\\d{1,9}")) {
            throw new IllegalArgumentException(INVALID_ID_MESSAGE);
        }
        int id = Integer.parseInt(rawId);
        if (id <= 0) {
            throw new IllegalArgumentException(INVALID_ID_MESSAGE);
        }
        return id;
    }
}
