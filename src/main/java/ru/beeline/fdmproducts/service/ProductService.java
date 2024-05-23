package ru.beeline.fdmproducts.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.beeline.fdmproducts.domain.UserProduct;
import ru.beeline.fdmproducts.repository.UserProductRepository;
import ru.beeline.fdmproducts.domain.Product;

import java.util.List;
import java.util.stream.Collectors;

@Transactional
@Service
@Slf4j
public class ProductService {
    private final UserProductRepository userProductRepository;

    public ProductService(UserProductRepository userProductRepository) {
        this.userProductRepository = userProductRepository;
    }

    public List<Product> getProductsByUser(Integer userId) {
        return userProductRepository.findAllByUserId(userId).stream().map(UserProduct::getProduct).collect(Collectors.toList());
    }
}
