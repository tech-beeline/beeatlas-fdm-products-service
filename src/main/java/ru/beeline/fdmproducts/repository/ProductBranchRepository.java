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

@Repository
public interface ProductBranchRepository extends JpaRepository<ProductBranch, Integer> {

    /**
     * Ветки продукта с указанным именем без учёта регистра. Список, а не Optional: уникальный индекс
     * uq_product_branch_alias_branch_name регистрозависимый, поэтому пара main / Main в таблице
     * возможна — на данных, заведённых мимо API. Выбор из нескольких — за вызывающим кодом.
     */
    List<ProductBranch> findAllByAliasIgnoreCaseAndBranchNameIgnoreCaseOrderByIdAsc(String alias, String branchName);

    List<ProductBranch> findAllByAlias(String alias);

    void deleteByAlias(String alias);

    @Query(value = "INSERT INTO product.product_branch (alias, branch_name) "
            + "VALUES (:alias, :branchName) "
            + "ON CONFLICT (alias, branch_name) DO UPDATE SET alias = EXCLUDED.alias "
            + "RETURNING id", nativeQuery = true)
    Integer upsert(@Param("alias") String alias, @Param("branchName") String branchName);
}
