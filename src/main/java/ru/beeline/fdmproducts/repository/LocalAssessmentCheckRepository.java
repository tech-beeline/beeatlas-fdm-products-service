package ru.beeline.fdmproducts.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.beeline.fdmproducts.domain.LocalAssessmentCheck;

public interface LocalAssessmentCheckRepository extends JpaRepository<LocalAssessmentCheck, Integer> {
}
