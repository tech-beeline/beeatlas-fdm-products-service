package ru.beeline.fdmproducts.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.beeline.fdmproducts.domain.LocalFitnessFunction;

import java.util.Optional;
@Repository
public interface LocalFitnessFunctionRepository extends JpaRepository<LocalFitnessFunction, Integer> {
    Optional<LocalFitnessFunction> findByCode(String code);
}
