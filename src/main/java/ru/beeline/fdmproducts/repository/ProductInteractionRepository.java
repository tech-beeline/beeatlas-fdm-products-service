/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.beeline.fdmproducts.domain.ProductInteraction;

import java.util.List;

@Repository
public interface ProductInteractionRepository extends JpaRepository<ProductInteraction, Integer> {

    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM ProductInteraction p "
            + "WHERE LOWER(p.providerAlias) = LOWER(:providerAlias) "
            + "AND LOWER(p.consumerAlias) = LOWER(:consumerAlias)")
    boolean existsByProviderAndConsumerIgnoreCase(@Param("providerAlias") String providerAlias,
                                                  @Param("consumerAlias") String consumerAlias);

    @Query("SELECT DISTINCT p.providerAlias FROM ProductInteraction p "
            + "WHERE LOWER(p.consumerAlias) = LOWER(:alias) "
            + "ORDER BY p.providerAlias")
    List<String> findProviderAliasesByConsumerAliasIgnoreCase(@Param("alias") String alias);

    @Query("SELECT DISTINCT p.consumerAlias FROM ProductInteraction p "
            + "WHERE LOWER(p.providerAlias) = LOWER(:alias) "
            + "ORDER BY p.consumerAlias")
    List<String> findConsumerAliasesByProviderAliasIgnoreCase(@Param("alias") String alias);
}
