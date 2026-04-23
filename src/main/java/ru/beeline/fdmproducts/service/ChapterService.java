/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.beeline.fdmproducts.client.TechradarClient;
import ru.beeline.fdmproducts.domain.Chapter;
import ru.beeline.fdmproducts.domain.ChapterNfr;
import ru.beeline.fdmproducts.domain.ChapterPattern;
import ru.beeline.fdmproducts.domain.NonFunctionalRequirementEnum;
import ru.beeline.fdmproducts.domain.NonFunctionalRequirementEnumCore;
import ru.beeline.fdmproducts.dto.chapter.ChapterCreateDTO;
import ru.beeline.fdmproducts.dto.chapter.ChapterCreateRequestDTO;
import ru.beeline.fdmproducts.dto.chapter.ChapterPatchRequestDTO;
import ru.beeline.fdmproducts.dto.chapter.ChapterWithNfrDTO;
import ru.beeline.fdmproducts.dto.nfr.NfrItemDTO;
import ru.beeline.fdmproducts.dto.ProductAvailableDTO;
import ru.beeline.fdmproducts.exception.EntityNotFoundException;
import ru.beeline.fdmproducts.exception.ForbiddenException;
import ru.beeline.fdmproducts.repository.ChapterNfrRepository;
import ru.beeline.fdmproducts.repository.ChapterPatternRepository;
import ru.beeline.fdmproducts.repository.ChapterRepository;
import ru.beeline.fdmproducts.repository.NonFunctionalRequirementEnumRepository;

