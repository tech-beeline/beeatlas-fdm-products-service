/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.beeline.fdmproducts.domain.SequenceStepOperation;

import java.util.Collection;

@Repository
public interface SequenceStepOperationRepository extends JpaRepository<SequenceStepOperation, Integer> {

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM SequenceStepOperation o WHERE o.id IN :ids "
            + "AND NOT EXISTS (SELECT 1 FROM SeqProductStep s "
            + "WHERE s.seqStepOperationId = o.id OR s.seqStepRelatedOperationId = o.id)")
    void deleteOrphansByIdIn(@Param("ids") Collection<Integer> ids);
}
