package ru.beeline.fdmproducts.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.beeline.fdmproducts.domain.Infra;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface InfraRepository extends JpaRepository<Infra, Integer> {

    Optional<Infra> findByCmdbId(String cmdbId);

    @Query("select i.cmdbId from Infra i where i.cmdbId in :cmdbIds")
    List<String> findCmdbIdByCmdbIdIn(@Param("cmdbIds") Collection<String> cmdbIds);

    List<Infra> findByCmdbIdIn(Collection<String> cmdbIds);


    @Query("SELECT ip.infra FROM InfraProduct ip WHERE ip.product.id = :productId AND ip.deletedDate IS NULL")
    List<Infra> findInfrasByProductId(@Param("productId") Integer productId);
}
