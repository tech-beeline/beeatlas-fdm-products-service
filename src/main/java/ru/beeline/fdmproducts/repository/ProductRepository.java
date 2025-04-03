package ru.beeline.fdmproducts.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.beeline.fdmproducts.domain.Product;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    @Query("SELECT p FROM Product p WHERE LOWER(p.alias) = LOWER(:code)")
    Product findByAliasCaseInsensitive(@Param("code") String code);
    Product findByStructurizrApiKey(String apiKey);

    @Query("SELECT p FROM Product p JOIN p.techProducts tp WHERE tp.deletedDate IS NULL")
    List<Product> findAllWithTechProductNotDeleted();

    @Query("SELECT p.alias FROM Product p JOIN p.techProducts tp WHERE tp.deletedDate IS NULL")
    List<String> findAllAliasesWithTechProductNotDeleted();
}
