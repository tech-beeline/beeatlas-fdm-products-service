package ru.beeline.fdmproducts.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.beeline.fdmproducts.domain.PatternsCheck;

@Repository
public interface PatternsCheckRepository extends JpaRepository<PatternsCheck, Integer> {
}
