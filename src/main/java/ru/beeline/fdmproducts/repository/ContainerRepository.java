/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.repository;

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

    List<ContainerProduct> findAllByProductId(Integer productId);

    List<ContainerProduct> findAllByProductIdAndDeletedDateIsNull(Integer productId);

    List<ContainerProduct> findAllByCodeInAndProductId(List<String> codes, Integer productId);

    List<ContainerProduct> findAllByProductIdAndDeletedDateIsNotNull(Integer productId);

    @Query("SELECT c.id FROM ContainerProduct c WHERE c.productId = :productId AND c.deletedDate IS NULL")
    List<Integer> findContainerIdsByProductIdAndDeletedDateIsNull(Integer productId);

    @Modifying
    @Query("UPDATE ContainerProduct c SET c.deletedDate = :deletedDate WHERE c.productId = :productId AND c.code NOT IN :codes AND c.deletedDate IS NULL")
    void markContainersAsDeleted(@Param("productId") Integer productId,
                                 @Param("codes") List<String> codes,
                                 @Param("deletedDate") Date deletedDate);

    @Modifying
    @Query("UPDATE ContainerProduct c SET c.deletedDate = :deletedDate WHERE c.productId = :productId AND c.deletedDate IS NULL")
    void markAllContainersAsDeleted(@Param("productId") Integer productId,
                                    @Param("deletedDate") Date deletedDate);

    List<ContainerProduct> findAllBySourceMetricIsNotNullAndDeletedDateIsNull();

    @Modifying
    @Query("UPDATE ContainerProduct c SET c.sourceMetric = :sourceMetric WHERE c.id = :id")
    void updateSourceMetricById(@Param("id") Integer id,
                                @Param("sourceMetric") String sourceMetric);

    List<ContainerProduct> findAllByProductIdAndNameInAndDeletedDateIsNull(Integer productId,
                                                                           List<String> containerName);
}
