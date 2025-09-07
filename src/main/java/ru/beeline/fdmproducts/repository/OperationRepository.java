package ru.beeline.fdmproducts.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.beeline.fdmproducts.domain.Operation;

import java.util.List;
import java.util.Optional;

@Repository
public interface OperationRepository extends JpaRepository<Operation, Integer> {

    Optional<Operation> findByNameAndInterfaceId(String name, Integer interfaceId);

    List<Operation> findByInterfaceIdAndDeletedDateIsNull(Integer id);

    Optional<Operation> findByNameAndTypeAndInterfaceIdIn(String name, String type, List<Integer> ids);

    List<Operation> findAllByInterfaceId(Integer interfaceId);

    List<Operation> findByNameInAndInterfaceId (List<String> names, Integer interfaceId);

}
