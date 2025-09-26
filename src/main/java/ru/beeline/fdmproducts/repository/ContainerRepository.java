package ru.beeline.fdmproducts.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.beeline.fdmproducts.domain.ContainerProduct;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContainerRepository extends JpaRepository<ContainerProduct, Integer> {

    Optional<ContainerProduct> findByCode(String code);

    List<ContainerProduct> findAllByProductId(Integer productId);

    List<ContainerProduct> findAllByProductIdAndDeletedDateIsNull(Integer productId);

    List<ContainerProduct> findAllByCodeIn(List<String> codes);

    List<ContainerProduct> findAllByCodeInAndProductId(List<String> codes, Integer productId);

}
