/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.beeline.fdmproducts.domain.EnumSourceType;

import java.util.Optional;

@Repository
public interface EnumSourceTypeRepository extends JpaRepository<EnumSourceType,Integer> {

    Optional<EnumSourceType> findByName(String name);
}
