/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.beeline.fdmproducts.domain.EnumSourceType;
import ru.beeline.fdmproducts.domain.Product;
import ru.beeline.fdmproducts.domain.ProductPatterns;
import ru.beeline.fdmproducts.dto.PostPatternProductDTO;
import ru.beeline.fdmproducts.exception.EntityNotFoundException;
import ru.beeline.fdmproducts.exception.ValidationException;
import ru.beeline.fdmproducts.repository.EnumSourceTypeRepository;
import ru.beeline.fdmproducts.repository.ProductPatternsRepository;
import ru.beeline.fdmproducts.repository.ProductRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProductPatternsService {

    private final ProductRepository productRepository;
    private final EnumSourceTypeRepository enumSourceTypeRepository;
    private final ProductPatternsRepository productPatternsRepository;

    @Transactional
    public void postPatternProductV2(String alias, String sourceType, String branch, Integer sourceId,
                                     List<PostPatternProductDTO> items) {
        List<PostPatternProductDTO> body = items == null ? List.of() : items;
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
