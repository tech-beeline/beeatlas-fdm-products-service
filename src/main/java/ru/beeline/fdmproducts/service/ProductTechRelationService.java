/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.beeline.fdmproducts.dto.ProductDTO;
import ru.beeline.fdmproducts.dto.ProductTechRelationDTO;
import ru.beeline.fdmproducts.mapper.ProductTechMapper;

import java.util.List;

@Transactional
@Service
@Slf4j
public class ProductTechRelationService {
    private final ProductService productService;
    private final TechService techService;
    

    public ProductTechRelationService(ProductService productService, TechService techService) {
        this.productService = productService;
        this.techService = techService;
    }

    public void addRelation(Integer techId, ProductTechRelationDTO productTechRelationDTO) {
        techService.saveOrNone(techId, productService.getProductByCode(productTechRelationDTO.getCmdbCode()));
    }

    public List<ProductDTO> getProductsWithTech() {
        return ProductTechMapper.mapToDto(productService.findAllWithTechProductNotDeleted());
    }

    public void deleteRelation(Integer techId, Integer productId) {
        techService.deleteRelation(techId, productId);
    }
}
