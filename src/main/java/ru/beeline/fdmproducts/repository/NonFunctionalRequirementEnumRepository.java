/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.beeline.fdmproducts.domain.NonFunctionalRequirementEnum;

import java.util.List;

@Repository
public interface NonFunctionalRequirementEnumRepository extends JpaRepository<NonFunctionalRequirementEnum, Integer> {

    List<NonFunctionalRequirementEnum> findByCoreId(Integer coreId);
}
