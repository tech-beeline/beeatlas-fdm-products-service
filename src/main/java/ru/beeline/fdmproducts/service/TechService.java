package ru.beeline.fdmproducts.service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.beeline.fdmproducts.domain.TechProduct;
import ru.beeline.fdmproducts.repository.TechProductRepository;
import ru.beeline.fdmproducts.domain.Product;

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

    public List<Product> getProductsByTechId(Integer techId) {
        return techProductRepository.findAllByTechId(techId).stream().map(TechProduct::getProduct).collect(Collectors.toList());
    }
}
