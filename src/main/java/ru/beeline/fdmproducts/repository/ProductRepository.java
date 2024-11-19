package ru.beeline.fdmproducts.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.beeline.fdmproducts.domain.Product;

public interface ProductRepository extends JpaRepository<Product, Long> {
    Product findByAlias(String code);
    Product findByStructurizrApiKey(String apiKey);
}
