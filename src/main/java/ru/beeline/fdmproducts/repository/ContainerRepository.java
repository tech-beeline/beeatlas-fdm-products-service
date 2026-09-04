/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.beeline.fdmproducts.domain.ContainerProduct;

import java.util.Date;
import java.util.List;

@Repository
public interface ContainerRepository extends JpaRepository<ContainerProduct, Integer> {

    @EntityGraph(attributePaths = {"interfaces"})
    List<ContainerProduct> findAllByProductBranchId(Integer productBranchId);

    @EntityGraph(attributePaths = {"interfaces"})
    List<ContainerProduct> findAllByProductBranchIdAndDeletedDateIsNull(Integer productBranchId);

    List<ContainerProduct> findAllByCodeInAndProductBranchId(List<String> codes, Integer productBranchId);

    /** Global lookup by code, not scoped to a product — {@code codes} must already be lower-cased. */
    @Query("SELECT c FROM ContainerProduct c WHERE LOWER(c.code) IN :codes AND c.deletedDate IS NULL")
    List<ContainerProduct> findAllByCodeInIgnoreCaseAndDeletedDateIsNull(@Param("codes") List<String> codes);

    @Query("SELECT c FROM ContainerProduct c WHERE c.productBranchId = :productBranchId AND LOWER(c.code) = LOWER(:code)")
    List<ContainerProduct> findAllByProductBranchIdAndCodeIgnoreCase(@Param("productBranchId") Integer productBranchId,
                                                                     @Param("code") String code);

    @Query("SELECT c FROM ContainerProduct c WHERE c.productBranchId = :productBranchId AND c.code IS NULL AND LOWER(c.name) = LOWER(:name)")
    List<ContainerProduct> findAllByProductBranchIdAndCodeIsNullAndNameIgnoreCase(@Param("productBranchId") Integer productBranchId,
                                                                                  @Param("name") String name);

    @Query("SELECT c.id FROM ContainerProduct c WHERE c.productBranchId = :productBranchId AND c.deletedDate IS NULL")
    List<Integer> findContainerIdsByProductBranchIdAndDeletedDateIsNull(Integer productBranchId);

    @Modifying
    @Query("UPDATE ContainerProduct c SET c.deletedDate = :deletedDate WHERE c.productBranchId = :productBranchId AND c.deletedDate IS NULL")
    void markAllContainersAsDeleted(@Param("productBranchId") Integer productBranchId,
                                    @Param("deletedDate") Date deletedDate);

    @Modifying
    @Query("UPDATE ContainerProduct c SET c.deletedDate = :deletedDate WHERE c.id IN :ids")
    void markContainersAsDeletedByIds(@Param("ids") List<Integer> ids,
                                      @Param("deletedDate") Date deletedDate);

    List<ContainerProduct> findAllBySourceMetricIsNotNullAndDeletedDateIsNull();

    @Modifying
    @Query("UPDATE ContainerProduct c SET c.sourceMetric = :sourceMetric WHERE c.id = :id")
    void updateSourceMetricById(@Param("id") Integer id,
                                @Param("sourceMetric") String sourceMetric);

    @Query("SELECT c FROM ContainerProduct c WHERE c.productBranchId = :productBranchId AND LOWER(c.name) IN :containerNames AND c.deletedDate IS NULL")
    List<ContainerProduct> findAllByProductBranchIdAndNameInIgnoreCaseAndDeletedDateIsNull(@Param("productBranchId") Integer productBranchId,
                                                                                           @Param("containerNames") List<String> containerNames);

    @Query("SELECT cp.id FROM ContainerProduct cp WHERE cp.productBranchId = :productBranchId")
    List<Integer> findIdsByProductBranchId(@Param("productBranchId") Integer productBranchId);

    @Query("SELECT cp.id FROM ContainerProduct cp WHERE cp.productBranchId IN :productBranchIds")
    List<Integer> findIdsByProductBranchIdIn(@Param("productBranchIds") List<Integer> productBranchIds);

    @Modifying
    @Query("DELETE FROM ContainerProduct cp WHERE cp.id IN :ids")
    void deleteByIdIn(@Param("ids") List<Integer> ids);
}
