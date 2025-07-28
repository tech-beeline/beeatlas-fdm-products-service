package ru.beeline.fdmproducts.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.beeline.fdmproducts.domain.DiscoveredInterface;
import ru.beeline.fdmproducts.domain.DiscoveredOperation;

import java.util.List;
import java.util.Optional;

@Repository
public interface DiscoveredInterfaceRepository extends JpaRepository<DiscoveredInterface,Integer> {

    List<DiscoveredInterface> findByExternalIdIn(List<Integer> externalIds);
    Optional<DiscoveredInterface> findByDiscoveredInterfaceId(Integer interfaceId);

}
