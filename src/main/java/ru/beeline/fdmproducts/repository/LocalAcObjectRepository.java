/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.beeline.fdmproducts.domain.LocalAcObject;

import java.util.List;

@Repository
public interface LocalAcObjectRepository extends JpaRepository<LocalAcObject, Integer> {

    List<LocalAcObject> findAllByLacId(Integer lacId);
    Integer countByLacId(Integer lacId);
    Integer countByLacIdAndIsCheckTrue(Integer lacId);

    @Query("SELECT lao.id FROM LocalAcObject lao WHERE lao.localAssessmentCheck.id IN :assessmentIds")
    List<Integer> findAllByLocalAssessmentCheckIn(List<Integer> assessmentIds);

    @Modifying
    @Query("DELETE FROM LocalAcObject l WHERE l.lacId IN :ids")
    void deleteByLacIdIn(@Param("ids") List<Integer> ids);
}
