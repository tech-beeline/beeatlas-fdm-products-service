package ru.beeline.fdmproducts.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.beeline.fdmproducts.domain.PatternsAssessment;

@Repository
public interface PatternsAssessmentRepository extends JpaRepository<PatternsAssessment, Integer> {
}
