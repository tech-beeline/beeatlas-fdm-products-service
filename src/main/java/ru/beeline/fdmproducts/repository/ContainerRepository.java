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
    List<ContainerProduct> findAllByProductId(Integer productId);

    @EntityGraph(attributePaths = {"interfaces"})
    List<ContainerProduct> findAllByProductIdAndDeletedDateIsNull(Integer productId);

    List<ContainerProduct> findAllByCodeInAndProductId(List<String> codes, Integer productId);

    @Query("SELECT c FROM ContainerProduct c WHERE c.productId = :productId AND LOWER(c.code) = LOWER(:code)")
    List<ContainerProduct> findAllByProductIdAndCodeIgnoreCase(@Param("productId") Integer productId,
                                                                @Param("code") String code);

    @Query("SELECT c FROM ContainerProduct c WHERE c.productId = :productId AND c.code IS NULL AND LOWER(c.name) = LOWER(:name)")
    List<ContainerProduct> findAllByProductIdAndCodeIsNullAndNameIgnoreCase(@Param("productId") Integer productId,
                                                                            @Param("name") String name);

    @Query("SELECT c.id FROM ContainerProduct c WHERE c.productId = :productId AND c.deletedDate IS NULL")
    List<Integer> findContainerIdsByProductIdAndDeletedDateIsNull(Integer productId);

    @Modifying
    @Query("UPDATE ContainerProduct c SET c.deletedDate = :deletedDate WHERE c.productId = :productId AND c.deletedDate IS NULL")
    void markAllContainersAsDeleted(@Param("productId") Integer productId,
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

    @Query("SELECT c FROM ContainerProduct c WHERE c.productId = :productId AND LOWER(c.name) IN :containerNames AND c.deletedDate IS NULL")
    List<ContainerProduct> findAllByProductIdAndNameInIgnoreCaseAndDeletedDateIsNull(@Param("productId") Integer productId,
                                                                                     @Param("containerNames") List<String> containerNames);

    @Query("SELECT cp.id FROM ContainerProduct cp WHERE cp.product.id = :productId")
    List<Integer> findIdsByProductId(@Param("productId") Integer productId);

    @Modifying
    @Query("DELETE FROM ContainerProduct cp WHERE cp.id IN :ids")
    void deleteByIdIn(@Param("ids") List<Integer> ids);
}
