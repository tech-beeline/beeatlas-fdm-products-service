package ru.beeline.fdmproducts.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.beeline.fdmproducts.domain.Interface;

import java.util.List;
import java.util.Optional;

@Repository
public interface InterfaceRepository extends JpaRepository<Interface, Integer> {

    Optional<Interface> findByCodeAndContainerId(String code, Integer containerId);

    List<Interface> findAllByContainerIdAndDeletedDateIsNull(Integer containerId);

    List<Interface> findAllByContainerIdIn(List<Integer> containerId);

    List<Interface> findAllByContainerId(Integer containerId);

    List<Interface> findByCodeInAndContainerId(List<String> code, Integer containerId);

    List<Interface> findAllByContainerIdAndCodeIn(Integer containerId, List<String> interfaceCodes);


}
