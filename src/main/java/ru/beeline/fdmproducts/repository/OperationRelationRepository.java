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

import java.util.List;

@Repository
public interface OperationRelationRepository extends JpaRepository<OperationRelation, Integer> {

    @Modifying
    @Query("DELETE FROM OperationRelation r WHERE r.e2eId = :e2eId")
    void deleteAllByE2eId(@Param("e2eId") Integer e2eId);

    List<OperationRelation> findAllByE2eId(Integer e2eId);

    @Query(value = "SELECT DISTINCT operation_id FROM product.operation_relations WHERE operation_id IN :ids "
            + "UNION "
            + "SELECT DISTINCT related_operation_id FROM product.operation_relations WHERE related_operation_id IN :ids",
            nativeQuery = true)
    List<Integer> findOperationIdsInUse(@Param("ids") List<Integer> ids);
}
