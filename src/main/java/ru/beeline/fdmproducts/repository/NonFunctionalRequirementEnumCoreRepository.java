/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.beeline.fdmproducts.domain.NonFunctionalRequirementEnumCore;

import java.util.Optional;

@Repository
public interface NonFunctionalRequirementEnumCoreRepository extends JpaRepository<NonFunctionalRequirementEnumCore, Integer> {

    Optional<NonFunctionalRequirementEnumCore> findByCode(String code);
}
