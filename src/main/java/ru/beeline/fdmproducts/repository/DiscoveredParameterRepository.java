/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.beeline.fdmproducts.domain.DiscoveredParameter;

import java.util.List;

@Repository
public interface DiscoveredParameterRepository extends JpaRepository<DiscoveredParameter, Integer> {


    @Modifying
    @Query("DELETE FROM DiscoveredParameter dp WHERE dp.discoveredOperation.id IN :operationIds")
    void deleteByDiscoveredOperationIdIn(@Param("operationIds") List<Integer> operationIds);
}
