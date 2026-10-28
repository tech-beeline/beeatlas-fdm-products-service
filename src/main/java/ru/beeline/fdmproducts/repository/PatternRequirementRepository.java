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

    @Query(value = ""
            + "WITH latest AS ("
            + "  SELECT DISTINCT ON (e.core_id) e.id, e.core_id "
            + "  FROM product.non_functional_requirement_enum e "
            + "  ORDER BY e.core_id, e.version DESC"
            + "), product_cores AS ("
            + "  SELECT DISTINCT e.core_id "
            + "  FROM product.non_functional_requirement_enum e "
            + "  WHERE e.id IN (:nfrIds)"
            + ") "
            + "SELECT pr.pattern_id "
            + "FROM product.pattern_requirement pr "
            + "JOIN latest l ON l.id = pr.nfr_id "
            + "LEFT JOIN product_cores pc ON pc.core_id = l.core_id "
            + "GROUP BY pr.pattern_id "
            + "HAVING COUNT(*) = COUNT(pc.core_id) "
            + "ORDER BY pr.pattern_id",
            nativeQuery = true)
    List<Integer> findPatternIdsWhereAllRequirementsIn(@Param("nfrIds") List<Integer> nfrIds);

    List<PatternRequirement> findByNfrIdIn(List<Integer> nfrIds);
}
