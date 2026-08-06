/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.beeline.fdmproducts.client.TechradarClient;
import ru.beeline.fdmproducts.domain.Product;
import ru.beeline.fdmproducts.domain.TechProduct;
import ru.beeline.fdmproducts.dto.GetProductDTO;
import ru.beeline.fdmproducts.dto.techradar.TechAdvancedGetDTO;
import ru.beeline.fdmproducts.repository.TechProductRepository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Transactional
@Service
@Slf4j
public class TechService {
    private final TechProductRepository techProductRepository;
    private final TechradarClient techradarClient;

    public TechService(TechProductRepository techProductRepository, TechradarClient techradarClient) {
        this.techProductRepository = techProductRepository;
        this.techradarClient = techradarClient;
    }

    public List<GetProductDTO> getProductsByTechId(Integer techId) {
        return techProductRepository.findAllByTechId(techId).stream().map(techProduct -> GetProductDTO.builder()
                .id(techProduct.getProduct().getId())
                .name(techProduct.getProduct().getName())
                .alias(techProduct.getProduct().getAlias())
                .build()).collect(Collectors.toList());
    }

    public void saveOrNone(Integer techId, Product product) {
        if (techProductRepository.findByTechIdAndProduct(techId, product) == null) {
            techProductRepository.save(TechProduct.builder().product(product).techId(techId).build());
        }
    }

    public void deleteRelation(Integer techId, Integer productId) {
        techProductRepository.deleteByTechIdAndProductId(techId, productId);
    }

    /**
     * Заменяет набор связей продукта с технологиями на переданный список techId одним запросом:
     * отсутствующие связи создаются, лишние (не вошедшие в список) удаляются.
     */
    public void replaceRelations(Product product, List<Integer> techIds) {
        Set<Integer> targetTechIds = filterExistingTechIds(techIds);
        List<TechProduct> existing = techProductRepository.findAllByProductId(product.getId());
        Set<Integer> existingTechIds = existing.stream().map(TechProduct::getTechId).collect(Collectors.toSet());

        List<TechProduct> toDelete = existing.stream()
                .filter(techProduct -> !targetTechIds.contains(techProduct.getTechId()))
                .collect(Collectors.toList());
        List<TechProduct> toCreate = targetTechIds.stream()
                .filter(techId -> !existingTechIds.contains(techId))
                .map(techId -> TechProduct.builder().product(product).techId(techId).build())
                .collect(Collectors.toList());

        if (!toDelete.isEmpty()) {
            techProductRepository.deleteAll(toDelete);
        }
        if (!toCreate.isEmpty()) {
            techProductRepository.saveAll(toCreate);
        }
        log.info("replaceRelations product={}: было={}, удалено={}, добавлено={}",
                product.getAlias(), existing.size(), toDelete.size(), toCreate.size());
    }

    private Set<Integer> filterExistingTechIds(List<Integer> techIds) {
        Set<Integer> requestedTechIds = new HashSet<>(techIds != null ? techIds : List.of());
        if (requestedTechIds.isEmpty()) {
            return requestedTechIds;
        }
        Set<Integer> existingInTechradar = techradarClient.getTechById(new ArrayList<>(requestedTechIds)).stream()
                .map(TechAdvancedGetDTO::getId)
                .collect(Collectors.toSet());
        requestedTechIds.retainAll(existingInTechradar);
        return requestedTechIds;
    }
}
