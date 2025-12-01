package ru.beeline.fdmproducts.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.beeline.fdmproducts.domain.ProductAvailability;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductAvailabilityRepository extends JpaRepository<ProductAvailability, Integer> {

    @Query("SELECT pa FROM ProductAvailability pa WHERE pa.productId = :productId ORDER BY pa.createdDate DESC")
    Optional<ProductAvailability> findLatestAvailabilityByProductId(@Param("productId") Integer productId);
}