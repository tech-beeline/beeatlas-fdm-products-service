/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.beeline.fdmproducts.domain.Operation;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OperationRepository extends JpaRepository<Operation, Integer> {


    @Query(value = "SELECT * FROM product.operation o " +
            "WHERE o.name ILIKE :name " +
            "AND o.type ILIKE :type " +
            "AND o.deleted_date IS NULL " +
            "AND o.interface_id IN (:ids) " +
            "LIMIT 1", nativeQuery = true)
    Optional<Operation> findByNameAndTypeILikeNative(@Param("name") String name,
                                                     @Param("type") String type,
                                                     @Param("ids") List<Integer> ids);

    List<Operation> findAllByInterfaceId(Integer interfaceId);

    List<Operation> findAllByInterfaceIdIn(List<Integer> interfaceIds);

    List<Operation> findAllByInterfaceIdAndDeletedDateIsNull(Integer interfaceId);

    List<Operation> findAllByInterfaceIdInAndDeletedDateIsNull(List<Integer> interfaceIds);

    List<Operation> findAllByIdIn(List<Integer> ids);

    @Query("SELECT o.id FROM Operation o WHERE o.interfaceId IN (:interfaceIds) AND o.deletedDate IS NULL")
    List<Integer> findOperationIdsByInterfaceIdInAndDeletedDateIsNull(List<Integer> interfaceIds);

    @Modifying
    @Query("UPDATE Operation o SET o.deletedDate = :deletedDate " +
            "WHERE o.interfaceId IN :interfaceIds")
    void markAllOperationsAsDeleted(@Param("interfaceIds") List<Integer> interfaceIds,
                                    @Param("deletedDate") LocalDateTime deletedDate);
}
