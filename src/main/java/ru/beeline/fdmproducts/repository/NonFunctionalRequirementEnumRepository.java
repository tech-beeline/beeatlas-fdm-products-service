/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.beeline.fdmproducts.domain.NonFunctionalRequirementEnum;

import java.util.List;

@Repository
public interface NonFunctionalRequirementEnumRepository extends JpaRepository<NonFunctionalRequirementEnum, Integer> {

    List<NonFunctionalRequirementEnum> findByCoreId(Integer coreId);

    boolean existsByCoreIdAndVersionGreaterThan(Integer coreId, Integer version);

    @EntityGraph(attributePaths = {"core"})
    @Query("SELECT nfr FROM NonFunctionalRequirementEnum nfr WHERE nfr.id NOT IN (SELECT cn.nfr.id FROM ChapterNfr cn)")
    List<NonFunctionalRequirementEnum> findAllUnbound();
}

