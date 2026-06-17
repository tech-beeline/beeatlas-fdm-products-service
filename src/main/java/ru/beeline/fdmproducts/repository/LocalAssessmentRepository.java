/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.beeline.fdmproducts.domain.LocalAssessment;
import ru.beeline.fdmproducts.domain.Product;
import ru.beeline.fdmproducts.dto.LatestAssessmentCheckDTO;

import java.util.List;
import java.util.Optional;

@Repository
public interface LocalAssessmentRepository extends JpaRepository<LocalAssessment, Integer> {


    @Query("SELECT la FROM LocalAssessment la WHERE la.product.id = :productId ORDER BY la.createdTime DESC")
    List<LocalAssessment> findLatestByProductId(@Param("productId") Integer productId);

    Optional<LocalAssessment> findBySourceIdAndProduct(Integer sourceId, Product product);

    Optional<LocalAssessment> findBySourceIdAndProductIdAndSourceTypeId(Integer sourceId, Integer productId, Integer SourceTypeId);

    Optional<LocalAssessment> findFirstBySourceTypeIdAndProductIdOrderByCreatedTimeDesc(Integer sourceId, Integer productId);

    @Query("SELECT la.id FROM LocalAssessment la WHERE la.product.id = :productId")
    List<Integer> findIdsByProductId(@Param("productId") Integer productId);

    @Modifying
    @Query("DELETE FROM LocalAssessment la WHERE la.product.id = :productId")
    void deleteByProductId(@Param("productId") Integer productId);

    @Query("SELECT la.id, la.product.id FROM LocalAssessment la " +
            "WHERE la.product.id IN :productIds " +
            "ORDER BY la.product.id ASC, la.createdTime DESC")
    List<Object[]> findAssessmentIdsOrderedByProductAndTime(@Param("productIds") List<Integer> productIds);

    @Query("SELECT NEW ru.beeline.fdmproducts.dto.LatestAssessmentCheckDTO(" +
            "    lac.assessment.product.id, " +
            "    lac.id, " +
            "    lac.isCheck, " +
            "    lac.fitnessFunction.id" +
            ") " +
            "FROM LocalAssessmentCheck lac " +
            "WHERE lac.assessment.id IN :assessmentIds")
    List<LatestAssessmentCheckDTO> findChecksByAssessmentIds(@Param("assessmentIds") List<Integer> assessmentIds);
}
