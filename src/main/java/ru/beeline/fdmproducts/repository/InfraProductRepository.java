package ru.beeline.fdmproducts.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.beeline.fdmproducts.domain.InfraProduct;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface InfraProductRepository extends JpaRepository<InfraProduct, Integer> {
    List<InfraProduct> findByProductId(Integer productId);

    @Modifying
    @Query("UPDATE InfraProduct ip SET ip.deletedDate = :now WHERE ip.product.id = :productId AND ip.infra.cmdbId NOT IN :cmdbIds AND ip.deletedDate IS NULL")
    int markInfraProductsDeleted(@Param("productId") Integer productId, @Param("cmdbIds") List<String> cmdbIds, @Param("now") LocalDateTime now);

}
