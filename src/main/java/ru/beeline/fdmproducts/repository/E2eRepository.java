/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.beeline.fdmproducts.domain.E2e;

import java.util.List;
import java.util.Optional;

@Repository
public interface E2eRepository extends JpaRepository<E2e, Integer> {

    Optional<E2e> findByCode(String code);

    @Query("SELECT e FROM E2e e WHERE LOWER(e.code) = LOWER(:code)")
    Optional<E2e> findByCodeIgnoreCase(@Param("code") String code);

    @Query("SELECT e FROM E2e e WHERE e.biStepCode IS NULL OR TRIM(e.biStepCode) = ''")
    List<E2e> findAllWithoutBiStepCode();

    @Query("SELECT e FROM E2e e WHERE e.biStepCode IS NOT NULL AND TRIM(e.biStepCode) <> ''")
    List<E2e> findAllWithBiStepCode();
}

