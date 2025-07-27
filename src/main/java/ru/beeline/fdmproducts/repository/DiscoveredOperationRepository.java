package ru.beeline.fdmproducts.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.beeline.fdmproducts.domain.DiscoveredOperation;

@Repository
public interface DiscoveredOperationRepository extends JpaRepository<DiscoveredOperation,Integer> {
}
