package ru.beeline.fdmproducts.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.beeline.fdmproducts.domain.ContainerProduct;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContainerRepository extends JpaRepository<ContainerProduct, Integer> {

    List<ContainerProduct> findAllByProductId(Integer productId);

    List<ContainerProduct> findAllByProductIdAndDeletedDateIsNull(Integer productId);

    List<ContainerProduct> findAllByCodeInAndProductId(List<String> codes, Integer productId);

}
