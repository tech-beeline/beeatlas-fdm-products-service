/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.beeline.fdmproducts.domain.LocalFitnessFunction;

import java.util.List;
import java.util.Optional;

@Repository
public interface LocalFitnessFunctionRepository extends JpaRepository<LocalFitnessFunction, Integer> {

    Optional<LocalFitnessFunction> findByCode(String code);

    List<LocalFitnessFunction> findByCodeIn(List<String> codes);

    @Query("SELECT lff FROM LocalFitnessFunction lff WHERE LOWER(lff.code) IN :lowerCodes")
    List<LocalFitnessFunction> findByCodeInIgnoreCase(@Param("lowerCodes") List<String> lowerCodes);
}
