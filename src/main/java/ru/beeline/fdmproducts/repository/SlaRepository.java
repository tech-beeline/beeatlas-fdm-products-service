package ru.beeline.fdmproducts.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.beeline.fdmproducts.domain.Sla;

import java.util.Optional;

public interface SlaRepository extends JpaRepository<Sla, Integer> {
    Optional<Sla> findByOperationId(Integer id);
}
