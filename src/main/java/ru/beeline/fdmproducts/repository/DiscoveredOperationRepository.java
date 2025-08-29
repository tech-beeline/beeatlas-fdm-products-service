package ru.beeline.fdmproducts.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.beeline.fdmproducts.domain.DiscoveredInterface;
import ru.beeline.fdmproducts.domain.DiscoveredOperation;

import java.util.List;
import java.util.Optional;

@Repository
public interface DiscoveredOperationRepository extends JpaRepository<DiscoveredOperation, Integer> {
    Optional<DiscoveredOperation> findByInterfaceIdAndNameAndTypeAndDeletedDateIsNull(Integer interfaceId,
                                                                                      String name,
                                                                                      String type);

    List<DiscoveredOperation> findAllByConnectionOperationIdIn(List<Integer> interfaceId);

   DiscoveredOperation findByConnectionOperationId(Integer operationId);

    List<DiscoveredOperation> findAllByInterfaceId(Integer interfaceId);

    @Query("SELECT do FROM DiscoveredOperation do " + "WHERE do.interfaceId = :interfaceId " + "AND NOT EXISTS (" + "   SELECT 1 FROM DiscoveredOperation do2 JOIN Operation o ON do2.connectionOperationId = o.id " + "   WHERE do2.interfaceId = :interfaceId " + "     AND o.id NOT IN :operationIds" + ")")
    List<DiscoveredOperation> getRows(@Param("interfaceId") Integer interfaceId,
                                      @Param("operationIds") List<Integer> operationIds);
}
