package ru.beeline.fdmproducts.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.beeline.fdmproducts.domain.Interface;

import java.util.Optional;

public interface InterfaceRepository extends JpaRepository<Interface, Integer> {

    Optional<Interface> findByCode(String code);
}
