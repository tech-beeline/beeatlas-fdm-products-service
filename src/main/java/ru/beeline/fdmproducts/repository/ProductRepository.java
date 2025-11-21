package ru.beeline.fdmproducts.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.beeline.fdmproducts.domain.ContainerProduct;
import ru.beeline.fdmproducts.domain.Product;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Integer> {
    @Query("SELECT p.alias FROM Product p WHERE p.id IN :ids")
    List<String> findAliasesByIds(@Param("ids") List<Integer> ids);

    @Query(value = "SELECT * FROM product.product p WHERE p.alias ILIKE :code", nativeQuery = true)
    Product findByAliasCaseInsensitive(@Param("code") String code);

    Product findByStructurizrApiKey(String apiKey);

    @Query("SELECT DISTINCT p FROM Product p JOIN p.techProducts tp WHERE tp.deletedDate IS NULL")
    List<Product> findAllWithTechProductNotDeleted();

    @Query("SELECT p.alias FROM Product p")
    List<String> findAllAliases();

    @Query("SELECT p FROM Product p WHERE LOWER(p.alias) IN :lowerAliases")
    List<Product> findByAliasInIgnoreCase(@Param("lowerAliases") List<String> lowerAliases);

    @Query(value = "SELECT p.*"
            + "FROM product.product p "
            + "JOIN product.containers_product cp ON cp.product_id = p.id "
            + "WHERE cp.id = :id", nativeQuery = true)
    Optional<Product> findProductByContainerProductID(@Param("id") Integer id);

    @Query(value = "SELECT p.*"
            + "FROM product.product p "
            + "JOIN product.containers_product cp ON cp.product_id = p.id "
            + "JOIN product.interface i ON cp.id = i.container_id "
            + "WHERE i.id = :id", nativeQuery = true)
    Optional<Product> findProductByInterfaceId(@Param("id") Integer id);

    @Query(value = "SELECT p.*"
            + "FROM product.product p "
            + "JOIN product.containers_product cp ON cp.product_id = p.id "
            + "JOIN product.interface i ON cp.id = i.container_id "
            + "JOIN product.operation o ON o.interface_id = i.id "
            + "WHERE o.id = :id", nativeQuery = true)
    Optional<Product> findProductByOperationID(@Param("id") Integer id);

}
