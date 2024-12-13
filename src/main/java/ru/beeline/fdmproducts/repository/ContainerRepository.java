package ru.beeline.fdmproducts.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.beeline.fdmproducts.domain.ContainerProduct;
import ru.beeline.fdmproducts.domain.Product;

import java.util.Optional;

public interface ContainerRepository extends JpaRepository<ContainerProduct, Integer> {
    Optional<ContainerProduct> findByCode(String code);
}
