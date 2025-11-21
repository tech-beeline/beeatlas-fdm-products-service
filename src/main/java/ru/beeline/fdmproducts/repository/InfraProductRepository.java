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
    @Query(value = "SELECT product_id FROM product.infra_product WHERE infra_id = :infraId", nativeQuery = true)
    List<Integer> findProductIdsByInfraId(@Param("infraId") Integer infraId);

    @Modifying
    @Query(value = "UPDATE product.infra_product ip SET deleted_date = :now " +
            "WHERE ip.product_id = :productId AND ip.infra_id IN " +
            "(SELECT i.id FROM product.infra i WHERE i.cmdb_id NOT IN (:cmdbIds)) " +
            "AND ip.deleted_date IS NULL", nativeQuery = true)
    int markInfraProductsDeleted(@Param("productId") Integer productId, @Param("cmdbIds") List<String> cmdbIds, @Param("now") LocalDateTime now);

    @Modifying
    @Query(value = "UPDATE product.infra_product ip SET deleted_date = NULL, last_modified_date = :now " +
            "WHERE ip.product_id = :productId AND ip.infra_id IN " +
            "(SELECT i.id FROM product.infra i WHERE i.cmdb_id IN (:cmdbIds)) " +
            "AND ip.deleted_date IS NOT NULL", nativeQuery = true)
    int restoreInfraProducts(@Param("productId") Integer productId,
                             @Param("cmdbIds") List<String> cmdbIds,
                             @Param("now") LocalDateTime now);
}
