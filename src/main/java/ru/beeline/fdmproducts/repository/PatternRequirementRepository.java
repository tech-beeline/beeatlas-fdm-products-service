/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.beeline.fdmproducts.domain.PatternRequirement;

import java.util.List;
import java.util.Optional;

@Repository
public interface PatternRequirementRepository extends JpaRepository<PatternRequirement, Integer> {

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"nfr", "nfr.core"})
    @Query("SELECT pr FROM PatternRequirement pr WHERE pr.patternId = :patternId")
    List<PatternRequirement> findByPatternIdWithNfrAndCore(@Param("patternId") Integer patternId);

    List<PatternRequirement> findByPatternId(Integer patternId);

    List<PatternRequirement> findByNfrId(Integer nfrId);

    Optional<PatternRequirement> findByPatternIdAndNfrId(Integer patternId, Integer nfrId);
}
