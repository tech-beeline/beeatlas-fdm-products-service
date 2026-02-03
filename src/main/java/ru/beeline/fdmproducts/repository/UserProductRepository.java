/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.beeline.fdmproducts.domain.UserProduct;

import java.util.List;

@Repository
public interface UserProductRepository extends JpaRepository<UserProduct, Long> {
    List<UserProduct> findAllByUserId(Integer userId);

    Boolean existsByUserIdAndProductId(Integer userId, Integer Id);

    List<UserProduct> findAllByProductId(Integer productId);
}