import java.util.*;
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
    @Autowired
    private ChapterPatternRepository chapterPatternRepository;
    @Autowired
    private TechradarClient techradarClient;

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

    public List<Integer> getPatternIdsByChapterId(Integer chapterId) {
        boolean exists = chapterRepository.existsById(chapterId);
        if (!exists) {
            throw new EntityNotFoundException("Chapter с таким id не существует");
        }
        List<ChapterPattern> rows = chapterPatternRepository.findAllByChapterId(chapterId);
        return rows.stream().map(ChapterPattern::getPatternId).toList();
    }

    public ChapterCreateDTO createChapter(ChapterCreateRequestDTO body, String userRoles) {
        ensureAdministratorRole(userRoles);
        if (body == null
                || body.getName() == null || body.getName().isBlank()
                || body.getDescription() == null || body.getDescription().isBlank()) {
            throw new IllegalArgumentException("Не переданы обязательные параметры");
        }
        List<Integer> nfrIdsDistinct = body.getNfr() == null ? List.of() : body.getNfr().stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (!nfrIdsDistinct.isEmpty()) {
            List<NonFunctionalRequirementEnum> enums = nonFunctionalRequirementEnumRepository.findAllById(nfrIdsDistinct);
            if (enums.size() != nfrIdsDistinct.size()) {
                throw new IllegalArgumentException("В массиве nfr переданы идентификаторы несуществующих требований");
            }
        }
        List<Integer> patternIdsDistinct = body.getPatterns() == null ? List.of() : body.getPatterns().stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (!patternIdsDistinct.isEmpty()) {
            ProductAvailableDTO availability = techradarClient.checkPatternsAvailability(patternIdsDistinct);
            if (availability == null || Boolean.FALSE.equals(availability.getAvailability())) {
                throw new IllegalArgumentException("В массиве patterns переданы идентификаторы несуществующих паттернов");
            }
        }
        Chapter chapter = Chapter.builder()
                .name(body.getName())
                .description(body.getDescription())
                .docLink(body.getDocLink())
                .build();
        chapter = chapterRepository.save(chapter);
        chapter.setCode(generateChapterCode(chapter.getId()));
        chapter = chapterRepository.save(chapter);
        if (!nfrIdsDistinct.isEmpty()) {
            List<NonFunctionalRequirementEnum> enums = nonFunctionalRequirementEnumRepository.findAllById(nfrIdsDistinct);
            for (NonFunctionalRequirementEnum nfr : enums) {
                ChapterNfr link = ChapterNfr.builder()
                        .chapter(chapter)
                        .nfr(nfr)
                        .build();
                chapterNfrRepository.save(link);
            }
        }
        if (!patternIdsDistinct.isEmpty()) {
            for (Integer patternId : patternIdsDistinct) {
                ChapterPattern link = ChapterPattern.builder()
                        .chapterId(chapter.getId())
                        .patternId(patternId)
                        .build();
                chapterPatternRepository.save(link);
            }
        }
        return ChapterCreateDTO.builder()
                .id(chapter.getId())
                .build();
    }

    private String generateChapterCode(Integer id) {
        String digits = String.format("%08d", id);
        return "CH."
                + digits.substring(0, 2) + "."
                + digits.substring(2, 4) + "."
                + digits.substring(4, 6) + "."
                + digits.substring(6, 8);
    }

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
                .code(chapter.getCode())
                .docLink(chapter.getDocLink())
                .nfr(nfrList)
                .build();
    }

    private NfrItemDTO toNfrItemDTO(NonFunctionalRequirementEnum nfr) {
        NonFunctionalRequirementEnumCore core = nfr.getCore();
        return NfrItemDTO.builder()
                .id(nfr.getId())
                .code(core != null ? core.getCode() : null)
                .version(nfr.getVersion())
                .name(nfr.getName())
                .description(nfr.getDescription())
                .rule(nfr.getRule())
                .source(core != null ? core.getSource() : null)
                .build();
    }

    public void patchChapter(Integer id, String code, String userRoles, ChapterPatchRequestDTO body) {
        validateIdentifierParams(id, code);
        ensureAdministratorRole(userRoles);
        Integer resolvedId = resolveChapterId(id, code);
        boolean emptyBody = body == null
                || (body.getName() == null
                && body.getDescription() == null
                && body.getDocLink() == null
                && body.getNfr() == null
                && body.getPatterns() == null);
        if (emptyBody) {
            boolean exists = chapterRepository.existsById(resolvedId);
            if (!exists) {
                throw new EntityNotFoundException("Жизненная ситуация не найдена");
            }
            return;
        }
        patchChapterByResolvedId(resolvedId, body);
    }

    private void patchChapterByResolvedId(Integer resolvedId, ChapterPatchRequestDTO body) {
        Chapter chapter = chapterRepository.findById(resolvedId)
                .orElseThrow(() -> new EntityNotFoundException("Жизненная ситуация не найдена"));
        if (body.getName() != null) {
            chapter.setName(body.getName());
        }
        if (body.getDescription() != null) {
            chapter.setDescription(body.getDescription());
        }
        if (body.getDocLink() != null) {
            chapter.setDocLink(body.getDocLink());
        }
        chapterRepository.save(chapter);
        if (body.getNfr() != null) {
            synchronizeChapterNfr(chapter, body.getNfr());
        }
        if (body.getPatterns() != null) {
            synchronizeChapterPatterns(chapter.getId(), body.getPatterns());
        }
    }

    private void validateIdentifierParams(Integer id, String code) {
        if (id != null && code != null) {
            throw new IllegalArgumentException("Переданы несколько идентификаторов");
        }
        if (id == null && (code == null || code.isBlank())) {
            throw new IllegalArgumentException("Не передан идентификатор главы (id или code)");
        }
    }

    private void ensureAdministratorRole(String userRoles) {
        List<String> roles = userRoles == null ? List.of() : Arrays.stream(userRoles.split(","))
                .map(role -> role.replaceAll("^[^a-zA-Z]+|[^a-zA-Z]+$", ""))
                .filter(str -> str != null && !str.isBlank())
                .toList();
        if (!roles.contains("ADMINISTRATOR")) {
            throw new ForbiddenException("Пользователь не является администратором");
        }
    }

    private Integer resolveChapterId(Integer id, String code) {
        if (id != null) {
            return id;
        }
        List<Chapter> byCode = chapterRepository.findAllByCode(code);
        if (byCode.size() != 1) {
            throw new EntityNotFoundException("Жизненная ситуация не найдена");
        }
        return byCode.get(0).getId();
    }

    private void synchronizeChapterNfr(Chapter chapter, List<Integer> nfrIdsRaw) {
        List<Integer> target = nfrIdsRaw == null ? List.of() : nfrIdsRaw.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (!target.isEmpty()) {
            List<NonFunctionalRequirementEnum> enums = nonFunctionalRequirementEnumRepository.findAllById(target);
            if (enums.size() != target.size()) {
                throw new IllegalArgumentException("В массиве nfr переданы идентификаторы несуществующих требований");
            }
        }

        List<ChapterNfr> existing = chapterNfrRepository.findByChapterId(chapter.getId());
        Set<Integer> targetSet = target.stream().collect(Collectors.toSet());
        List<ChapterNfr> toRemove = existing.stream()
                .filter(link -> link.getNfr() == null || link.getNfr().getId() == null || !targetSet.contains(link.getNfr().getId()))
                .toList();
        Set<Integer> existingIds = existing.stream()
                .map(ChapterNfr::getNfr)
                .filter(Objects::nonNull)
                .map(NonFunctionalRequirementEnum::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        List<Integer> toAddIds = target.stream()
                .filter(nfrId -> !existingIds.contains(nfrId))
                .toList();
        if (!toRemove.isEmpty()) {
            chapterNfrRepository.deleteAll(toRemove);
        }
        if (!toAddIds.isEmpty()) {
            List<NonFunctionalRequirementEnum> enums = nonFunctionalRequirementEnumRepository.findAllById(toAddIds);
            Map<Integer, NonFunctionalRequirementEnum> byId = new HashMap<>();
            for (NonFunctionalRequirementEnum e : enums) {
                byId.put(e.getId(), e);
            }
            List<ChapterNfr> toCreate = toAddIds.stream()
                    .map(nfrId -> ChapterNfr.builder()
                            .chapter(chapter)
                            .nfr(byId.get(nfrId))
                            .build())
                    .toList();
            chapterNfrRepository.saveAll(toCreate);
        }
    }

    private void synchronizeChapterPatterns(Integer chapterId, List<Integer> patternIdsRaw) {
        List<Integer> target = patternIdsRaw == null ? List.of() : patternIdsRaw.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (!target.isEmpty()) {
            ProductAvailableDTO availability = techradarClient.checkPatternsAvailability(target);
            if (availability == null || Boolean.FALSE.equals(availability.getAvailability())) {
                throw new IllegalArgumentException("В массиве patterns переданы идентификаторы несуществующих паттернов");
            }
        }
        List<ChapterPattern> existing = chapterPatternRepository.findAllByChapterId(chapterId);
        Set<Integer> targetSet = new HashSet<>(target);
        List<ChapterPattern> toRemove = existing.stream()
                .filter(link -> link.getPatternId() == null || !targetSet.contains(link.getPatternId()))
                .toList();
        Set<Integer> existingIds = existing.stream().map(ChapterPattern::getPatternId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        List<ChapterPattern> toCreate = target.stream().filter(patternId -> !existingIds.contains(patternId))
                .map(patternId -> ChapterPattern.builder()
                        .chapterId(chapterId)
                        .patternId(patternId)
                        .build())
                .toList();
        if (!toRemove.isEmpty()) {
            chapterPatternRepository.deleteAll(toRemove);
        }
        if (!toCreate.isEmpty()) {
            chapterPatternRepository.saveAll(toCreate);
        }
    }
}
