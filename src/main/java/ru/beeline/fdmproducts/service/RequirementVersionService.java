/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.beeline.fdmproducts.client.TechradarClient;
import ru.beeline.fdmproducts.controller.RequestContext;
import ru.beeline.fdmproducts.domain.Chapter;
import ru.beeline.fdmproducts.domain.ChapterNfr;
import ru.beeline.fdmproducts.domain.NonFunctionalRequirementEnum;
import ru.beeline.fdmproducts.domain.NonFunctionalRequirementEnumCore;
import ru.beeline.fdmproducts.domain.PatternRequirement;
import ru.beeline.fdmproducts.dto.CreateRequirementRequestDTO;
import ru.beeline.fdmproducts.dto.CreateRequirementVersionResponseDTO;
import ru.beeline.fdmproducts.exception.EntityNotFoundException;
import ru.beeline.fdmproducts.exception.NotAdministratorException;
import ru.beeline.fdmproducts.repository.ChapterNfrRepository;
import ru.beeline.fdmproducts.repository.ChapterRepository;
import ru.beeline.fdmproducts.repository.NonFunctionalRequirementEnumCoreRepository;
import ru.beeline.fdmproducts.repository.NonFunctionalRequirementEnumRepository;
import ru.beeline.fdmproducts.repository.PatternRequirementRepository;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RequirementVersionService {

    private final TechradarClient techradarClient;
    private final ChapterRepository chapterRepository;
    private final NonFunctionalRequirementEnumCoreRepository coreRepository;
    private final NonFunctionalRequirementEnumRepository enumRepository;
    private final ChapterNfrRepository chapterNfrRepository;
    private final PatternRequirementRepository patternRequirementRepository;

    @Transactional
    public CreateRequirementVersionResponseDTO createVersion(Integer coreId, String coreCode, CreateRequirementRequestDTO request) {
        if (!isAdministrator(RequestContext.getRoles())) {
            throw new NotAdministratorException("Пользователь не является администратором");
        }

        if (coreId != null && coreCode != null && !coreCode.isBlank()) {
            throw new IllegalArgumentException("Переданы несколько идентификаторов");
        }
        if (coreId == null && (coreCode == null || coreCode.isBlank())) {
            throw new IllegalArgumentException("Не передан идентификатор требования (id или code)");
        }

        if (request == null
                || isBlank(request.getName())
                || isBlank(request.getDescription())) {
            throw new IllegalArgumentException("Не переданы обязательные параметры");
        }

        NonFunctionalRequirementEnumCore core = findCore(coreId, coreCode);

        List<Integer> chapters = distinctIds(request.getChapters());
        List<Integer> patterns = distinctIds(request.getPatterns());

        if (!chapters.isEmpty()) {
            validateChaptersExist(chapters);
        }
        if (!patterns.isEmpty()) {
            validatePatternsExistInTechradar(patterns);
        }

        int nextVersion = computeNextVersion(core.getId());

        NonFunctionalRequirementEnum nfrEnum = enumRepository.save(NonFunctionalRequirementEnum.builder()
                .name(request.getName())
                .description(request.getDescription())
                .rule(request.getRule())
                .version(nextVersion)
                .core(core)
                .build());

        if (!chapters.isEmpty()) {
            List<Chapter> chapterEntities = chapterRepository.findAllById(chapters);
            Map<Integer, Chapter> chapterById = chapterEntities.stream()
                    .filter(c -> c.getId() != null)
                    .collect(Collectors.toMap(Chapter::getId, Function.identity(), (a, b) -> a));

            List<ChapterNfr> chapterNfrs = chapters.stream()
                    .map(chapterId -> ChapterNfr.builder()
                            .chapter(chapterById.get(chapterId))
                            .nfr(nfrEnum)
                            .build())
                    .collect(Collectors.toList());
            chapterNfrRepository.saveAll(chapterNfrs);
        }

        if (!patterns.isEmpty()) {
            List<PatternRequirement> patternRequirements = patterns.stream()
                    .map(patternId -> PatternRequirement.builder()
                            .patternId(patternId)
                            .nfr(nfrEnum)
                            .build())
                    .collect(Collectors.toList());
            patternRequirementRepository.saveAll(patternRequirements);
        }

        return CreateRequirementVersionResponseDTO.builder()
                .versionId(nfrEnum.getId())
                .build();
    }

    private NonFunctionalRequirementEnumCore findCore(Integer coreId, String coreCode) {
        Optional<NonFunctionalRequirementEnumCore> coreOpt;
        if (coreId != null) {
            coreOpt = coreRepository.findById(coreId);
        } else {
            coreOpt = coreRepository.findByCode(coreCode.trim());
        }
        return coreOpt.orElseThrow(() -> new EntityNotFoundException("Требование не найдено"));
    }

    private int computeNextVersion(Integer coreId) {
        List<NonFunctionalRequirementEnum> existing = enumRepository.findByCoreId(coreId);
        return existing.stream()
                .map(NonFunctionalRequirementEnum::getVersion)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .map(v -> v + 1)
                .orElse(1);
    }

    private void validateChaptersExist(List<Integer> chapterIds) {
        List<Chapter> found = chapterRepository.findAllById(chapterIds);
        if (found.size() != chapterIds.size()) {
            throw new IllegalArgumentException("В массиве chapters переданы идентификаторы несуществующих жизненных ситуаций");
        }
    }

    private void validatePatternsExistInTechradar(List<Integer> patterns) {
        var response = techradarClient.checkPatternsAvailability(patterns);
        if (response == null || Boolean.FALSE.equals(response.getAvailability())) {
            throw new IllegalArgumentException("В массиве patterns переданы идентификаторы несуществующих паттернов");
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static boolean isAdministrator(List<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return false;
        }
        return roles.stream()
                .filter(Objects::nonNull)
                .map(r -> r.toUpperCase(Locale.ROOT))
                .anyMatch(r -> r.contains("ADMINISTRATOR"));
    }

    private static List<Integer> distinctIds(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return ids.stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }
}

