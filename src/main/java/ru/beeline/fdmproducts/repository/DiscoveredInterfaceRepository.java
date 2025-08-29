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

    Optional<DiscoveredInterface> findByConnectionInterfaceId(Integer interfaceId);

    List<DiscoveredInterface> findAllByProduct(Product product);

    Optional<DiscoveredInterface> findByExternalId(Integer externalId);

    Optional<DiscoveredInterface> findByApiId(Integer apiId);
}
