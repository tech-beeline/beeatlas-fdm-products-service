package ru.beeline.fdmproducts.repository;

import org.springframework.data.jpa.repository.EntityGraph;
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

    @Query(value = """
    SELECT
        o.id as opId,
        o.name as opName,
        o.type as opType,
        i.id as interfaceId,
        i.name as interfaceName,
        i.code as interfaceCode,
        cp.id as containerId,
        cp.name as containerName,
        cp.code as containerCode,
        p.id as productId,
        p.name as productName,
        p.alias as productAlias
    FROM product.operation o
    JOIN product.interface i ON o.interface_id = i.id
    JOIN product.containers_product cp ON i.container_id = cp.id
    JOIN product.product p ON cp.product_id = p.id
    WHERE o.name LIKE CONCAT('%', ?1, '%')
      AND (?2 IS NULL OR UPPER(o.type) = UPPER(?2))
      AND o.deleted_date IS NULL
      AND i.deleted_date IS NULL
      AND cp.deleted_date IS NULL
    ORDER BY o.id
    LIMIT 50
    """, nativeQuery = true)
    List<ArchOperationProjection> findArchOperationsProjectionByType(
            String path, String type);

    @Query(value = """
    SELECT
        o.id as opId,
        o.name as opName,
        o.type as opType,
        i.id as interfaceId,
        i.name as interfaceName,
        i.code as interfaceCode,
        cp.id as containerId,
        cp.name as containerName,
        cp.code as containerCode,
        p.id as productId,
        p.name as productName,
        p.alias as productAlias
    FROM product.operation o
    JOIN product.interface i ON o.interface_id = i.id
    JOIN product.containers_product cp ON i.container_id = cp.id
    JOIN product.product p ON cp.product_id = p.id
    WHERE o.name LIKE CONCAT('%', ?1, '%')
      AND o.deleted_date IS NULL
      AND i.deleted_date IS NULL
      AND cp.deleted_date IS NULL
    ORDER BY o.id
    LIMIT 50
    """, nativeQuery = true)
    List<ArchOperationProjection> findArchOperationsProjection(
            String path);

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

    @EntityGraph(attributePaths = {
            "interfaceObj",
            "interfaceObj.containerProduct",
            "interfaceObj.containerProduct.product"
    })
    @Query("SELECT o FROM Operation o " +
            "LEFT JOIN o.interfaceObj i " +
            "LEFT JOIN i.containerProduct c " +
            "LEFT JOIN c.product p " +
            "WHERE o.tcId = :tcId " +
            "AND o.deletedDate IS NULL " +
            "AND (i IS NULL OR i.deletedDate IS NULL) " +
            "AND (c IS NULL OR c.deletedDate IS NULL) ")
    List<Operation> findOperationsWithFullChainGraph(@Param("tcId") Integer tcId);
}
