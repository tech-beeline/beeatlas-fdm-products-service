/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.beeline.fdmproducts.domain.UserProduct;

import java.util.List;

@Repository
public interface UserProductRepository extends JpaRepository<UserProduct, Long> {
    List<UserProduct> findAllByUserId(Integer userId);

    Boolean existsByUserIdAndProductId(Integer userId, Integer Id);

    List<UserProduct> findAllByProductId(Integer productId);

    @Modifying
    @Query("DELETE FROM UserProduct up WHERE up.product.id = :productId")
    void deleteByProductId(@Param("productId") Integer productId);
}
