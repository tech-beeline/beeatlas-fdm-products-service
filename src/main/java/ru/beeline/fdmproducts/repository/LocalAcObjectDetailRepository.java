/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.beeline.fdmproducts.domain.LocalAcObjectDetail;

import java.util.List;

@Repository
public interface LocalAcObjectDetailRepository extends JpaRepository<LocalAcObjectDetail, Integer> {

    @Modifying
    @Query("DELETE FROM LocalAcObjectDetail l WHERE l.lacoId IN :ids")
    void deleteByLacoIdIn(@Param("ids") List<Integer> ids);
}
