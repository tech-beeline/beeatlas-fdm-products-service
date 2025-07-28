package ru.beeline.fdmproducts.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.beeline.fdmproducts.domain.Product;
import ru.beeline.fdmproducts.domain.TechProduct;
import java.util.List;
@Repository
public interface TechProductRepository extends JpaRepository<TechProduct, Long> {
    List<TechProduct> findAllByTechId(Integer techId);
    TechProduct findByTechIdAndProduct(Integer techId, Product product);
    void deleteByTechIdAndProductId(Integer techId, Integer productId);
}
