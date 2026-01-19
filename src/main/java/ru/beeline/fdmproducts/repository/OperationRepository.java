package ru.beeline.fdmproducts.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.beeline.fdmproducts.domain.Operation;
import ru.beeline.fdmproducts.dto.search.projection.ArchOperationProjection;

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

    @Query("""
            SELECT
                o.id AS opId,
                o.name AS opName,
                o.type AS opType,
                i.id AS interfaceId,
                i.name AS interfaceName,
                i.code AS interfaceCode,
                cp.id AS containerId,
                cp.name AS containerName,
                cp.code AS containerCode,
                p.id AS productId,
                p.name AS productName,
                p.alias AS productAlias
            FROM Operation o
            JOIN o.interfaceObj i
            JOIN i.containerProduct cp
            JOIN cp.product p
            WHERE o.name = :path
              AND (:type IS NULL OR UPPER(o.type) = UPPER(:type))
              AND o.deletedDate IS NULL
              AND i.deletedDate IS NULL
              AND cp.deletedDate IS NULL
            """)
    List<ArchOperationProjection> findArchOperationsProjection(@Param("path") String path, @Param("type") String type);

    @Query("""
            SELECT
                o.id AS opId,
                o.name AS opName,
                o.type AS opType,
                i.id AS interfaceId,
                i.name AS interfaceName,
                i.code AS interfaceCode,
                cp.id AS containerId,
                cp.name AS containerName,
                cp.code AS containerCode,
                p.id AS productId,
                p.name AS productName,
                p.alias AS productAlias
            FROM Operation o
            JOIN o.interfaceObj i
            JOIN i.containerProduct cp
            JOIN cp.product p
            WHERE o.id IN :connectionOperationIds
              AND o.deletedDate IS NULL
              AND i.deletedDate IS NULL
              AND cp.deletedDate IS NULL
            """)
    List<ArchOperationProjection> findOperationsProjection(
            @Param("connectionOperationIds") List<Integer> connectionOperationIds
    );
}
