/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.beeline.fdmproducts.domain.Sla;

import java.util.List;
import java.util.Optional;

@Repository
public interface SlaRepository extends JpaRepository<Sla, Integer> {
    Optional<Sla> findByOperationId(Integer id);

    List<Sla> findAllByOperationIdIn(List<Integer> operationIds);

    @Modifying
    @Query("DELETE FROM Sla s WHERE s.operationId IN :operationIds")
    void deleteByOperationIdIn(@Param("operationIds") List<Integer> operationIds);
}
