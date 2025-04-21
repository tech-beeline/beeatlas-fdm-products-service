package ru.beeline.fdmproducts.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.beeline.fdmproducts.domain.LocalFitnessFunction;

import java.util.Optional;

public interface LocalFitnessFunctionRepository extends JpaRepository<LocalFitnessFunction, Integer> {
    Optional<LocalFitnessFunction> findByCode(String code);
}
