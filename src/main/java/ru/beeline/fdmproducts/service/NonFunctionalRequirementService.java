/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpStatusCodeException;
import ru.beeline.fdmproducts.client.FfManagerClient;
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
import ru.beeline.fdmproducts.domain.ProductBranch;
import ru.beeline.fdmproducts.dto.chapter.ChapterNfrDTO;
import ru.beeline.fdmproducts.dto.ffmanager.FfManagerFitnessFunctionDTO;
import ru.beeline.fdmproducts.dto.ffunction.FitnessFunctionNfrDTO;
import ru.beeline.fdmproducts.dto.ffunction.FitnessFunctionNfrV2DTO;
import ru.beeline.fdmproducts.dto.nfr.NfrDetailsDTO;
import ru.beeline.fdmproducts.dto.nfr.NfrDetailsV2DTO;
import ru.beeline.fdmproducts.dto.nfr.NfrItemProductDTO;
import ru.beeline.fdmproducts.dto.nfr.NfrItemProductV2DTO;
import ru.beeline.fdmproducts.dto.nfr.NfrPatternDTO;
import ru.beeline.fdmproducts.dto.nfr.RequirementProductDTO;
import ru.beeline.fdmproducts.exception.EntityNotFoundException;
import ru.beeline.fdmproducts.repository.ChapterNfrRepository;
import ru.beeline.fdmproducts.repository.LocalFitnessFunctionRepository;
import ru.beeline.fdmproducts.repository.NonFunctionalRequirementEnumRepository;
import ru.beeline.fdmproducts.repository.NonFunctionalRequirementRepository;
import ru.beeline.fdmproducts.repository.PatternRequirementRepository;
import ru.beeline.fdmproducts.repository.ProductBranchRepository;
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
    private FfManagerClient ffManagerClient;
    @Autowired
    private UserClient userClient;
    @Autowired
    private PatternRequirementRepository patternRequirementRepository;
    @Autowired
    private ProductBranchRepository productBranchRepository;
    @Autowired
    private ProductBranchService productBranchService;

    public NonFunctionalRequirement addRequirement(Integer productBranchId, Integer nfrId, String source) {
        ProductBranch productBranch = productBranchRepository.findById(productBranchId)
                .orElseThrow(() -> new EntityNotFoundException("Ветка продукта не найдена"));
        NonFunctionalRequirementEnum nfr = nonFunctionalRequirementEnumRepository.findById(nfrId)
                .orElseThrow(() -> new EntityNotFoundException("NFR enum не найден"));

        NonFunctionalRequirement requirement = NonFunctionalRequirement.builder()
                .productBranch(productBranch)
                .nfr(nfr)
                .source(source)
                .createdDate(LocalDateTime.now())
                .build();

        return nonFunctionalRequirementRepository.save(requirement);
    }

    public void linkRequirementsToBranch(Integer productBranchId, List<Integer> nfrIdsDistinct, String source, boolean userIdProvided) {
        if (nfrIdsDistinct == null || nfrIdsDistinct.isEmpty()) {
            return;
        }

        ProductBranch productBranch = productBranchRepository.findById(productBranchId)
                .orElseThrow(() -> new EntityNotFoundException("Ветка продукта не найдена"));

        List<NonFunctionalRequirementEnum> enums = nonFunctionalRequirementEnumRepository.findAllById(nfrIdsDistinct);
        if (enums.size() != nfrIdsDistinct.size()) {
            throw new IllegalArgumentException("Передан несуществующий идентификатор требования");
        }
        Map<Integer, NonFunctionalRequirementEnum> enumById = enums.stream()
                .collect(Collectors.toMap(NonFunctionalRequirementEnum::getId, e -> e));

        List<NonFunctionalRequirement> existing = nonFunctionalRequirementRepository
                .findByProductBranchIdAndNfrIds(productBranchId, nfrIdsDistinct);
        Map<Integer, NonFunctionalRequirement> existingByNfrId = existing.stream()
                .filter(r -> r.getNfr() != null && r.getNfr().getId() != null)
                .collect(Collectors.toMap(r -> r.getNfr().getId(), r -> r, (a, b) -> a));

        LocalDateTime now = LocalDateTime.now();

        for (Integer nfrId : nfrIdsDistinct) {
            NonFunctionalRequirement current = existingByNfrId.get(nfrId);
            if (current == null) {
                NonFunctionalRequirement toCreate = NonFunctionalRequirement.builder()
                        .productBranch(productBranch)
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

    public void addProductNfr(Integer id, String alias, String apiKey, String branch, String userIdHeader, List<Integer> nfrIds) {
        Product product = resolveProduct(id, alias, apiKey);
        Integer productBranchId = productBranchService.getExistingOrMainId(product.getAlias(), branch);
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
            linkRequirementsToBranch(productBranchId, nfrIdsDistinct, source, userIdProvided);
        } catch (IllegalArgumentException ex) {
            if ("Передан несуществующий идентификатор требования".equals(ex.getMessage())) {
                throw new IllegalArgumentException("Передан несуществующий идентификатор требования");
            }
            throw new IllegalArgumentException(ex.getMessage());
        }
    }

    public List<NonFunctionalRequirement> findByProductBranchId(Integer productBranchId) {
        return nonFunctionalRequirementRepository.findByProductBranch_Id(productBranchId);
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

    public Product resolveProduct(Integer id, String alias, String apiKey) {
        long providedCount = (id != null ? 1 : 0)
                + (alias != null && !alias.isBlank() ? 1 : 0)
                + (apiKey != null && !apiKey.isBlank() ? 1 : 0);
        if (providedCount == 0) {
            throw new IllegalArgumentException("Не передан один из идентификаторов приложения: id/alias/api-key");
        }
        if (providedCount > 1) {
            throw new IllegalArgumentException("Передано несколько идентификаторов приложения");
        }
        return findProductByIdOrAliasOrApiKey(id, alias, apiKey)
                .orElseThrow(() -> new EntityNotFoundException("Продукт с указанным идентификатором не найден"));
    }

    public Optional<Integer> findProductBranchId(Integer id, String alias, String apiKey, String branch) {
        Product product = resolveProduct(id, alias, apiKey);
        return productBranchService.findId(product.getAlias(), branch);
    }

    public void deleteProductNfr(Integer productBranchId, Integer reqId) {
        if (productBranchId == null || reqId == null) {
            return;
        }
        var relOpt = nonFunctionalRequirementRepository.findByProductBranch_IdAndNfr_Id(productBranchId, reqId);
        if (relOpt.isEmpty()) {
            return;
        }
        NonFunctionalRequirement rel = relOpt.get();
        if ("Beeatlas".equals(rel.getSource())) {
            throw new IllegalArgumentException("Требование назначенное автоматически, нельзя удалить вручную");
        }
        nonFunctionalRequirementRepository.delete(rel);
    }

    public void deleteProductNfr(Integer reqId, Integer id, String alias, String apiKey, String branch) {
        findProductBranchId(id, alias, apiKey, branch)
                .ifPresent(productBranchId -> deleteProductNfr(productBranchId, reqId));
    }

    public void deleteBeeatlasProductNfrRelations(Integer id, String alias, String apiKey, String branch, List<Integer> relationIds) {
        Integer productBranchId = findProductBranchId(id, alias, apiKey, branch).orElse(null);
        if (relationIds == null || relationIds.isEmpty()) {
            throw new IllegalArgumentException("Не передан ни один идентификатор связи");
        }
        List<Integer> idsDistinct = relationIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (idsDistinct.isEmpty()) {
            throw new IllegalArgumentException("Не передан ни один идентификатор связи");
        }

        List<NonFunctionalRequirement> rels = nonFunctionalRequirementRepository.findAllByIdInWithProductBranch(idsDistinct);
        if (rels.size() != idsDistinct.size()) {
            Set<Integer> found = rels.stream()
                    .map(NonFunctionalRequirement::getId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            List<Integer> missing = idsDistinct.stream().filter(i -> !found.contains(i)).toList();
            throw new IllegalArgumentException("Не найдены связи: " + missing);
        }

        for (NonFunctionalRequirement rel : rels) {
            Integer relProductBranchId = (rel.getProductBranch() != null ? rel.getProductBranch().getId() : null);
            if (productBranchId == null || !productBranchId.equals(relProductBranchId)) {
                throw new IllegalArgumentException("Связь " + rel.getId() + " не принадлежит указанному продукту");
            }
            if (!"Beeatlas".equals(rel.getSource())) {
                throw new IllegalArgumentException("Связь " + rel.getId() + " имеет source отличный от 'Beeatlas'");
            }
        }

        nonFunctionalRequirementRepository.deleteAll(rels);
    }

    public List<NfrItemProductDTO> getProductNfr(Integer id, String alias, String apiKey, String branch) {
        return findProductBranchId(id, alias, apiKey, branch)
                .map(this::getProductNfr)
                .orElse(List.of());
    }

    public List<NfrItemProductV2DTO> getProductNfrV2(Integer id, String alias, String apiKey, String branch) {
        return findProductBranchId(id, alias, apiKey, branch)
                .map(this::getProductNfrV2)
                .orElse(List.of());
    }

    private List<NfrItemProductDTO> getProductNfr(Integer productBranchId) {
        List<NonFunctionalRequirement> requirements = nonFunctionalRequirementRepository
                .findByProductBranchIdWithNfrAndCore(productBranchId);
        if (requirements.isEmpty()) {
            return List.of();
        }
        Map<Integer, List<Integer>> patternMap = buildNfrIdToPatternIdsMap(requirements);
        Map<Integer, NonFunctionalRequirement> latestByCore = toLatestByCore(requirements);
        Map<Integer, NfrPatternDTO> patternById = loadPatternsById(patternMap);

        return latestByCore.values().stream()
                .filter(req -> isActualNfrVersion(req.getNfr()))
                .map(req -> toNfrItemProductDTO(req, resolvePatternsForNfr(req.getNfrId(), patternMap, patternById)))
                .toList();
    }

    private List<NfrItemProductV2DTO> getProductNfrV2(Integer productBranchId) {
        List<NonFunctionalRequirement> requirements = nonFunctionalRequirementRepository
                .findByProductBranchIdWithNfrAndCore(productBranchId);
        if (requirements.isEmpty()) {
            return List.of();
        }
        Map<Integer, List<Integer>> patternMap = buildNfrIdToPatternIdsMap(requirements);
        Map<Integer, NonFunctionalRequirement> latestByCore = toLatestByCore(requirements);
        Map<Integer, NfrPatternDTO> patternById = loadPatternsById(patternMap);
        Map<String, FfManagerFitnessFunctionDTO> catalogByCodeLower = loadFfManagerCatalogByCodeLower();

        return latestByCore.values().stream()
                .filter(req -> isActualNfrVersion(req.getNfr()))
                .map(req -> {
                    List<NfrPatternDTO> patterns = resolvePatternsForNfr(req.getNfrId(), patternMap, patternById);
                    String rule = req.getNfr() != null ? req.getNfr().getRule() : null;
                    List<FitnessFunctionNfrV2DTO> fitnessFunctions =
                            resolveFitnessFunctionsFromFfManager(rule, catalogByCodeLower);
                    return toNfrItemProductV2DTO(req, patterns, fitnessFunctions);
                })
                .toList();
    }

    private Map<Integer, List<Integer>> buildNfrIdToPatternIdsMap(List<NonFunctionalRequirement> requirements) {
        List<PatternRequirement> patternRequirements = patternRequirementRepository.findByNfrIdIn(requirements.stream()
                .map(NonFunctionalRequirement::getNfrId).collect(Collectors.toList()));
        return patternRequirements.stream()
                .collect(Collectors.groupingBy(
                        PatternRequirement::getNfrId,
                        Collectors.mapping(PatternRequirement::getPatternId, Collectors.toList())
                ));
    }

    private Map<Integer, NonFunctionalRequirement> toLatestByCore(List<NonFunctionalRequirement> requirements) {
        return requirements.stream()
                .filter(req -> req.getNfr() != null && req.getNfr().getCore() != null)
                .collect(Collectors.toMap(
                        req -> req.getNfr().getCore().getId(),
                        Function.identity(),
                        this::compareByVersion
                ));
    }

    private Map<Integer, NfrPatternDTO> loadPatternsById(Map<Integer, List<Integer>> patternMap) {
        List<Integer> allPatternIds = patternMap.values().stream()
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (allPatternIds.isEmpty()) {
            return Map.of();
        }
        return techradarClient.getPatternsByIds(allPatternIds).stream()
                .filter(Objects::nonNull)
                .filter(p -> p.getId() != null)
                .collect(Collectors.toMap(NfrPatternDTO::getId, Function.identity(), (a, b) -> a));
    }

    private List<NfrPatternDTO> resolvePatternsForNfr(Integer nfrId, Map<Integer, List<Integer>> patternMap,
                                                      Map<Integer, NfrPatternDTO> patternById) {
        List<Integer> ids = patternMap.get(nfrId);
        if (ids == null) {
            return List.of();
        }
        return ids.stream()
                .filter(Objects::nonNull)
                .distinct()
                .map(patternById::get)
                .filter(Objects::nonNull)
                .toList();
    }

    private Map<String, FfManagerFitnessFunctionDTO> loadFfManagerCatalogByCodeLower() {
        Map<String, FfManagerFitnessFunctionDTO> result = ffManagerClient.getAllFitnessFunctions().stream()
                .filter(ff -> ff.getCode() != null && !ff.getCode().isBlank())
                .collect(Collectors.toMap(
                        ff -> ff.getCode().toLowerCase(Locale.ROOT),
                        Function.identity(),
                        (a, b) -> a));
        log.info("Список по запросу из ff-manager, размер: {}", result.size());
        return result;
    }

    private List<FitnessFunctionNfrV2DTO> resolveFitnessFunctionsFromFfManager(
            String rule,
            Map<String, FfManagerFitnessFunctionDTO> catalogByCodeLower) {
        log.info("rule: {}", rule);
        List<String> lowerCodes = parseRuleCodesLower(rule);
        if (lowerCodes.isEmpty()) {
            return List.of();
        }
        List<FitnessFunctionNfrV2DTO> result = new ArrayList<>();
        for (String lowerCode : lowerCodes) {
            log.info("rule codes: {}", lowerCode);
            FfManagerFitnessFunctionDTO ff = catalogByCodeLower.get(lowerCode);
            if (ff != null) {
                result.add(FitnessFunctionNfrV2DTO.builder()
                        .id(ff.getId())
                        .code(ff.getCode())
                        .description(ff.getDescription())
                        .build());
            }
        }
        return result;
    }

    private NonFunctionalRequirement compareByVersion(NonFunctionalRequirement req1, NonFunctionalRequirement req2) {
        Integer v1 = req1.getNfr() != null ? req1.getNfr().getVersion() : null;
        Integer v2 = req2.getNfr() != null ? req2.getNfr().getVersion() : null;
        if (v1 == null) return req2;
        if (v2 == null) return req1;
        return v1 > v2 ? req1 : req2;
    }

    private boolean isActualNfrVersion(NonFunctionalRequirementEnum nfr) {
        if (nfr == null || nfr.getCore() == null) {
            return false;
        }
        if (nfr.getVersion() == null) {
            log.warn("NFR для Core {} имеет версию null, исключаем из результата",
                    nfr.getCore().getId());
            return false;
        }
        boolean hasNewerVersion = nonFunctionalRequirementEnumRepository.existsByCoreIdAndVersionGreaterThan(
                nfr.getCore().getId(),
                nfr.getVersion());
        if (hasNewerVersion) {
            log.info("NFR для Core {} устарел: текущая версия={}, есть более новая версия в БД",
                    nfr.getCore().getId(), nfr.getVersion());
            return false;
        }
        return true;
    }

    private NfrItemProductDTO toNfrItemProductDTO(NonFunctionalRequirement requirement, List<NfrPatternDTO> patterns) {
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

    private NfrItemProductV2DTO toNfrItemProductV2DTO(NonFunctionalRequirement requirement,
                                                      List<NfrPatternDTO> patterns,
                                                      List<FitnessFunctionNfrV2DTO> fitnessFunctions) {
        NonFunctionalRequirementEnum nfr = requirement.getNfr();
        NonFunctionalRequirementEnumCore core = nfr.getCore();
        List<ChapterNfr> chapterNfrs = chapterNfrRepository.findByNfrId(nfr.getId());
        List<Chapter> chapters = chapterNfrs.stream().map(ChapterNfr::getChapter).filter(Objects::nonNull).toList();
        List<ChapterNfrDTO> chapterNfrDTOS = buildChapterNfrDTO(chapters);
        return NfrItemProductV2DTO.builder()
                .id(nfr.getId())
                .code(core != null ? core.getCode() : null)
                .version(nfr.getVersion())
                .name(nfr.getName())
                .createdDate(requirement.getCreatedDate())
                .description(nfr.getDescription())
                .patterns(patterns != null ? patterns : new ArrayList<>())
                .fitnessFunctions(fitnessFunctions)
                .chapters(chapterNfrDTOS)
                .source(core != null ? core.getSource() : null)
                .sourcePurpose(requirement.getSource())
                .build();
    }

    private List<String> parseRuleCodesLower(String rule) {
        if (rule == null || rule.trim().isEmpty()) {
            log.warn("Rule is null or empty");
            return List.of();
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
        }
        return lowerCodes;
    }

    private List<LocalFitnessFunction> getFitnessFunctionsFromRule(String rule) {
        List<String> lowerCodes = parseRuleCodesLower(rule);
        if (lowerCodes.isEmpty()) {
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

    @Transactional
    public void actualizeRequirementOnProduct(String nfrIdStr, Integer productQueryId, String alias, String apiKey, String branch) {
        Integer nfrId;
        try {
            nfrId = Integer.parseInt(nfrIdStr);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Некорректный идентификатор id");
        }
        Optional<Integer> productBranchId = findProductBranchId(productQueryId, alias, apiKey, branch);

        NonFunctionalRequirement relation = productBranchId
                .flatMap(branchId -> nonFunctionalRequirementRepository.findByProductBranch_IdAndNfr_Id(branchId, nfrId))
                .orElseThrow(() -> new EntityNotFoundException("Связь требования с продуктом не найдена"));

        if ("Beeatlas".equals(relation.getSource())) {
            throw new IllegalArgumentException("Актуализировать можно только версию требования назначенного в ручную");
        }

        Integer coreId = relation.getNfr().getCore().getId();

        NonFunctionalRequirementEnum maxVersion = nonFunctionalRequirementEnumRepository.findByCoreId(coreId).stream()
                .filter(v -> v.getVersion() != null)
                .max(Comparator.comparing(NonFunctionalRequirementEnum::getVersion))
                .orElse(relation.getNfr());

        if (maxVersion.getId().equals(nfrId)) {
            return;
        }

        if (nonFunctionalRequirementRepository.findByProductBranch_IdAndNfr_Id(productBranchId.get(), maxVersion.getId()).isPresent()) {
            return;
        }

        nonFunctionalRequirementRepository.save(NonFunctionalRequirement.builder()
                .productBranch(relation.getProductBranch())
                .nfr(maxVersion)
                .source(relation.getSource())
                .createdDate(LocalDateTime.now())
                .build());
    }

    @Transactional(readOnly = true)
    public List<RequirementProductDTO> getProductsByRequirementId(String nfrIdStr, String filterRaw, String branch) {
        Integer nfrId;
        try {
            nfrId = Integer.parseInt(nfrIdStr);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Некорректный идентификатор id");
        }
        final String filter = (filterRaw == null || filterRaw.isBlank())
                ? "all"
                : filterRaw.trim().toLowerCase(Locale.ROOT);
        if (!Set.of("all", "auto", "hand").contains(filter)) {
            throw new IllegalArgumentException(
                    "Недопустимое значение filter. Допустимые значения: all, auto, hand");
        }
        String branchName = productBranchService.resolveBranchName(branch);
        if (!nonFunctionalRequirementEnumRepository.existsById(nfrId)) {
            throw new EntityNotFoundException("Требование не найдено");
        }
        return nonFunctionalRequirementRepository.findByNfrIdWithProductBranch(nfrId).stream()
                .filter(r -> r.getProductBranch() != null)
                .filter(r -> branchName.equalsIgnoreCase(r.getProductBranch().getBranchName()))
                .filter(r -> {
                    if ("auto".equals(filter)) return "Beeatlas".equals(r.getSource());
                    if ("hand".equals(filter)) return r.getSource() == null || !"Beeatlas".equals(r.getSource());
                    return true;
                })
                .map(r -> RequirementProductDTO.builder()
                        .alias(r.getProductBranch().getAlias())
                        .branch(r.getProductBranch().getBranchName())
                        .source(r.getSource())
                        .build())
                .sorted(Comparator.comparing(
                        dto -> dto.getAlias() != null ? dto.getAlias().toLowerCase(Locale.ROOT) : ""))
                .collect(Collectors.toList());
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

    public NfrDetailsV2DTO getNfrDetailsV2(Integer id) {
        NonFunctionalRequirementEnum nfr = nonFunctionalRequirementEnumRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Требование не найдено"));
        NonFunctionalRequirementEnumCore core = nfr.getCore();
        Map<String, FfManagerFitnessFunctionDTO> catalogByCodeLower = loadFfManagerCatalogByCodeLower();
        List<FitnessFunctionNfrV2DTO> fitnessFunctions =
                resolveFitnessFunctionsFromFfManager(nfr.getRule(), catalogByCodeLower);
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
        return NfrDetailsV2DTO.builder()
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
