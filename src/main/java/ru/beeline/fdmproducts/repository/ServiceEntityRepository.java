package ru.beeline.fdmproducts.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.beeline.fdmproducts.domain.ServiceEntity;

public interface ServiceEntityRepository extends JpaRepository<ServiceEntity, Long> {

    ServiceEntity findByApiKey(String apiKey);
}
