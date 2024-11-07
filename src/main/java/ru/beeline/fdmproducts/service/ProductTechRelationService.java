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
    private final ProductTechMapper productTechMapper;

    public ProductTechRelationService(ProductService productService, TechService techService, ProductTechMapper productTechMapper) {
        this.productService = productService;
        this.techService = techService;
        this.productTechMapper = productTechMapper;
    }

    public void addRelation(Integer techId, ProductTechRelationDTO productTechRelationDTO) {
        techService.saveOrNone(techId, productService.getProductByCode(productTechRelationDTO.getCmdbCode()));
    }

    public List<ProductDTO> getProductsWithTech() {
        return productTechMapper.mapToDto(productService.findAll());
    }
}
