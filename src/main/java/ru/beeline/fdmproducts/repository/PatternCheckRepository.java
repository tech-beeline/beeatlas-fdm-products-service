/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.beeline.fdmproducts.domain.PatternCheck;

import java.util.List;

@Repository
public interface PatternCheckRepository extends JpaRepository<PatternCheck, Integer> {

    List<PatternCheck> findByProductId(Integer productId);

    List<PatternCheck> findByPatternCode(String patternCode);
}
