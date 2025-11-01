package ru.beeline.fdmproducts.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.beeline.fdmproducts.domain.LocalAssessment;
import ru.beeline.fdmproducts.domain.Product;

import java.util.List;
import java.util.Optional;
@Repository
public interface LocalAssessmentRepository extends JpaRepository<LocalAssessment, Integer> {

    @Query("SELECT la FROM LocalAssessment la WHERE la.product.id = :productId ORDER BY la.createdTime DESC")
    List<LocalAssessment> findLatestByProductId(@Param("productId") Integer productId);

    Optional<LocalAssessment> findBySourceIdAndProduct(Integer sourceId, Product product);

    Optional<LocalAssessment> findBySourceIdAndProductIdAndSourceTypeId(Integer sourceId, Integer productId, Integer SourceTypeId);

    @Query("SELECT la FROM LocalAssessment la WHERE la.sourceTypeId= :sourceId and la.product.id = :productId ORDER BY la.createdTime DESC")
    Optional<LocalAssessment> findLatestBySourceTypeIdAndProductId(Integer sourceId, Integer productId);
}
