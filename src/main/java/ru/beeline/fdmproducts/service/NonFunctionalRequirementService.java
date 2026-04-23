/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpStatusCodeException;
import ru.beeline.fdmproducts.client.TechradarClient;
import ru.beeline.fdmproducts.client.UserClient;
import ru.beeline.fdmproducts.domain.Chapter;
import ru.beeline.fdmproducts.domain.ChapterNfr;
import ru.beeline.fdmproducts.domain.LocalFitnessFunction;
import ru.beeline.fdmproducts.domain.NonFunctionalRequirement;
import ru.beeline.fdmproducts.domain.NonFunctionalRequirementEnum;
import ru.beeline.fdmproducts.domain.NonFunctionalRequirementEnumCore;
import ru.beeline.fdmproducts.domain.PatternRequirement;
import ru.beeline.fdmproducts.domain.Product;
import ru.beeline.fdmproducts.dto.ffunction.FitnessFunctionNfrDTO;
import ru.beeline.fdmproducts.dto.chapter.ChapterNfrDTO;
import ru.beeline.fdmproducts.dto.nfr.NfrDetailsDTO;
import ru.beeline.fdmproducts.dto.nfr.NfrItemProductDTO;
import ru.beeline.fdmproducts.dto.nfr.NfrPatternDTO;
import ru.beeline.fdmproducts.exception.EntityNotFoundException;
import ru.beeline.fdmproducts.repository.ChapterNfrRepository;
import ru.beeline.fdmproducts.repository.LocalFitnessFunctionRepository;
import ru.beeline.fdmproducts.repository.NonFunctionalRequirementEnumRepository;
import ru.beeline.fdmproducts.repository.NonFunctionalRequirementRepository;
import ru.beeline.fdmproducts.repository.PatternRequirementRepository;
import ru.beeline.fdmproducts.repository.ProductRepository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Transactional
@Service
@Slf4j
public class NonFunctionalRequirementService {

    @Autowired
    private NonFunctionalRequirementRepository nonFunctionalRequirementRepository;
    @Autowired
    private NonFunctionalRequirementEnumRepository nonFunctionalRequirementEnumRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private LocalFitnessFunctionRepository localFitnessFunctionRepository;
    @Autowired
    private ChapterNfrRepository chapterNfrRepository;
    @Autowired
    TechradarClient techradarClient;
    @Autowired
    private UserClient userClient;
    @Autowired
    private PatternRequirementRepository patternRequirementRepository;

    public NonFunctionalRequirement addRequirement(Integer productId, Integer nfrId, String source) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Продукт не найден"));
        NonFunctionalRequirementEnum nfr = nonFunctionalRequirementEnumRepository.findById(nfrId)
                .orElseThrow(() -> new EntityNotFoundException("NFR enum не найден"));

        NonFunctionalRequirement requirement = NonFunctionalRequirement.builder()
                .product(product)
                .nfr(nfr)
                .source(source)
                .createdDate(LocalDateTime.now())
                .build();

