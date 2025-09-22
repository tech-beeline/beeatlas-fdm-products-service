package ru.beeline.fdmproducts.repository.mapic;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.beeline.fdmproducts.domain.mapic.CapabilityMapic;
import ru.beeline.fdmproducts.domain.mapic.ProductMapic;

import java.util.List;

public interface CapabilityMapicRepository extends JpaRepository<CapabilityMapic, Integer> {

    List<CapabilityMapic> findByProductId(ProductMapic product);
}
