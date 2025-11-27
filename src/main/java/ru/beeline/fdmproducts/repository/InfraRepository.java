package ru.beeline.fdmproducts.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.beeline.fdmproducts.domain.Infra;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface InfraRepository extends JpaRepository<Infra, Integer> {

    @Query("select i.cmdbId from Infra i where i.cmdbId in (:cmdbIds)")
    List<String> findCmdbIdByCmdbIdIn(@Param("cmdbIds") Collection<String> cmdbIds);

    @Query("SELECT i FROM Infra i WHERE LOWER(i.name) = LOWER(:name) AND i.deletedDate IS NULL")
    Optional<Infra> findByNameCaseInsensitiveAndNotDeleted(@Param("name") String name);
    List<Infra> findByCmdbIdIn(Collection<String> cmdbIds);

    @Query("select i from Infra i join fetch i.infraProducts ip where ip.product.id = :productId and ip.deletedDate is null")
    List<Infra> findInfrasByProductId(@Param("productId") Integer productId);
}
