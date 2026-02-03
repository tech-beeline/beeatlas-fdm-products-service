/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.beeline.fdmproducts.domain.Parameter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ParameterRepository extends JpaRepository<Parameter, Integer> {

    Optional<Parameter> findByOperationIdAndParameterNameAndParameterType(Integer operationId, String name, String type);

    List<Parameter> findByOperationId(Integer operationId);

    List<Parameter> findByOperationIdIn(List<Integer> operationIds);

    List<Parameter> findByOperationIdInAndDeletedDateIsNull(List<Integer> operationIds);

    @Modifying
    @Query("UPDATE Parameter p SET p.deletedDate = :deletedDate " +
            "WHERE p.operationId IN :operationIds")
    void markAllParametersAsDeleted(@Param("operationIds") List<Integer> operationIds,
                                    @Param("deletedDate") LocalDateTime deletedDate);
}
