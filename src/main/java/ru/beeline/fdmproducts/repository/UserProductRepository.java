package ru.beeline.fdmproducts.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.beeline.fdmproducts.domain.UserProduct;
import java.util.List;
public interface UserProductRepository extends JpaRepository<UserProduct, Long> {
    List<UserProduct> findAllByUserId(Integer userId);
}
