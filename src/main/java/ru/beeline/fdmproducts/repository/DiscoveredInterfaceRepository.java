package ru.beeline.fdmproducts.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.beeline.fdmproducts.domain.DiscoveredInterface;

import java.util.List;

@Repository
public interface DiscoveredInterfaceRepository extends JpaRepository<DiscoveredInterface,Integer> {

    List<DiscoveredInterface> findByExternalIdIn(List<Integer> externalIds);
}
