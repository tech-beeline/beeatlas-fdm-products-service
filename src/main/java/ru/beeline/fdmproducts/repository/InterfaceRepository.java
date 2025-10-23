package ru.beeline.fdmproducts.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.beeline.fdmproducts.domain.Interface;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface InterfaceRepository extends JpaRepository<Interface, Integer> {

    Optional<Interface> findByCodeAndContainerId(String code, Integer containerId);

    List<Interface> findAllByContainerIdAndDeletedDateIsNull(Integer containerId);

    List<Interface> findAllByContainerIdIn(List<Integer> containerId);

    List<Interface> findAllByContainerIdInAndDeletedDateIsNull(List<Integer> containerId);

    List<Interface> findAllByContainerId(Integer containerId);

    List<Interface> findByCodeInAndContainerId(List<String> code, Integer containerId);

    List<Interface> findAllByContainerIdAndCodeIn(Integer containerId, List<String> interfaceCodes);

    @Modifying
    @Query("UPDATE Interface i SET i.deletedDate = :deletedDate WHERE i.containerId = :containerId AND i.code NOT IN :codes AND i.deletedDate IS NULL")
    void markInterfacesAsDeleted(@Param("containerId") Integer containerId,
                                 @Param("codes") List<String> codes,
                                 @Param("deletedDate") LocalDateTime deletedDate);

    List<Interface> findAllByContainerIdAndDeletedDate(Integer containerId,LocalDateTime now);
}
