/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.beeline.fdmproducts.domain.OperationRelation;
import ru.beeline.fdmproducts.dto.e2e.E2eRelationParticipation;

import java.util.Collection;
import java.util.List;

@Repository
public interface OperationRelationRepository extends JpaRepository<OperationRelation, Integer> {

    @Modifying
    @Query("DELETE FROM OperationRelation r WHERE r.e2eBranchId = :e2eBranchId")
    void deleteAllByE2eBranchId(@Param("e2eBranchId") Integer e2eBranchId);

    List<OperationRelation> findAllByE2eBranchId(Integer e2eBranchId);

    /**
     * Связи веток e2e с именем branchName (без учёта регистра), где discovered_operation из ids стоит на
     * любой стороне (SFDM-4244: обе колонки — id product.discovered_operation, фильтра по типу нет), вместе
     * с id карточки e2e (SFDM-4250). Строк одной карточки, различающихся только регистром имени, нет:
     * их запрещает uq_e2e_branch_code_lower_branch_name.
     */
    @Query("""
            SELECT new ru.beeline.fdmproducts.dto.e2e.E2eRelationParticipation(r, e.id)
            FROM OperationRelation r, E2eBranch b, E2e e
            WHERE b.id = r.e2eBranchId
              AND e.code = b.code
              AND LOWER(b.branchName) = :branchName
              AND (r.operationId IN :ids OR r.relatedOperationId IN :ids)
            """)
    List<E2eRelationParticipation> findDiscoveredOperationParticipations(@Param("ids") Collection<Integer> ids,
                                                                         @Param("branchName") String branchName);
}
