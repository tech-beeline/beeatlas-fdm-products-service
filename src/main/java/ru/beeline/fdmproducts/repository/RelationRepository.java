package ru.beeline.fdmproducts.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.beeline.fdmproducts.domain.Relation;

import java.util.List;

@Repository
public interface RelationRepository extends JpaRepository<Relation, Integer> {
    List<Relation> findByParentId(String parentId);
    List<Relation> findByParentIdIn(List<String> parentId);

}
