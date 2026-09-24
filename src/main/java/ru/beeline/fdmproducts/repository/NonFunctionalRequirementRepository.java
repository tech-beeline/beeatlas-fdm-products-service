/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.beeline.fdmproducts.domain.NonFunctionalRequirement;

import java.util.List;
import java.util.Optional;

@Repository
public interface NonFunctionalRequirementRepository extends JpaRepository<NonFunctionalRequirement, Integer> {

    @EntityGraph(attributePaths = {"nfr", "nfr.core"})
    @Query("SELECT nfr FROM NonFunctionalRequirement nfr WHERE nfr.productBranch.id = :productBranchId")
    List<NonFunctionalRequirement> findByProductBranchIdWithNfrAndCore(@Param("productBranchId") Integer productBranchId);

    List<NonFunctionalRequirement> findByProductBranch_Id(Integer productBranchId);

    List<NonFunctionalRequirement> findByNfrId(Integer nfrId);

    @Query("SELECT DISTINCT nfr.nfr.id FROM NonFunctionalRequirement nfr WHERE nfr.productBranch.id = :productBranchId")
    List<Integer> findDistinctNfrEnumIdsByProductBranchId(@Param("productBranchId") Integer productBranchId);

    Optional<NonFunctionalRequirement> findByProductBranch_IdAndNfr_Id(Integer productBranchId, Integer nfrId);

    @Query("SELECT nfr FROM NonFunctionalRequirement nfr WHERE nfr.productBranch.id = :productBranchId AND nfr.nfr.id IN :nfrIds")
    List<NonFunctionalRequirement> findByProductBranchIdAndNfrIds(@Param("productBranchId") Integer productBranchId,
                                                                  @Param("nfrIds") List<Integer> nfrIds);

    @EntityGraph(attributePaths = {"productBranch"})
    @Query("SELECT rel FROM NonFunctionalRequirement rel WHERE rel.id IN :ids")
    List<NonFunctionalRequirement> findAllByIdInWithProductBranch(@Param("ids") List<Integer> ids);

    @EntityGraph(attributePaths = {"productBranch", "productBranch.product"})
    @Query("SELECT r FROM NonFunctionalRequirement r WHERE r.nfr.id = :nfrId")
    List<NonFunctionalRequirement> findByNfrIdWithProductBranch(@Param("nfrId") Integer nfrId);
}
