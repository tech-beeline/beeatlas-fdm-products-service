/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.beeline.fdmproducts.domain.ProductSequence;

import java.util.Optional;

@Repository
public interface ProductSequenceRepository extends JpaRepository<ProductSequence, Integer> {

    @Query("SELECT s FROM ProductSequence s WHERE s.productBranchId = :productBranchId "
            + "AND LOWER(s.code) = LOWER(:code)")
    Optional<ProductSequence> findByProductBranchIdAndCodeIgnoreCase(@Param("productBranchId") Integer productBranchId,
                                                                     @Param("code") String code);
}
