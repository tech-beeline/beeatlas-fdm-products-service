package ru.beeline.fdmproducts.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.beeline.fdmproducts.domain.LocalAcObjectDetail;

@Repository
public interface LocalAcObjectDetailRepository extends JpaRepository<LocalAcObjectDetail, Integer> {
}
