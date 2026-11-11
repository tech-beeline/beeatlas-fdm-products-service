/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.beeline.fdmproducts.domain.DiscoveredInterface;
import ru.beeline.fdmproducts.domain.Product;

import javax.transaction.Transactional;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface DiscoveredInterfaceRepository extends JpaRepository<DiscoveredInterface, Integer> {

    /**
     * Атрибут связи discovered-интерфейса ветки {@code :branch} — main, develop или design, как
     * {@link ru.beeline.fdmproducts.domain.DiscoveredConnectionBranch#getBranchName()}.
     */
    String BRANCH_CONNECTION_INTERFACE = "CASE :branch "
            + "WHEN 'main' THEN connection_interface_id "
            + "WHEN 'develop' THEN connection_interface_dev_id "
            + "WHEN 'design' THEN connection_interface_design_id END";

    /** SET: обнуляет атрибут интерфейса ветки {@code :branch}, атрибуты двух других веток не меняются. */
    String CLEAR_BRANCH_CONNECTION_INTERFACE = "connection_interface_id = "
            + "CASE WHEN :branch = 'main' THEN NULL ELSE connection_interface_id END, "
            + "connection_interface_dev_id = "
            + "CASE WHEN :branch = 'develop' THEN NULL ELSE connection_interface_dev_id END, "
            + "connection_interface_design_id = "
            + "CASE WHEN :branch = 'design' THEN NULL ELSE connection_interface_design_id END";

    List<DiscoveredInterface> findByExternalIdIn(List<String> externalIds);

    /**
     * Снимает у остальных discovered-интерфейсов связь ветки {@code branch} (main, develop, design) с
     * archInterfaceId; атрибуты двух других веток не меняются.
     */
    @Modifying
    @Transactional
    @Query(value = "UPDATE product.discovered_interface SET " + CLEAR_BRANCH_CONNECTION_INTERFACE + " "
            + "WHERE id <> :mapicInterfaceId AND " + BRANCH_CONNECTION_INTERFACE + " = :archInterfaceId",
            nativeQuery = true)
    int clearConnectionInterfaceIdExcept(@Param("archInterfaceId") Integer archInterfaceId,
                                         @Param("mapicInterfaceId") Integer mapicInterfaceId,
                                         @Param("branch") String branch);

    /** Снимает связь ветки {@code branch} с интерфейсами контейнера entityId; атрибуты других веток остаются. */
    @Modifying
    @Transactional
    @Query(value = "UPDATE product.discovered_interface SET " + CLEAR_BRANCH_CONNECTION_INTERFACE + " " +
            "WHERE " + BRANCH_CONNECTION_INTERFACE + " IN (SELECT id FROM product.interface WHERE container_id = :entityId)",
            nativeQuery = true)
    int clearConnectionInterfaceIdByEntityId(@Param("entityId") Integer entityId, @Param("branch") String branch);

    /** Снимает связь ветки {@code branch} с интерфейсом interfaceId; атрибуты других веток остаются. */
    @Modifying
    @Transactional
    @Query(value = "UPDATE product.discovered_interface SET " + CLEAR_BRANCH_CONNECTION_INTERFACE + " " +
            "WHERE " + BRANCH_CONNECTION_INTERFACE + " = :interfaceId", nativeQuery = true)
    int clearConnectionInterfaceIdByInterfaceId(@Param("interfaceId") Integer interfaceId, @Param("branch") String branch);

    /**
     * Снимает связь ветки {@code branch} у интерфейсов, среди методов которых атрибут операции этой ветки
     * равен operationId; атрибуты других веток остаются. Колонки connection_operation_* в подзапросе
     * принадлежат discovered_operation o.
     */
    @Modifying
    @Transactional
    @Query(value = "UPDATE product.discovered_interface SET " + CLEAR_BRANCH_CONNECTION_INTERFACE + " " +
            "WHERE id IN (SELECT o.interface_id FROM product.discovered_operation o " +
            "WHERE " + DiscoveredOperationRepository.BRANCH_CONNECTION_OPERATION + " = :operationId)",
            nativeQuery = true)
    int clearConnectionInterfaceIdByOperationId(@Param("operationId") Integer operationId, @Param("branch") String branch);


    /**
     * Discovered-интерфейсы продукта для сопоставления с операцией arch-интерфейса: атрибут интерфейса
     * ветки {@code branch} пуст или уже равен этому интерфейсу (COALESCE — то же «IS NULL OR = :archInterfaceId»).
     */
    @Query(value = "SELECT * FROM product.discovered_interface "
            + "WHERE product_id = :productId "
            + "AND COALESCE(" + BRANCH_CONNECTION_INTERFACE + ", :archInterfaceId) = :archInterfaceId",
            nativeQuery = true)
    List<DiscoveredInterface> findAllForArchOperationComparison(@Param("productId") Integer productId,
                                                                @Param("archInterfaceId") Integer archInterfaceId,
                                                                @Param("branch") String branch);

    List<DiscoveredInterface> findAllByConnectionInterfaceIdIn(List<Integer> interfaceIds);

    List<DiscoveredInterface> findAllByProductAndSourceIgnoreCase(Product product, String source);

    List<DiscoveredInterface> findAllByProductAndSourceIgnoreCaseAndDeletedDateIsNull(Product product, String source);

    List<DiscoveredInterface> findAllByProductAndDeletedDateIsNull(Product product);

    Optional<DiscoveredInterface> findByExternalId(String externalId);

    Optional<DiscoveredInterface> findByApiId(Integer apiId);

    Optional<DiscoveredInterface> findBySourceIgnoreCaseAndProductIdAndExternalIdIgnoreCase(String source,
                                                                                            Integer productId,
                                                                                            String externalId);

    List<DiscoveredInterface> findAllBySourceIgnoreCaseAndExternalIdIgnoreCase(String source, String externalId);

    @Query("""
            SELECT di FROM DiscoveredInterface di
            LEFT JOIN FETCH di.product
            WHERE LOWER(di.source) = LOWER(:source)
              AND LOWER(di.externalId) IN :codes
            """)
    List<DiscoveredInterface> findAllBySourceAndExternalIdInIgnoreCase(@Param("source") String source,
                                                                       @Param("codes") Collection<String> codes);

    @EntityGraph(attributePaths = {"product"})
    List<DiscoveredInterface> findAllByIdInAndDeletedDateIsNull(List<Integer> ids);

    @Query("SELECT di.id FROM DiscoveredInterface di WHERE di.connectionInterfaceId IN :interfaceIds "
            + "OR di.connectionInterfaceDevId IN :interfaceIds "
            + "OR di.connectionInterfaceDesignId IN :interfaceIds")
    List<Integer> findIdsByInterfaceIds(@Param("interfaceIds") List<Integer> interfaceIds);

    @Modifying
    @Query("DELETE FROM DiscoveredInterface di WHERE di.id IN :ids")
    void deleteByIdIn(@Param("ids") List<Integer> ids);

    @Query(value = "SELECT DISTINCT LOWER(source) FROM product.discovered_interface "
            + "WHERE source IS NOT NULL ORDER BY LOWER(source)",
            nativeQuery = true)
    List<String> findDistinctSources();

    @Query("""
            SELECT di FROM DiscoveredInterface di
            WHERE di.product.id = :productId
              AND di.deletedDate IS NULL
              AND LOWER(di.source) IN :sources
            """)
    List<DiscoveredInterface> findActiveByProductIdAndSourceInIgnoreCase(
            @Param("productId") Integer productId,
            @Param("sources") Collection<String> sources);
}
