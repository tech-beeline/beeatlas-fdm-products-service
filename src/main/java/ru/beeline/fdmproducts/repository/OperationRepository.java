/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

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


    /**
     * Единственный предикат сопоставления «метод ↔ архитектурная операция» в сервисе: по нему и
     * автоматическое сопоставление discovered-операций проставляет connection_operation_id
     * (ComparisonOperationsService), и поиск кандидатов из UI (POST /api/v1/operation/search-matched).
     * Разводить их нельзя — иначе UI покажет совпадения, которых автомат не сделает, и наоборот.
     * <p>
     * Выборка отбирает кандидатов области поиска, а имя метода сравнивает уже Java —
     * {@link ru.beeline.fdmproducts.utils.OperationPathMatcher}: имена path-параметров не важны,
     * query отбрасывается, «_» и «%» — обычные символы, конкретное значение совпадает с шаблоном
     * каталога, но не наоборот. type сравнивается здесь через lower() — регистронезависимое
     * равенство целиком. {@code type} и {@code protocol} необязательны:
     * null снимает соответствующий фильтр. Переданный protocol отбрасывает интерфейсы с
     * {@code protocol IS NULL} — сравнение с NULL не истинно. Удалённые записи отсекаются на каждом
     * уровне цепочки; product фильтровать не по чему — в таблице нет deleted_date, продукты удаляются
     * физически. Порядок по o.id — чтобы «первое совпадение» у автомата было воспроизводимым.
     * <p>
     * {@code interfaceIds} не должен быть пустым: {@code IN ()} — синтаксическая ошибка в SQL.
     */
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
                p.alias as productAlias,
                pb.branch_name as productBranchName
            FROM product.operation o
            JOIN product.interface i ON o.interface_id = i.id
            JOIN product.containers_product cp ON i.container_id = cp.id
            JOIN product.product_branch pb ON cp.product_branch_id = pb.id
            JOIN product.product p ON p.alias = pb.alias
            WHERE (CAST(:type AS text) IS NULL OR lower(o.type) = lower(CAST(:type AS text)))
              AND (CAST(:protocol AS text) IS NULL OR i.protocol ILIKE CAST(:protocol AS text))
              AND o.deleted_date IS NULL
              AND i.deleted_date IS NULL
              AND cp.deleted_date IS NULL
              AND o.interface_id IN (:interfaceIds)
            ORDER BY o.id
            """, nativeQuery = true)
    List<ArchOperationProjection> findArchOperationsForMatching(@Param("type") String type,
                                                                 @Param("protocol") String protocol,
                                                                 @Param("interfaceIds") List<Integer> interfaceIds);

    List<Operation> findAllByInterfaceId(Integer interfaceId);

    List<Operation> findAllByInterfaceIdIn(List<Integer> interfaceIds);

    List<Operation> findAllByInterfaceIdAndDeletedDateIsNull(Integer interfaceId);

    List<Operation> findAllByInterfaceIdInAndDeletedDateIsNull(List<Integer> interfaceIds);

    List<Operation> findAllByIdIn(List<Integer> ids);

    @Query("SELECT o.id FROM Operation o WHERE o.interfaceId IN (:interfaceIds) AND o.deletedDate IS NULL")
    List<Integer> findOperationIdsByInterfaceIdInAndDeletedDateIsNull(List<Integer> interfaceIds);

    @Modifying
    @Query("UPDATE Operation o SET o.deletedDate = :deletedDate " +
            "WHERE o.id IN :ids")
    void markOperationsAsDeletedByIds(@Param("ids") List<Integer> ids,
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
                p.alias as productAlias,
                pb.branch_name as productBranchName
            FROM product.operation o
            JOIN product.interface i ON o.interface_id = i.id
            JOIN product.containers_product cp ON i.container_id = cp.id
            JOIN product.product_branch pb ON cp.product_branch_id = pb.id
            JOIN product.product p ON p.alias = pb.alias
            WHERE o.name ILIKE CONCAT('%', ?1, '%')
              AND (?2 IS NULL OR o.type ILIKE ?2)
              AND o.deleted_date IS NULL
              AND i.deleted_date IS NULL
              AND cp.deleted_date IS NULL
            ORDER BY o.id
            LIMIT 50
            """, nativeQuery = true)
    List<ArchOperationProjection> findArchOperationsProjectionByType(String path, String type);

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
                p.alias as productAlias,
                pb.branch_name as productBranchName
            FROM product.operation o
            JOIN product.interface i ON o.interface_id = i.id
            JOIN product.containers_product cp ON i.container_id = cp.id
            JOIN product.product_branch pb ON cp.product_branch_id = pb.id
            JOIN product.product p ON p.alias = pb.alias
            WHERE o.name ILIKE CONCAT('%', ?1, '%')
              AND o.deleted_date IS NULL
              AND i.deleted_date IS NULL
              AND cp.deleted_date IS NULL
            ORDER BY o.id
            LIMIT 50
            """, nativeQuery = true)
    List<ArchOperationProjection> findArchOperationsProjection(String path);

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
                p.alias AS productAlias,
                pb.branchName AS productBranchName
            FROM Operation o
            JOIN o.interfaceObj i
            JOIN i.containerProduct cp
            JOIN cp.productBranch pb
            JOIN pb.product p
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
            "interfaceObj.containerProduct.productBranch",
            "interfaceObj.containerProduct.productBranch.product"
    })
    @Query("SELECT o FROM Operation o " +
            "LEFT JOIN o.interfaceObj i " +
            "LEFT JOIN i.containerProduct c " +
            "LEFT JOIN c.productBranch pb " +
            "LEFT JOIN pb.product p " +
            "WHERE o.tcId = :tcId " +
            "AND o.deletedDate IS NULL " +
            "AND (i IS NULL OR i.deletedDate IS NULL) " +
            "AND (c IS NULL OR c.deletedDate IS NULL) ")
    List<Operation> findOperationsWithFullChainGraph(@Param("tcId") Integer tcId);

    @Modifying
    @Query("UPDATE Operation o SET o.isDeletedTc = true " +
            "WHERE o.tcId = :tcId AND (o.isDeletedTc = false OR o.isDeletedTc IS NULL)")
    void markAsDeleted(@Param("tcId") Integer tcId);

    @Modifying
    @Query("UPDATE Operation o SET o.isDeletedTc = false " +
            "WHERE o.tcId = :tcId AND (o.isDeletedTc = true OR o.isDeletedTc IS NULL)")
    void markAsUpdated(@Param("tcId") Integer tcId);

    @Query("SELECT o.id FROM Operation o WHERE o.interfaceId IN :interfaceIds")
    List<Integer> findIdsByInterfaceIds(@Param("interfaceIds") List<Integer> interfaceIds);

    @Modifying
    @Query("DELETE FROM Operation o WHERE o.id IN :ids")
    void deleteByIdIn(@Param("ids") List<Integer> ids);

    Optional<Operation> findByNameAndTypeAndInterfaceIdAndDeletedDateIsNull(String name,
                                                                              String type,
                                                                              Integer interfaceId);

    Optional<Operation> findByNameAndTypeAndInterfaceId(String name, String type, Integer interfaceId);

    @Query(value = """
            SELECT o.id
            FROM product.operation o
            JOIN product.interface i ON o.interface_id = i.id
            JOIN product.containers_product cp ON i.container_id = cp.id
            JOIN product.product_branch pb ON cp.product_branch_id = pb.id
            WHERE LOWER(pb.alias) = LOWER(CAST(:productAlias AS text))
              AND LOWER(pb.branch_name) = LOWER(CAST(:branch AS text))
              AND (CAST(:containerCode AS text) IS NULL OR LOWER(cp.code) = LOWER(CAST(:containerCode AS text)))
              AND (CAST(:interfaceCode AS text) IS NULL OR LOWER(i.code) = LOWER(CAST(:interfaceCode AS text)))
              AND LOWER(o.name) = LOWER(CAST(:name AS text))
              AND LOWER(o.type) = LOWER(CAST(:type AS text))
              AND o.deleted_date IS NULL
              AND i.deleted_date IS NULL
              AND cp.deleted_date IS NULL
            ORDER BY o.id
            LIMIT 2
            """, nativeQuery = true)
    List<Integer> findIdsForSequenceStep(@Param("productAlias") String productAlias,
                                         @Param("branch") String branch,
                                         @Param("containerCode") String containerCode,
                                         @Param("interfaceCode") String interfaceCode,
                                         @Param("name") String name,
                                         @Param("type") String type);
}
