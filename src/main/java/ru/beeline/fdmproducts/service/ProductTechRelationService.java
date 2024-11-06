package ru.beeline.fdmproducts.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.beeline.fdmproducts.dto.ProductTechRelationDTO;

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
        productService.getProductByCode(productTechRelationDTO.getCmdbCode());
        techService.saveOrNone(techId, productService.getProductByCode(productTechRelationDTO.getCmdbCode()));
    }
}
