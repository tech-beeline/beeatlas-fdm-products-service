/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.beeline.fdmproducts.domain.Chapter;
import ru.beeline.fdmproducts.domain.ChapterNfr;
import ru.beeline.fdmproducts.domain.NonFunctionalRequirementEnum;
import ru.beeline.fdmproducts.domain.NonFunctionalRequirementEnumCore;
import ru.beeline.fdmproducts.dto.ChapterWithNfrDTO;
import ru.beeline.fdmproducts.dto.NfrItemDTO;
import ru.beeline.fdmproducts.exception.EntityNotFoundException;
import ru.beeline.fdmproducts.repository.ChapterNfrRepository;
import ru.beeline.fdmproducts.repository.ChapterRepository;
import ru.beeline.fdmproducts.repository.NonFunctionalRequirementEnumRepository;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Transactional
@Service
@Slf4j
public class ChapterService {

    @Autowired
    private ChapterRepository chapterRepository;
    @Autowired
    private ChapterNfrRepository chapterNfrRepository;
    @Autowired
    private NonFunctionalRequirementEnumRepository nonFunctionalRequirementEnumRepository;

    public Chapter create(String name, String description, String docLink) {
        Chapter chapter = Chapter.builder()
                .name(name)
                .description(description)
                .docLink(docLink)
                .build();
        return chapterRepository.save(chapter);
    }

    public Chapter findById(Integer id) {
        return chapterRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Раздел не найден"));
    }

    public List<Chapter> findAll() {
        return chapterRepository.findAll();
    }

    public ChapterNfr addNfrToChapter(Integer chapterId, Integer nfrId) {
        Chapter chapter = findById(chapterId);
        NonFunctionalRequirementEnum nfr = nonFunctionalRequirementEnumRepository.findById(nfrId)
                .orElseThrow(() -> new EntityNotFoundException("NFR enum не найден"));

        ChapterNfr chapterNfr = ChapterNfr.builder()
                .chapter(chapter)
                .nfr(nfr)
                .build();
        return chapterNfrRepository.save(chapterNfr);
    }

    public List<ChapterNfr> findNfrsByChapterId(Integer chapterId) {
        return chapterNfrRepository.findByChapterId(chapterId);
    }

    /**
     * Получить список всех жизненных ситуаций (разделов) и требований к ним.
     * Для каждого chapter собирает связанные NFR, при наличии нескольких версий
     * одного требования (одинаковый core_id) оставляет только с максимальной версией.
     */
    public List<ChapterWithNfrDTO> getChaptersWithNfr() {
        List<Chapter> chapters = chapterRepository.findAll();

        return chapters.stream()
                .map(this::toChapterWithNfrDTO)
                .collect(Collectors.toList());
    }

    private ChapterWithNfrDTO toChapterWithNfrDTO(Chapter chapter) {
        List<ChapterNfr> chapterNfrs = chapterNfrRepository.findByChapterIdWithNfrAndCore(chapter.getId());

        List<NfrItemDTO> nfrList = chapterNfrs.stream()
                .map(ChapterNfr::getNfr)
                .filter(nfr -> nfr != null && nfr.getCore() != null)
                .collect(Collectors.groupingBy(nfr -> nfr.getCore().getId()))
                .values().stream()
                .map(nfrGroup -> nfrGroup.stream()
                        .max(Comparator.comparing(NonFunctionalRequirementEnum::getVersion,
                                Comparator.nullsFirst(Comparator.naturalOrder())))
                        .orElse(null))
                .filter(nfr -> nfr != null)
                .map(this::toNfrItemDTO)
                .collect(Collectors.toList());

        return ChapterWithNfrDTO.builder()
                .id(chapter.getId())
                .name(chapter.getName())
                .description(chapter.getDescription())
                .docLink(chapter.getDocLink())
                .nfr(nfrList)
                .build();
    }

    private NfrItemDTO toNfrItemDTO(NonFunctionalRequirementEnum nfr) {
        NonFunctionalRequirementEnumCore core = nfr.getCore();
        return NfrItemDTO.builder()
                .id(String.valueOf(nfr.getId()))
                .code(core != null ? core.getCode() : null)
                .version(nfr.getVersion())
                .name(nfr.getName())
                .description(nfr.getDescription())
                .rule(nfr.getRule())
                .source(core != null ? core.getSource() : null)
                .build();
    }
}
