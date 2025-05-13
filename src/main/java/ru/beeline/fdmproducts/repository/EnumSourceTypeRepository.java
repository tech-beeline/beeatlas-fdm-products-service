package ru.beeline.fdmproducts.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.beeline.fdmproducts.domain.EnumSourceType;

import java.util.Optional;

public interface EnumSourceTypeRepository extends JpaRepository<EnumSourceType,Integer> {

    Optional<EnumSourceType> findByName(String name);
}
