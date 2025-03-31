package ru.beeline.fdmproducts.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.beeline.fdmproducts.domain.LocalAssessment;
import ru.beeline.fdmproducts.domain.LocalFitnessFunction;

import java.util.Optional;

public interface LocalAssessmentRepository extends JpaRepository<LocalAssessment, Integer> {
}
