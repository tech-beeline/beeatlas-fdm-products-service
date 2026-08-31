/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.beeline.fdmproducts.domain.ProductBranch;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductBranchRepository extends JpaRepository<ProductBranch, Integer> {

    Optional<ProductBranch> findByAliasAndBranchName(String alias, String branchName);

    List<ProductBranch> findAllByAlias(String alias);

    void deleteByAlias(String alias);

    @Query(value = "INSERT INTO product.product_branch (alias, branch_name) "
            + "VALUES (:alias, :branchName) "
            + "ON CONFLICT (alias, branch_name) DO UPDATE SET alias = EXCLUDED.alias "
            + "RETURNING id", nativeQuery = true)
    Integer upsert(@Param("alias") String alias, @Param("branchName") String branchName);
}
