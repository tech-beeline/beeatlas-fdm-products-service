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

    @Query(value = "SELECT pb.branch_name FROM product.containers_product c "
            + "JOIN product.product_branch pb ON pb.id = c.product_branch_id "
            + "WHERE c.id = :containerId", nativeQuery = true)
    Optional<String> findBranchNameByContainerId(@Param("containerId") Integer containerId);

    @Query(value = "SELECT pb.branch_name FROM product.interface i "
            + "JOIN product.containers_product c ON c.id = i.container_id "
            + "JOIN product.product_branch pb ON pb.id = c.product_branch_id "
            + "WHERE i.id = :interfaceId", nativeQuery = true)
    Optional<String> findBranchNameByInterfaceId(@Param("interfaceId") Integer interfaceId);

    @Query(value = "SELECT pb.branch_name FROM product.operation o "
            + "JOIN product.interface i ON i.id = o.interface_id "
            + "JOIN product.containers_product c ON c.id = i.container_id "
            + "JOIN product.product_branch pb ON pb.id = c.product_branch_id "
            + "WHERE o.id = :operationId", nativeQuery = true)
    Optional<String> findBranchNameByOperationId(@Param("operationId") Integer operationId);
}
