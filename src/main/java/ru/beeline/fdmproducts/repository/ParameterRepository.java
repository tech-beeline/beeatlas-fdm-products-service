package ru.beeline.fdmproducts.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.beeline.fdmproducts.domain.Operation;
import ru.beeline.fdmproducts.domain.Parameter;

import java.util.Optional;

public interface ParameterRepository extends JpaRepository<Parameter, Integer> {

    Optional<Parameter>findBy
}
