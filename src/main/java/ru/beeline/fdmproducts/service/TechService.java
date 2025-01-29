package ru.beeline.fdmproducts.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.beeline.fdmproducts.domain.Product;
import ru.beeline.fdmproducts.domain.TechProduct;
import ru.beeline.fdmproducts.dto.GetProductDTO;
import ru.beeline.fdmproducts.repository.TechProductRepository;

import java.util.List;
import java.util.stream.Collectors;

@Transactional
@Service
@Slf4j
public class TechService {
    private final TechProductRepository techProductRepository;

    public TechService(TechProductRepository techProductRepository) {
        this.techProductRepository = techProductRepository;
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
}