        return nonFunctionalRequirementRepository.save(requirement);
    }

    public void linkRequirementsToProduct(Integer productId, List<Integer> nfrIdsDistinct, String source, boolean userIdProvided) {
        if (nfrIdsDistinct == null || nfrIdsDistinct.isEmpty()) {
            return;
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Продукт не найден"));

        List<NonFunctionalRequirementEnum> enums = nonFunctionalRequirementEnumRepository.findAllById(nfrIdsDistinct);
        if (enums.size() != nfrIdsDistinct.size()) {
            throw new IllegalArgumentException("Передан несуществующий идентификатор требования");
        }
        Map<Integer, NonFunctionalRequirementEnum> enumById = enums.stream()
                .collect(Collectors.toMap(NonFunctionalRequirementEnum::getId, e -> e));

        List<NonFunctionalRequirement> existing = nonFunctionalRequirementRepository
                .findByProductIdAndNfrIds(productId, nfrIdsDistinct);
        Map<Integer, NonFunctionalRequirement> existingByNfrId = existing.stream()
                .filter(r -> r.getNfr() != null && r.getNfr().getId() != null)
                .collect(Collectors.toMap(r -> r.getNfr().getId(), r -> r, (a, b) -> a));

        LocalDateTime now = LocalDateTime.now();

        for (Integer nfrId : nfrIdsDistinct) {
            NonFunctionalRequirement current = existingByNfrId.get(nfrId);
            if (current == null) {
                NonFunctionalRequirement toCreate = NonFunctionalRequirement.builder()
                        .product(product)
                        .nfr(enumById.get(nfrId))
                        .source(source)
                        .createdDate(now)
                        .build();
                nonFunctionalRequirementRepository.save(toCreate);
                continue;
            }

            String currentSource = current.getSource();
            if (!userIdProvided
                    && currentSource != null
                    && !"Beeatlas".equals(currentSource)) {
                current.setSource("Beeatlas");
                current.setCreatedDate(now);
                nonFunctionalRequirementRepository.save(current);
            }
        }
    }

    public void addProductNfr(Integer id, String alias, String apiKey, String userIdHeader, List<Integer> nfrIds) {
        long providedCount = (id != null ? 1 : 0) + (alias != null && !alias.isBlank() ? 1 : 0) + (apiKey != null && !apiKey.isBlank() ? 1 : 0);
        if (providedCount == 0) {
            throw new IllegalArgumentException("Не передан один из идентификаторов приложения: id/alias/api-key");
        }
        if (providedCount > 1) {
            throw new IllegalArgumentException("Передано несколько идентификаторов приложения");
        }
        var productOpt = findProductByIdOrAliasOrApiKey(id, alias, apiKey);
        if (productOpt.isEmpty()) {
            throw new EntityNotFoundException("Продукт с указанным идентификатором не найден");
        }
        Integer productId = productOpt.get().getId();
        if (nfrIds == null || nfrIds.isEmpty()) {
            throw new IllegalArgumentException("Не передан ни один идентификатор требования");
        }
        List<Integer> nfrIdsDistinct = nfrIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (nfrIdsDistinct.isEmpty()) {
            throw new IllegalArgumentException("Не передан ни один идентификатор требования");
        }
        boolean userIdProvided = userIdHeader != null && !userIdHeader.isBlank();
        String source = "Beeatlas";
        if (userIdProvided) {
            Integer userId;
            try {
                userId = Integer.valueOf(userIdHeader.trim());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Пользователь, являющийся инициатором добавления требования к продукту, не найден");
            }
            try {
                var userProfile = userClient.findUserProfileByIdStrict(userId);
                if (userProfile == null || userProfile.getFullName() == null || userProfile.getFullName().isBlank()) {
                    throw new EntityNotFoundException("Пользователь, являющийся инициатором добавления требования к продукту, не найден");
                }
                source = userProfile.getFullName();
            } catch (HttpStatusCodeException ex) {
                if (ex.getStatusCode().value() == 404) {
                    throw new EntityNotFoundException("Пользователь, являющийся инициатором добавления требования к продукту, не найден");
                }
                if (String.valueOf(ex.getStatusCode().value()).startsWith("5")) {
                    throw new RuntimeException("Сервис Auth недоступен");
                }
                throw new RuntimeException("Сервис Auth недоступен");
            } catch (Exception ex) {
                throw new RuntimeException("Сервис Auth недоступен");
            }
        }
        try {
            linkRequirementsToProduct(productId, nfrIdsDistinct, source, userIdProvided);
        } catch (IllegalArgumentException ex) {
            if ("Передан несуществующий идентификатор требования".equals(ex.getMessage())) {
                throw new IllegalArgumentException("Передан несуществующий идентификатор требования");
            }
            throw new IllegalArgumentException(ex.getMessage());
        }
    }

    public List<NonFunctionalRequirement> findByProductId(Integer productId) {
        return nonFunctionalRequirementRepository.findByProductId(productId);
    }

    public List<NonFunctionalRequirement> findByNfrId(Integer nfrId) {
        return nonFunctionalRequirementRepository.findByNfrId(nfrId);
    }

    public void deleteById(Integer id) {
        nonFunctionalRequirementRepository.deleteById(id);
    }

    public Optional<Product> findProductByIdOrAliasOrApiKey(Integer id, String alias, String apiKey) {
        if (id != null) {
            return productRepository.findById(id);
        }
        if (alias != null && !alias.isBlank()) {
            Product product = productRepository.findByAliasCaseInsensitive(alias);
            return Optional.ofNullable(product);
        }
        if (apiKey != null && !apiKey.isBlank()) {
            Product product = productRepository.findByStructurizrApiKey(apiKey);
            return Optional.ofNullable(product);
        }
        return Optional.empty();
    }

    public Integer resolveProductId(Integer id, String alias, String apiKey) {
        long providedCount = (id != null ? 1 : 0)
                + (alias != null && !alias.isBlank() ? 1 : 0)
                + (apiKey != null && !apiKey.isBlank() ? 1 : 0);
        if (providedCount == 0) {
            throw new IllegalArgumentException("Не передан один из идентификаторов приложения: id/alias/api-key");
        }
        if (providedCount > 1) {
            throw new IllegalArgumentException("Передано несколько идентификаторов приложения");
        }
        var productOpt = findProductByIdOrAliasOrApiKey(id, alias, apiKey);
        if (productOpt.isEmpty()) {
            throw new EntityNotFoundException("Продукт с указанным идентификатором не найден");
        }
        return productOpt.get().getId();
    }

    public void deleteProductNfr(Integer productId, Integer reqId) {
        if (productId == null || reqId == null) {
            return;
        }
        var relOpt = nonFunctionalRequirementRepository.findByProduct_IdAndNfr_Id(productId, reqId);
        if (relOpt.isEmpty()) {
            return;
        }
        NonFunctionalRequirement rel = relOpt.get();
        if ("Beeatlas".equals(rel.getSource())) {
            throw new IllegalArgumentException("Требование назначенное автоматически, нельзя удалить вручную");
        }
        nonFunctionalRequirementRepository.delete(rel);
    }

    public void deleteProductNfr(Integer reqId, Integer id, String alias, String apiKey) {
        Integer productId = resolveProductId(id, alias, apiKey);
        deleteProductNfr(productId, reqId);
    }

    public List<NfrItemProductDTO> getProductNfr(Integer productId) {
        List<NonFunctionalRequirement> requirements = nonFunctionalRequirementRepository
                .findByProductIdWithNfrAndCore(productId);
        if (requirements.isEmpty()) {
            return List.of();
        }
        List<PatternRequirement> patternRequirements = patternRequirementRepository.findByNfrIdIn(requirements.stream()
                .map(NonFunctionalRequirement::getNfrId).collect(Collectors.toList()));
        Map<Integer, List<Integer>> patternMap = patternRequirements.stream()
                .collect(Collectors.groupingBy(
                        PatternRequirement::getNfrId,
                        Collectors.mapping(PatternRequirement::getPatternId, Collectors.toList())
                ));
        Map<Integer, NonFunctionalRequirement> latestByCore = requirements.stream()
                .filter(req -> req.getNfr() != null && req.getNfr().getCore() != null)
                .collect(Collectors.toMap(
                        req -> req.getNfr().getCore().getId(),
                        Function.identity(),
                        this::compareByVersion
                ));

        List<Integer> allPatternIds = patternMap.values().stream()
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Integer, NfrPatternDTO> patternById = (allPatternIds.isEmpty()
                ? List.<NfrPatternDTO>of()
                : techradarClient.getPatternsByIds(allPatternIds))
                .stream()
                .filter(Objects::nonNull)
                .filter(p -> p.getId() != null)
                .collect(Collectors.toMap(NfrPatternDTO::getId, Function.identity(), (a, b) -> a));

        return latestByCore.values().stream()
                .map(req -> {
                    List<Integer> ids = patternMap.get(req.getNfrId());
                    List<NfrPatternDTO> resolved = (ids == null ? List.<Integer>of() : ids).stream()
                            .filter(Objects::nonNull)
                            .distinct()
                            .map(patternById::get)
                            .filter(Objects::nonNull)
                            .toList();
                    return toNfrItemProductDTO(req, resolved);
                })
                .toList();
    }

    private NonFunctionalRequirement compareByVersion(NonFunctionalRequirement req1, NonFunctionalRequirement req2) {
        Integer v1 = req1.getNfr() != null ? req1.getNfr().getVersion() : null;
        Integer v2 = req2.getNfr() != null ? req2.getNfr().getVersion() : null;
        if (v1 == null) return req2;
        if (v2 == null) return req1;
        return v1 > v2 ? req1 : req2;
    }

    private NfrItemProductDTO toNfrItemProductDTO(NonFunctionalRequirement requirement,
                                                  List<NfrPatternDTO> patterns) {
        NonFunctionalRequirementEnum nfr = requirement.getNfr();
        NonFunctionalRequirementEnumCore core = nfr.getCore();
        List<LocalFitnessFunction> localFitnessFunctions = getFitnessFunctionsFromRule(nfr.getRule());
        List<FitnessFunctionNfrDTO> fitnessFunctionNfrDTOS = buildFitnessFunctionNfrDTO(localFitnessFunctions);
        List<ChapterNfr> chapterNfrs = chapterNfrRepository.findByNfrId(nfr.getId());
        List<Chapter> chapters = chapterNfrs.stream().map(ChapterNfr::getChapter).filter(Objects::nonNull).toList();
        List<ChapterNfrDTO> chapterNfrDTOS = buildChapterNfrDTO(chapters);
        return NfrItemProductDTO.builder()
                .id(nfr.getId())
                .code(core != null ? core.getCode() : null)
                .version(nfr.getVersion())
                .name(nfr.getName())
                .createdDate(requirement.getCreatedDate())
                .description(nfr.getDescription())
                .patterns(patterns != null ? patterns : new ArrayList<>())
                .fitnessFunctions(fitnessFunctionNfrDTOS)
                .chapters(chapterNfrDTOS)
                .source(core != null ? core.getSource() : null)
                .sourcePurpose(requirement.getSource())
                .build();
    }

    private List<LocalFitnessFunction> getFitnessFunctionsFromRule(String rule) {
        if (rule == null || rule.trim().isEmpty()) {
            log.warn("Rule is null or empty");
            return new ArrayList<>();
        }
        String ruleWithoutSpaces = rule.replaceAll("\\s+", "");
        List<String> lowerCodes = Arrays.stream(ruleWithoutSpaces.split(","))
                .map(String::trim)
                .filter(code -> !code.isEmpty())
                .map(code -> code.toLowerCase(Locale.ROOT))
                .distinct()
                .collect(Collectors.toList());
        if (lowerCodes.isEmpty()) {
            log.warn("No valid codes found in rule: {}", rule);
            return new ArrayList<>();
        }
        List<LocalFitnessFunction> fitnessFunctions = localFitnessFunctionRepository.findByCodeInIgnoreCase(lowerCodes);
        log.info("Found {}/{} fitness functions for codes (ignore case): {}",
                fitnessFunctions.size(), lowerCodes.size(), lowerCodes);
        return fitnessFunctions;
    }

    private List<FitnessFunctionNfrDTO> buildFitnessFunctionNfrDTO(List<LocalFitnessFunction> fitnessFunctions) {
        List<FitnessFunctionNfrDTO> result = new ArrayList<>();
        for (LocalFitnessFunction obj : fitnessFunctions) {
            result.add(FitnessFunctionNfrDTO.builder()
                    .id(obj.getId())
                    .docLink(obj.getDocLink())
                    .code(obj.getCode())
                    .description(obj.getDescription())
                    .build());
        }
        return result;
    }

    private List<ChapterNfrDTO> buildChapterNfrDTO(List<Chapter> chapters) {
        List<ChapterNfrDTO> result = new ArrayList<>();
        for (Chapter chapter : chapters) {
            result.add(ChapterNfrDTO.builder()
                    .id(chapter.getId())
                    .code(chapter.getCode())
                    .name(chapter.getName())
                    .description(chapter.getDescription())
                    .docLink(chapter.getDocLink())
                    .build());
        }
        return result;
    }

    public NfrDetailsDTO getNfrDetails(Integer id) {
        NonFunctionalRequirementEnum nfr = nonFunctionalRequirementEnumRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Требование не найдено"));
        NonFunctionalRequirementEnumCore core = nfr.getCore();
        List<LocalFitnessFunction> localFitnessFunctions = getFitnessFunctionsFromRule(nfr.getRule());
        List<FitnessFunctionNfrDTO> fitnessFunctions = buildFitnessFunctionNfrDTO(localFitnessFunctions);
        List<ChapterNfr> chapterNfrs = chapterNfrRepository.findByNfrId(nfr.getId());
        List<Chapter> chapters = chapterNfrs.stream().map(ChapterNfr::getChapter).filter(Objects::nonNull).toList();
        List<ChapterNfrDTO> chapterDtos = buildChapterNfrDTO(chapters);
        List<PatternRequirement> patternRequirements = patternRequirementRepository.findByNfrId(nfr.getId());
        List<Integer> patternIds = patternRequirements.stream()
                .map(PatternRequirement::getPatternId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        List<NfrPatternDTO> patterns = patternIds.isEmpty() ? List.of() : techradarClient.getPatternsByIds(patternIds);
        return NfrDetailsDTO.builder()
                .id(nfr.getId())
                .code(core != null ? core.getCode() : null)
                .version(nfr.getVersion())
                .name(nfr.getName())
                .description(nfr.getDescription())
                .fitnessFunctions(fitnessFunctions)
                .chapters(chapterDtos)
                .patterns(patterns)
                .build();
    }
}
