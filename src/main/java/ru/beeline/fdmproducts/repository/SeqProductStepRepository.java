/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.beeline.fdmproducts.domain.SeqProductStep;

import java.util.List;

@Repository
public interface SeqProductStepRepository extends JpaRepository<SeqProductStep, Integer> {

    List<SeqProductStep> findAllBySeqId(Integer seqId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM SeqProductStep s WHERE s.seqId = :seqId")
    void deleteAllBySeqId(@Param("seqId") Integer seqId);
}
