/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import ru.beeline.fdmproducts.domain.ProductInteraction;
import ru.beeline.fdmproducts.dto.interaction.ProductInteractionItemDTO;
import ru.beeline.fdmproducts.dto.interaction.ProductInteractionLinksDTO;
import ru.beeline.fdmproducts.dto.interaction.ProductInteractionResponseDTO;
import ru.beeline.fdmproducts.repository.ProductInteractionRepository;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ProductInteractionService {

    private final ProductInteractionRepository productInteractionRepository;

    @Transactional(readOnly = true)
    public ProductInteractionLinksDTO getInteractionsByAlias(String alias) {
        if (!StringUtils.hasText(alias)) {
            throw new IllegalArgumentException("Отсутствует обязательный path-параметр alias");
        }
        String trimmedAlias = alias.trim();
        return ProductInteractionLinksDTO.builder()
                .providers(productInteractionRepository
                        .findProviderAliasesByConsumerAliasIgnoreCase(trimmedAlias))
                .consumers(productInteractionRepository
                        .findConsumerAliasesByProviderAliasIgnoreCase(trimmedAlias))
                .build();
    }

    @Transactional
    public ProductInteractionResponseDTO saveInteractions(List<ProductInteractionItemDTO> items) {
        if (items == null) {
            throw new IllegalArgumentException("Тело запроса обязательно и должно быть массивом");
        }

        int created = 0;
        int ignored = 0;
        Set<String> seenKeys = new HashSet<>();

        for (int i = 0; i < items.size(); i++) {
            ProductInteractionItemDTO item = items.get(i);
            if (item == null) {
                throw new IllegalArgumentException("Элемент [" + i + "] не должен быть null");
            }
            requireNonBlank(item.getProviderAlias(), "providerAlias", i);
            requireNonBlank(item.getConsumerAlias(), "consumerAlias", i);
            requireNonBlank(item.getSource(), "source", i);

            String key = pairKey(item.getProviderAlias(), item.getConsumerAlias());
            if (seenKeys.contains(key)
                    || productInteractionRepository.existsByProviderAndConsumerIgnoreCase(
                    item.getProviderAlias().trim(), item.getConsumerAlias().trim())) {
                seenKeys.add(key);
                ignored++;
                continue;
            }

            productInteractionRepository.save(ProductInteraction.builder()
                    .providerAlias(item.getProviderAlias().trim())
                    .consumerAlias(item.getConsumerAlias().trim())
                    .source(item.getSource().trim())
                    .createdAt(LocalDateTime.now())
                    .build());
            seenKeys.add(key);
            created++;
        }

        return ProductInteractionResponseDTO.builder()
                .created(created)
                .ignored(ignored)
                .build();
    }

    private void requireNonBlank(String value, String fieldName, int index) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(
                    "Отсутствует обязательное поле " + fieldName + " у элемента [" + index + "]");
        }
    }

    private String pairKey(String providerAlias, String consumerAlias) {
        return providerAlias.trim().toLowerCase(Locale.ROOT)
                + '\0'
                + consumerAlias.trim().toLowerCase(Locale.ROOT);
    }
}
