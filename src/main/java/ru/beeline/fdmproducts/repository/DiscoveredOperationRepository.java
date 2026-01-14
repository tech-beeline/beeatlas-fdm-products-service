package ru.beeline.fdmproducts.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.beeline.fdmproducts.domain.DiscoveredOperation;

import java.util.List;
import java.util.Optional;

@Repository
public interface DiscoveredOperationRepository extends JpaRepository<DiscoveredOperation, Integer> {
    Optional<DiscoveredOperation> findByInterfaceIdAndNameAndTypeAndDeletedDateIsNull(Integer interfaceId,
                                                                                      String name,
                                                                                      String type);

    List<DiscoveredOperation> findAllByConnectionOperationIdIn(List<Integer> interfaceId);

    List<DiscoveredOperation> findAllByConnectionOperationId(Integer operationId);

    List<DiscoveredOperation> findAllByInterfaceIdAndDeletedDateIsNull(Integer interfaceId);

    @Modifying
    @Transactional
    @Query(value = "UPDATE product.discovered_operation SET connection_operation_id = NULL " +
            "WHERE connection_operation_id IN (" +
            "  SELECT o.id FROM product.operation o " +
            "  JOIN product.interface i ON o.interface_id = i.id " +
            "  WHERE i.container_id = :entityId)", nativeQuery = true)
    int clearConnectionOperationIdByEntityId(@Param("entityId") Integer entityId);

    @Modifying
    @Transactional
    @Query(value = "UPDATE product.discovered_operation SET connection_operation_id = NULL " +
            "WHERE connection_operation_id IN (" +
            "  SELECT o.id FROM product.operation o " +
            "  JOIN product.interface i ON o.interface_id = i.id " +
            "  WHERE i.id = :interfaceId)", nativeQuery = true)
    int clearConnectionOperationIdByInterfaceId(@Param("interfaceId") Integer interfaceId);

    @Modifying
    @Transactional
    @Query(value = "UPDATE product.discovered_operation SET connection_operation_id = NULL " +
            "WHERE connection_operation_id = :operationId", nativeQuery = true)
    int clearConnectionOperationIdByOperationId(@Param("operationId") Integer operationId);

    @Query("SELECT do FROM DiscoveredOperation do "
            + "WHERE do.interfaceId = :interfaceId "
            + "AND NOT EXISTS (" + "   SELECT 1 FROM DiscoveredOperation do2 JOIN Operation o ON do2.connectionOperationId = o.id "
            + "   WHERE do2.interfaceId = :interfaceId "
            + "     AND o.id NOT IN (:operationIds))")
    List<DiscoveredOperation> getRows(@Param("interfaceId") Integer interfaceId,
                                      @Param("operationIds") List<Integer> operationIds);

    List<DiscoveredOperation> findAllByInterfaceIdIn(List<Integer> discoveredInterfaceIds);

    List<DiscoveredOperation> findAllByInterfaceIdInAndDeletedDateIsNull(List<Integer> discoveredInterfaceIds);

    List<DiscoveredOperation> findAllByNameAndDeletedDateIsNull(String name);

    List<DiscoveredOperation> findAllByNameAndTypeAndDeletedDateIsNull(String name, String type);
}
