package ru.beeline.fdmproducts.repository.mapic;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.beeline.fdmproducts.domain.mapic.ApiMapic;
import ru.beeline.fdmproducts.domain.mapic.CapabilityMapic;

import java.util.List;

public interface ApiMapicRepository extends JpaRepository<ApiMapic, Integer> {

    List<ApiMapic> findByCapabilityIdIn(List<CapabilityMapic> capabilityMapics);
}
