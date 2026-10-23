/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.beeline.fdmproducts.domain.E2eBranch;

import java.util.List;

@Repository
public interface E2eBranchRepository extends JpaRepository<E2eBranch, Integer> {

    /**
     * Ветки карточки с указанным именем без учёта регистра. Список, а не Optional — по той же причине,
     * что и в ProductBranchRepository: выбор из строк, различающихся регистром, за вызывающим кодом.
     */
    List<E2eBranch> findAllByCodeAndBranchNameIgnoreCaseOrderByIdAsc(String code, String branchName);

    @Query(value = "INSERT INTO product.e2e_branch (code, branch_name) "
            + "VALUES (:code, :branchName) "
            + "ON CONFLICT (code, branch_name) DO UPDATE SET code = EXCLUDED.code "
            + "RETURNING id", nativeQuery = true)
    Integer upsert(@Param("code") String code, @Param("branchName") String branchName);
}
