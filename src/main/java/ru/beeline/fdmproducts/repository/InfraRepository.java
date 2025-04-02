package ru.beeline.fdmproducts.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.beeline.fdmproducts.domain.Infra;

import java.util.List;
import java.util.Optional;

@Repository
public interface InfraRepository extends JpaRepository<Infra, Integer> {
    List<Infra> findByProductId(Integer productId);

    Optional<Infra> findByCmdbId(String cmdbId);
}
