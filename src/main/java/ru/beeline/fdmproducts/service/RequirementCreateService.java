/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpStatusCodeException;
import ru.beeline.fdmproducts.controller.RequestContext;
import ru.beeline.fdmproducts.client.TechradarClient;
import ru.beeline.fdmproducts.client.UserClient;
import ru.beeline.fdmproducts.domain.Chapter;
import ru.beeline.fdmproducts.domain.ChapterNfr;
import ru.beeline.fdmproducts.domain.NonFunctionalRequirementEnum;
import ru.beeline.fdmproducts.domain.NonFunctionalRequirementEnumCore;
import ru.beeline.fdmproducts.domain.PatternRequirement;
import ru.beeline.fdmproducts.dto.CreateRequirementRequestDTO;
import ru.beeline.fdmproducts.dto.CreateRequirementResponseDTO;
import ru.beeline.fdmproducts.exception.AuthServiceUnavailableException;
import ru.beeline.fdmproducts.exception.EntityNotFoundException;
import ru.beeline.fdmproducts.exception.NotAdministratorException;
import ru.beeline.fdmproducts.repository.ChapterNfrRepository;
import ru.beeline.fdmproducts.repository.ChapterRepository;
import ru.beeline.fdmproducts.repository.NonFunctionalRequirementEnumCoreRepository;
import ru.beeline.fdmproducts.repository.NonFunctionalRequirementEnumRepository;
import ru.beeline.fdmproducts.repository.PatternRequirementRepository;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RequirementCreateService {

    private final UserClient userClient;
    private final TechradarClient techradarClient;
    private final ChapterRepository chapterRepository;
    private final NonFunctionalRequirementEnumCoreRepository coreRepository;
    private final NonFunctionalRequirementEnumRepository enumRepository;
    private final ChapterNfrRepository chapterNfrRepository;
    private final PatternRequirementRepository patternRequirementRepository;

    @Transactional
    public CreateRequirementResponseDTO createRequirement(CreateRequirementRequestDTO request) {
        List<String> roles = RequestContext.getRoles();
        if (!isAdministrator(roles)) {
            throw new NotAdministratorException("Пользователь не является администратором");
        }

        String initiatorUserIdHeader = RequestContext.getUserId();
        if (request == null
                || isBlank(request.getName())
                || isBlank(request.getDescription())
                || isBlank(initiatorUserIdHeader)) {
            throw new IllegalArgumentException("Не переданы обязательные параметры");
        }

        Integer initiatorUserId;
        try {
            initiatorUserId = Integer.valueOf(initiatorUserIdHeader.trim());
        } catch (Exception e) {
            throw new IllegalArgumentException("Не переданы обязательные параметры");
        }

        List<Integer> chapters = distinctIds(request.getChapters());
        List<Integer> patterns = distinctIds(request.getPatterns());

        if (!chapters.isEmpty()) {
            validateChaptersExist(chapters);
        }

        if (!patterns.isEmpty()) {
            validatePatternsExistInTechradar(patterns);
        }

        String fullName = fetchInitiatorFullName(initiatorUserId);

        NonFunctionalRequirementEnumCore core = coreRepository.saveAndFlush(NonFunctionalRequirementEnumCore.builder()
                .source(fullName)
                .code(null)
                .build());
        core.setCode(buildCoreCode(core.getId()));
        core = coreRepository.save(core);

        NonFunctionalRequirementEnum nfrEnum = enumRepository.save(NonFunctionalRequirementEnum.builder()
                .name(request.getName())
                .description(request.getDescription())
                .rule(request.getRule())
                .version(1)
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

        return CreateRequirementResponseDTO.builder()
                .coreId(core.getId())
                .versionId(nfrEnum.getId())
                .build();
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

    private String fetchInitiatorFullName(Integer userId) {
        try {
            var profile = userClient.findUserProfileByIdStrict(userId);
            if (profile == null || profile.getFullName() == null || profile.getFullName().isBlank()) {
                throw new EntityNotFoundException("Пользователь, являющийся инициатором добавления требования к продукту, не найден");
            }
            return profile.getFullName();
        } catch (HttpStatusCodeException ex) {
            int status = ex.getStatusCode().value();
            if (status == 404) {
                throw new EntityNotFoundException("Пользователь, являющийся инициатором добавления требования к продукту, не найден");
            }
            if (String.valueOf(status).startsWith("5")) {
                throw new AuthServiceUnavailableException("Сервис Auth недоступен");
            }
            throw new AuthServiceUnavailableException("Сервис Auth недоступен");
        } catch (Exception ex) {
            throw new AuthServiceUnavailableException("Сервис Auth недоступен");
        }
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

    private static String buildCoreCode(Integer id) {
        String digits = String.format(Locale.ROOT, "%08d", id);
        String grouped = digits.substring(0, 2) + "."
                + digits.substring(2, 4) + "."
                + digits.substring(4, 6) + "."
                + digits.substring(6, 8);
        return "REQ." + grouped;
    }
}

