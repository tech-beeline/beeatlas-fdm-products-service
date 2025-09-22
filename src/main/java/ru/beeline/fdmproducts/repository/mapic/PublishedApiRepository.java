package ru.beeline.fdmproducts.repository.mapic;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.beeline.fdmproducts.domain.mapic.ApiMapic;
import ru.beeline.fdmproducts.domain.mapic.PublishedApi;

import java.util.List;

public interface PublishedApiRepository extends JpaRepository<PublishedApi, Integer> {

    List<PublishedApi> findAllByApiIdIn(List<ApiMapic> apiList);
}
