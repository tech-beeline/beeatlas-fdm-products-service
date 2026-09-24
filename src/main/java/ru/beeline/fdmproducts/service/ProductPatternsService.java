/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.beeline.fdmproducts.client.TechradarClient;
import ru.beeline.fdmproducts.domain.EnumSourceType;
import ru.beeline.fdmproducts.domain.Product;
import ru.beeline.fdmproducts.domain.ProductPatterns;
import ru.beeline.fdmproducts.dto.PatternDTO;
import ru.beeline.fdmproducts.dto.PostPatternProductDTO;
import ru.beeline.fdmproducts.dto.ProductPatternV2DTO;
import ru.beeline.fdmproducts.exception.EntityNotFoundException;
import ru.beeline.fdmproducts.exception.ValidationException;
import ru.beeline.fdmproducts.repository.EnumSourceTypeRepository;
import ru.beeline.fdmproducts.repository.ProductPatternsRepository;
import ru.beeline.fdmproducts.repository.ProductRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProductPatternsService {

    private final ProductRepository productRepository;
    private final EnumSourceTypeRepository enumSourceTypeRepository;
    private final ProductPatternsRepository productPatternsRepository;
    private final TechradarClient techradarClient;

    @Transactional
    public void postPatternProductV2(String alias, String sourceType, String branch, Integer sourceId,
                                     List<PostPatternProductDTO> items) {
        List<PostPatternProductDTO> body = items == null ? List.of() : items;
        log.info("POST patterns v2: alias={}, sourceType={}, branch={}, sourceId={}, элементов={}",
                alias, sourceType, branch, sourceId, body.size());
        for (PostPatternProductDTO item : body) {
            validateItem(item);
        }
        Product product = productRepository.findByAliasCaseInsensitive(alias);
        if (product == null) {
            throw new EntityNotFoundException("Указанный продукт не существует");
        }
        EnumSourceType enumSourceType = enumSourceTypeRepository.findByName(sourceType)
                .orElseThrow(() -> new IllegalArgumentException("невозможный источник"));
        if (Boolean.TRUE.equals(enumSourceType.getIdentifySource()) && sourceId == null) {
            throw new IllegalArgumentException("Для указанного источника обязательна передача идентификатора");
        }
        String normalizedBranch = normalizeBranch(branch);
        String productAlias = product.getAlias();
        LocalDateTime now = LocalDateTime.now();
        for (PostPatternProductDTO item : body) {
            String patternCode = item.getCode().trim();
            Optional<ProductPatterns> actual = productPatternsRepository.findActual(
                    productAlias, normalizedBranch, enumSourceType.getId(), sourceId, patternCode);
            if (actual.isPresent()) {
                ProductPatterns previous = actual.get();
                previous.setIsActual(false);
                productPatternsRepository.save(previous);
                log.info("Деактуализирована запись product_patterns: id={}, alias={}, branch={}, code={}",
                        previous.getId(), productAlias, normalizedBranch, patternCode);
            }
            ProductPatterns created = productPatternsRepository.save(ProductPatterns.builder()
                    .patternCode(patternCode)
                    .isCheck(item.getIsCheck())
                    .resultDetails(item.getResultDetails())
                    .productAlias(productAlias)
                    .productBranch(normalizedBranch)
                    .sourceTypeId(enumSourceType.getId())
                    .sourceId(sourceId)
                    .createdDate(now)
                    .isActual(true)
                    .build());
            log.info("Создана актуальная запись product_patterns: id={}, alias={}, branch={}, code={}",
                    created.getId(), productAlias, normalizedBranch, patternCode);
        }
        log.info("POST patterns v2 завершён: alias={}, branch={}, sourceType={}, записано={}",
                productAlias, normalizedBranch, sourceType, body.size());
    }

    @Transactional(readOnly = true)
    public List<ProductPatternV2DTO> getProductPatternsV2(String alias, String branch) {
        log.info("GET patterns v2: alias={}, branch={}", alias, branch);
        Product product = productRepository.findByAliasCaseInsensitive(alias);
        if (product == null) {
            throw new EntityNotFoundException(String.format("Продукт c alias '%s' не найден", alias));
        }
        String normalizedBranch = normalizeBranch(branch);
        List<ProductPatterns> actual = productPatternsRepository
                .findAllByProductAliasAndProductBranchAndIsActualTrue(product.getAlias(), normalizedBranch);
        if (actual.isEmpty()) {
            log.info("GET patterns v2: актуальных записей нет, alias={}, branch={}",
                    product.getAlias(), normalizedBranch);
            return List.of();
        }
        Map<String, PatternDTO> byCode = patternsAutoCheckByCode();
        List<ProductPatternV2DTO> result = new ArrayList<>(actual.size());
        for (ProductPatterns row : actual) {
            result.add(toV2Dto(row, byCode.get(row.getPatternCode())));
        }
        log.info("GET patterns v2: alias={}, branch={}, из БД={}, обогащено из Techradar={}",
                product.getAlias(), normalizedBranch, result.size(), byCode.size());
        return result;
    }

    private Map<String, PatternDTO> patternsAutoCheckByCode() {
        List<PatternDTO> fromTechradar = techradarClient.getPatternsAutoCheck();
        if (fromTechradar == null || fromTechradar.isEmpty()) {
            log.info("GET patterns v2: Techradar auto-check пуст или недоступен");
            return Collections.emptyMap();
        }
        return fromTechradar.stream()
                .filter(dto -> dto.getCode() != null)
                .collect(Collectors.toMap(PatternDTO::getCode, Function.identity(), (first, second) -> first));
    }

    private ProductPatternV2DTO toV2Dto(ProductPatterns row, PatternDTO card) {
        ProductPatternV2DTO.ProductPatternV2DTOBuilder builder = ProductPatternV2DTO.builder()
                .code(row.getPatternCode())
                .isCheck(row.getIsCheck())
                .resultDetails(row.getResultDetails());
        if (card != null) {
            builder.id(card.getId())
                    .name(card.getName())
                    .rule(card.getRule())
                    .isAntiPattern(card.getIsAntiPattern())
                    .createDate(card.getCreateDate())
                    .updateDate(card.getUpdateDate())
                    .deleteDate(card.getDeleteDate())
                    .technologies(card.getTechnologies());
        }
        return builder.build();
    }

    private String normalizeBranch(String branch) {
        if (branch == null || branch.isBlank()) {
            return ProductBranchService.DEFAULT_BRANCH;
        }
        return branch.trim().toLowerCase(Locale.ROOT);
    }

    private void validateItem(PostPatternProductDTO dto) {
        StringBuilder errMsg = new StringBuilder();
        if (dto == null || dto.getCode() == null || dto.getCode().trim().isEmpty()) {
            errMsg.append("Отсутствует обязательное поле code; ");
        }
        if (dto == null || dto.getIsCheck() == null) {
            errMsg.append("Отсутствует обязательное поле isCheck; ");
        }
        if (!errMsg.toString().isEmpty()) {
            throw new ValidationException("409 Ошибка валидации тела запроса: " + errMsg.toString().trim());
        }
    }
}
