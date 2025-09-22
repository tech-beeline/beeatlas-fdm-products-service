package ru.beeline.fdmproducts.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.beeline.fdmproducts.domain.DiscoveredParameter;

import java.util.List;

@Repository
public interface DiscoveredParameterRepository extends JpaRepository<DiscoveredParameter, Integer> {
    List<DiscoveredParameter> findByDiscoveredOperationIdAndDeletedDateIsNull(Integer operationId);
}
