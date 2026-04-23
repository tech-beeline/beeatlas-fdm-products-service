/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.beeline.fdmproducts.domain.NonFunctionalRequirement;

import java.util.List;
import java.util.Optional;

@Repository
public interface NonFunctionalRequirementRepository extends JpaRepository<NonFunctionalRequirement, Integer> {

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"nfr", "nfr.core"})
    @Query("SELECT nfr FROM NonFunctionalRequirement nfr WHERE nfr.product.id = :productId")
    List<NonFunctionalRequirement> findByProductIdWithNfrAndCore(@Param("productId") Integer productId);

    List<NonFunctionalRequirement> findByProductId(Integer productId);

    List<NonFunctionalRequirement> findByNfrId(Integer nfrId);

    @Query("SELECT DISTINCT nfr.nfr.id FROM NonFunctionalRequirement nfr WHERE nfr.product.id = :productId")
    List<Integer> findDistinctNfrEnumIdsByProductId(@Param("productId") Integer productId);

    Optional<NonFunctionalRequirement> findByProduct_IdAndNfr_Id(Integer productId, Integer nfrId);

    @Query("SELECT nfr FROM NonFunctionalRequirement nfr WHERE nfr.product.id = :productId AND nfr.nfr.id IN :nfrIds")
    List<NonFunctionalRequirement> findByProductIdAndNfrIds(@Param("productId") Integer productId,
                                                            @Param("nfrIds") List<Integer> nfrIds);
}
