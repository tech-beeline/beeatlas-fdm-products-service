/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.beeline.fdmproducts.domain.ProductPatterns;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductPatternsRepository extends JpaRepository<ProductPatterns, Integer> {

    @Query("""
            SELECT p FROM ProductPatterns p
            WHERE p.productAlias = :alias
              AND p.productBranch = :branch
              AND p.sourceTypeId = :sourceTypeId
              AND ((:sourceId IS NULL AND p.sourceId IS NULL) OR p.sourceId = :sourceId)
              AND p.patternCode = :patternCode
              AND p.isActual = true
            """)
    Optional<ProductPatterns> findActual(@Param("alias") String alias,
                                         @Param("branch") String branch,
                                         @Param("sourceTypeId") Integer sourceTypeId,
                                         @Param("sourceId") Integer sourceId,
                                         @Param("patternCode") String patternCode);

    List<ProductPatterns> findAllByProductAliasAndProductBranchAndIsActualTrue(String productAlias,
                                                                              String productBranch);
}
