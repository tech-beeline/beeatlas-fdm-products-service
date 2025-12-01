package ru.beeline.fdmproducts.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.beeline.fdmproducts.domain.Infra;
import ru.beeline.fdmproducts.domain.Property;

import java.util.List;
import java.util.Optional;

@Repository
public interface PropertyRepository extends JpaRepository<Property, Integer> {
    @Query("SELECT i FROM Property i WHERE LOWER(i.name) LIKE LOWER(:name) AND LOWER(i.value) LIKE LOWER(:value)")
    List<Property> findByNameAndValue(@Param("name") String name, @Param("value") String value);

    List<Property> findByInfraIdIn(List<Integer> infraIds);
}
