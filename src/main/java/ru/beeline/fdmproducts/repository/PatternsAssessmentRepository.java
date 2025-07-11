package ru.beeline.fdmproducts.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.beeline.fdmproducts.domain.PatternsAssessment;

import java.util.Optional;

@Repository
public interface PatternsAssessmentRepository extends JpaRepository<PatternsAssessment, Integer> {

    Optional<PatternsAssessment> findFirstByProductIdOrderByCreateDateDesc(Integer productId);

    Optional<PatternsAssessment> findBySourceType_NameAndSourceId(String name, Integer sourceId);

    Optional<PatternsAssessment> findFirstBySourceType_NameOrderByCreateDateDesc(String name);
}
