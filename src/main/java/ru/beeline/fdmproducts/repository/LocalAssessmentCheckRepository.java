/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.beeline.fdmproducts.domain.LocalAssessmentCheck;

import java.util.List;
@Repository
public interface LocalAssessmentCheckRepository extends JpaRepository<LocalAssessmentCheck, Integer> {
    @Query("SELECT lac FROM LocalAssessmentCheck lac WHERE lac.assessment.id = :assessmentId")
    List<LocalAssessmentCheck> findByAssessmentId(@Param("assessmentId") Integer assessmentId);
}
