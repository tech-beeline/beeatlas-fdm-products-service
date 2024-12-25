package ru.beeline.fdmproducts.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.beeline.fdmproducts.domain.Interface;

import java.util.List;
import java.util.Optional;

public interface InterfaceRepository extends JpaRepository<Interface, Integer> {

    Optional<Interface> findByCodeAndContainerId(String code, Integer containerId);

    List<Interface> findByContainerIdAndDeletedDateIsNull(Integer containerId);
}
