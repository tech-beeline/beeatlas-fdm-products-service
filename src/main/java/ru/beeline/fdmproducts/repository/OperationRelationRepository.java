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

import java.util.Collection;
import java.util.List;

@Repository
public interface OperationRelationRepository extends JpaRepository<OperationRelation, Integer> {

    @Modifying
    @Query("DELETE FROM OperationRelation r WHERE r.e2eId = :e2eId")
    void deleteAllByE2eId(@Param("e2eId") Integer e2eId);

    List<OperationRelation> findAllByE2eId(Integer e2eId);

    /**
     * Связи e2e, где discovered_operation из ids стоит на любой стороне (SFDM-4244: обе колонки — id
     * product.discovered_operation, фильтра по типу нет).
     */
    @Query("""
            SELECT r FROM OperationRelation r
            WHERE r.e2eId IS NOT NULL
              AND (r.operationId IN :ids OR r.relatedOperationId IN :ids)
            """)
    List<OperationRelation> findDiscoveredOperationParticipations(@Param("ids") Collection<Integer> ids);
}
