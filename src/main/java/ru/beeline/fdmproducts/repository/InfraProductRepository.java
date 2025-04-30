package ru.beeline.fdmproducts.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.beeline.fdmproducts.domain.InfraProduct;

import java.util.List;

@Repository
public interface InfraProductRepository extends JpaRepository<InfraProduct, Integer> {
    List<InfraProduct> findByProductId(Integer productId);

}
