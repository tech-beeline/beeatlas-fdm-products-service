package ru.beeline.fdmproducts.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.beeline.fdmproducts.domain.Operation;

import java.util.List;
import java.util.Optional;

public interface OperationRepository extends JpaRepository<Operation, Integer> {

    Optional<Operation> findByName(String name);

    List<Operation> findByInterfaceIdAndDeletedDateIsNull(Integer id);
}
