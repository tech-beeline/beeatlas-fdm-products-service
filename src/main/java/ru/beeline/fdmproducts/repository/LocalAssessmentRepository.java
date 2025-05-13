package ru.beeline.fdmproducts.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.beeline.fdmproducts.domain.LocalAssessment;
import ru.beeline.fdmproducts.domain.Product;

import java.util.List;
import java.util.Optional;

public interface LocalAssessmentRepository extends JpaRepository<LocalAssessment, Integer> {
    @Query("SELECT la FROM LocalAssessment la WHERE la.product.id = :productId AND la.sourceId = :sourceId")
    Optional<LocalAssessment> findByProductIdAndSourceId(
            @Param("productId") Integer productId,
            @Param("sourceId") Integer sourceId);

    @Query("SELECT la FROM LocalAssessment la WHERE la.product.id = :productId ORDER BY la.createdTime DESC")
    List<LocalAssessment> findLatestByProductId(@Param("productId") Integer productId);

    Optional<LocalAssessment> findBySourceIdAndProduct(Integer sourceId, Product product);
}
