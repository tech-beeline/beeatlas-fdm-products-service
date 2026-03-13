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
import ru.beeline.fdmproducts.dto.LacCountsDTO;

import java.util.List;

@Repository
public interface LocalAcObjectRepository extends JpaRepository<LocalAcObject, Integer> {

    List<LocalAcObject> findAllByLacId(Integer lacId);

    @Query("SELECT lao.id FROM LocalAcObject lao WHERE lao.localAssessmentCheck.id IN :assessmentIds")
    List<Integer> findAllByLocalAssessmentCheckIn(List<Integer> assessmentIds);

    @Modifying
    @Query("DELETE FROM LocalAcObject l WHERE l.lacId IN :ids")
    void deleteByLacIdIn(@Param("ids") List<Integer> ids);

    @Query("SELECT NEW ru.beeline.fdmproducts.dto.LacCountsDTO(" +
            "    lao.lacId, " +
            "    CAST(COUNT(lao.id) AS integer), " +
            "    CAST(SUM(CASE WHEN lao.isCheck = true THEN 1 ELSE 0 END) AS integer)" +
            ") " +
            "FROM LocalAcObject lao " +
            "WHERE lao.lacId IN :lacIds " +
            "GROUP BY lao.lacId")
    List<LacCountsDTO> countsByLacIds(@Param("lacIds") List<Integer> lacIds);
}
