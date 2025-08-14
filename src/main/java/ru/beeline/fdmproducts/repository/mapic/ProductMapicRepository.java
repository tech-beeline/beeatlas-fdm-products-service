package ru.beeline.fdmproducts.repository.mapic;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.beeline.fdmproducts.domain.mapic.ProductMapic;

import java.util.Optional;

@Repository
public interface ProductMapicRepository extends JpaRepository<ProductMapic, Integer> {

    Optional<ProductMapic> findByCmdb(String cmdb);
}
