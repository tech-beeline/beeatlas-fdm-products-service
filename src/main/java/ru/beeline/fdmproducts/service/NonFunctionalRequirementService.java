/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.beeline.fdmproducts.domain.NonFunctionalRequirement;
import ru.beeline.fdmproducts.domain.NonFunctionalRequirementEnum;
import ru.beeline.fdmproducts.domain.NonFunctionalRequirementEnumCore;
import ru.beeline.fdmproducts.domain.Product;
import ru.beeline.fdmproducts.dto.NfrItemDTO;
import ru.beeline.fdmproducts.exception.EntityNotFoundException;
import ru.beeline.fdmproducts.repository.NonFunctionalRequirementEnumRepository;
import ru.beeline.fdmproducts.repository.NonFunctionalRequirementRepository;
import ru.beeline.fdmproducts.repository.ProductRepository;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
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

    public NonFunctionalRequirement addRequirement(Integer productId, Integer nfrId, String source) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Продукт не найден"));
        NonFunctionalRequirementEnum nfr = nonFunctionalRequirementEnumRepository.findById(nfrId)
                .orElseThrow(() -> new EntityNotFoundException("NFR enum не найден"));

        NonFunctionalRequirement requirement = NonFunctionalRequirement.builder()
                .product(product)
                .nfr(nfr)
                .source(source)
                .build();

        return nonFunctionalRequirementRepository.save(requirement);
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

    /**
     * Найти продукт по одному из идентификаторов: id, alias или api-key.
     */
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

    /**
     * Получить все актуальные версии требований (с максимальной версией) связанные с продуктом.
     * Дубли по core_id убираются, остаётся только запись с максимальным version.
     */
    public List<NfrItemDTO> getProductNfr(Integer productId) {
        List<NonFunctionalRequirement> requirements = nonFunctionalRequirementRepository
                .findByProductIdWithNfrAndCore(productId);

        if (requirements.isEmpty()) {
            return List.of();
        }

        return requirements.stream()
                .map(NonFunctionalRequirement::getNfr)
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
