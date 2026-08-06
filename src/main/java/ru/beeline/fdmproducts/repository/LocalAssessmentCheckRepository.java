/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.beeline.fdmproducts.domain.LocalAssessmentCheck;

import java.util.List;

@Repository
public interface LocalAssessmentCheckRepository extends JpaRepository<LocalAssessmentCheck, Integer> {

    @Query("SELECT lac.id FROM LocalAssessmentCheck lac WHERE lac.assessment.id IN :assessmentId")
    List<Integer> findByAssessmentIds(@Param("assessmentId") List<Integer> assessmentIds);

    @Modifying
    @Query("DELETE FROM LocalAssessmentCheck l WHERE l.assessment.id IN :ids")
    void deleteByLocalAssessmentIdsIn(@Param("ids") List<Integer> ids);
}
