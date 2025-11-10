package ru.beeline.fdmproducts.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.beeline.fdmproducts.domain.LocalAcObject;

import java.util.List;

@Repository
public interface LocalAcObjectRepository extends JpaRepository<LocalAcObject, Integer> {

    List<LocalAcObject> findAllByLacId(Integer lacId);
}
