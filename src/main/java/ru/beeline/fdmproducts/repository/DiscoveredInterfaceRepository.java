package ru.beeline.fdmproducts.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.beeline.fdmproducts.domain.DiscoveredInterface;
import ru.beeline.fdmproducts.domain.Product;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;

@Repository
public interface DiscoveredInterfaceRepository extends JpaRepository<DiscoveredInterface, Integer> {

    List<DiscoveredInterface> findByExternalIdIn(List<Integer> externalIds);

    @Modifying
    @Transactional
    @Query("UPDATE DiscoveredInterface di SET di.connectionInterfaceId = null WHERE di.id <> :mapicInterfaceId AND di.connectionInterfaceId = :archInterfaceId")
    int clearConnectionInterfaceIdExcept(@Param("archInterfaceId") Integer archInterfaceId,
                                         @Param("mapicInterfaceId") Integer mapicInterfaceId);
    @Modifying
    @Transactional
    @Query(value = "UPDATE product.discovered_interface SET connection_interface_id = NULL " +
            "WHERE connection_interface_id IN (SELECT id FROM product.interface WHERE container_id = :entityId)", nativeQuery = true)
    int clearConnectionInterfaceIdByEntityId(@Param("entityId") Integer entityId);

    @Modifying
    @Transactional
    @Query(value = "UPDATE product.discovered_interface SET connection_interface_id = NULL " +
            "WHERE connection_interface_id  = :interfaceId", nativeQuery = true)
    int clearConnectionInterfaceIdByInterfaceId(@Param("interfaceId") Integer interfaceId);

    @Modifying
    @Transactional
    @Query(value = "UPDATE product.discovered_interface i SET connection_interface_id = NULL " +
            "  JOIN product.discovered_operation o ON o.interface_id = i.id " +
            "WHERE connection_operation_id  = :operationId", nativeQuery = true)
    int clearConnectionInterfaceIdByOperationId(@Param("operationId") Integer operationId);

    List<DiscoveredInterface> findAllByConnectionInterfaceId(Integer interfaceId);

    List<DiscoveredInterface> findAllByConnectionInterfaceIdIn(List<Integer> interfaceIds);

    List<DiscoveredInterface> findAllByProduct(Product product);

    Optional<DiscoveredInterface> findByExternalId(Integer externalId);

    Optional<DiscoveredInterface> findByApiId(Integer apiId);
}
