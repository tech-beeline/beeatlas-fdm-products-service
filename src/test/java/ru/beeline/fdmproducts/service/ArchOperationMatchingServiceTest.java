/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import ru.beeline.fdmproducts.domain.Interface;
import ru.beeline.fdmproducts.domain.ProductBranch;
import ru.beeline.fdmproducts.dto.search.projection.ArchOperationProjection;
import ru.beeline.fdmproducts.repository.ContainerRepository;
import ru.beeline.fdmproducts.repository.InterfaceRepository;
import ru.beeline.fdmproducts.repository.OperationRepository;
import ru.beeline.fdmproducts.repository.ProductBranchRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Общее правило сопоставления «метод ↔ архитектурная операция». */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ArchOperationMatchingServiceTest {

    private static final String ALIAS = "orders-product";

    @Mock
    private ProductBranchRepository productBranchRepository;

    @Mock
    private ContainerRepository containerRepository;

    @Mock
    private InterfaceRepository interfaceRepository;

    @Mock
    private OperationRepository operationRepository;

    private ArchOperationMatchingService service;

    @BeforeEach
    void setUp() {
        service = new ArchOperationMatchingService(new ProductBranchService(productBranchRepository),
                containerRepository, interfaceRepository, operationRepository);
    }

    private void givenMainBranch() {
        ProductBranch branch = ProductBranch.builder().id(7).alias(ALIAS).branchName("main").build();
        when(productBranchRepository.findAllByAliasIgnoreCaseAndBranchNameIgnoreCaseOrderByIdAsc(ALIAS, "main"))
                .thenReturn(List.of(branch));
    }

    private void givenChain(List<Integer> containerIds, List<Integer> interfaceIds) {
        givenMainBranch();
        when(containerRepository.findContainerIdsByProductBranchIdAndDeletedDateIsNull(7)).thenReturn(containerIds);
        when(interfaceRepository.findAllByContainerIdInAndDeletedDateIsNull(containerIds))
                .thenReturn(interfaceIds.stream().map(id -> Interface.builder().id(id).build()).toList());
    }

    @Test
    @DisplayName("Область поиска: ветка main -> живые контейнеры -> живые интерфейсы")
    void resolvesInterfaceIdsThroughMainBranch() {
        givenChain(List.of(11, 12), List.of(101, 102));

        assertThat(service.resolveInterfaceIds(ALIAS)).containsExactly(101, 102);
    }

    @Test
    @DisplayName("Нет ветки main — искать негде, в БД за контейнерами не ходим")
    void returnsEmptyWhenNoMainBranch() {
        when(productBranchRepository.findAllByAliasIgnoreCaseAndBranchNameIgnoreCaseOrderByIdAsc(ALIAS, "main"))
                .thenReturn(List.of());

        assertThat(service.resolveInterfaceIds(ALIAS)).isEmpty();
        verifyNoInteractions(containerRepository, interfaceRepository);
    }

    @Test
    @DisplayName("Ветка main, записанная в другом регистре, всё равно находится")
    void findsMainBranchRegardlessOfCase() {
        ProductBranch branch = ProductBranch.builder().id(7).alias(ALIAS).branchName("Main").build();
        when(productBranchRepository.findAllByAliasIgnoreCaseAndBranchNameIgnoreCaseOrderByIdAsc(ALIAS, "main"))
                .thenReturn(List.of(branch));
        when(containerRepository.findContainerIdsByProductBranchIdAndDeletedDateIsNull(7)).thenReturn(List.of(11));
        when(interfaceRepository.findAllByContainerIdInAndDeletedDateIsNull(List.of(11)))
                .thenReturn(List.of(Interface.builder().id(101).build()));

        assertThat(service.resolveInterfaceIds(ALIAS)).containsExactly(101);
    }

    @Test
    @DisplayName("Нет живых контейнеров — за интерфейсами не ходим")
    void returnsEmptyWhenNoContainers() {
        givenMainBranch();
        when(containerRepository.findContainerIdsByProductBranchIdAndDeletedDateIsNull(7)).thenReturn(List.of());

        assertThat(service.resolveInterfaceIds(ALIAS)).isEmpty();
        verifyNoInteractions(interfaceRepository);
    }

    @Test
    @DisplayName("Пустая область поиска не доходит до запроса: IN () — синтаксическая ошибка")
    void doesNotQueryWithEmptyInterfaceIds() {
        assertThat(service.findMatches("GET /orders", "GET", "REST", List.of())).isEmpty();
        verifyNoInteractions(operationRepository);
    }

    @Test
    @DisplayName("Незаполненное имя метода не ищется")
    void doesNotQueryWithBlankName() {
        assertThat(service.findMatches("  ", "GET", null, List.of(101))).isEmpty();
        verifyNoInteractions(operationRepository);
    }

    @Test
    @DisplayName("Совпадения отдаются как есть, фильтры передаются в запрос без изменений")
    void passesFiltersToQuery() {
        ArchOperationProjection matching = projection(5, "/api/v1/orders/{id}");
        ArchOperationProjection other = projection(6, "/api/v1/clients");
        when(operationRepository.findArchOperationsForMatching("GET", "REST", List.of(101)))
                .thenReturn(List.of(matching, other));

        assertThat(service.findMatches("/api/v1/orders/42", "GET", "REST", List.of(101)))
                .containsExactly(matching);
    }

    @Test
    @DisplayName("Автомат берёт первое совпадение и не фильтрует по протоколу")
    void firstMatchIgnoresProtocol() {
        List<ArchOperationProjection> matches = List.of(projection(5, "createOrder"), projection(6, "createOrder"));
        when(operationRepository.findArchOperationsForMatching(eq("SOAP"), eq(null), anyList()))
                .thenReturn(matches);

        Optional<ArchOperationProjection> match = service.findFirstMatch("createOrder", "SOAP", List.of(101));

        assertThat(match).isPresent();
        assertThat(match.get().getOpId()).isEqualTo(5);
    }

    @Test
    @DisplayName("Операция без типа автоматически не сопоставляется — как и прежде при o.type ILIKE NULL")
    void firstMatchRequiresType() {
        assertThat(service.findFirstMatch("createOrder", null, List.of(101))).isEmpty();
        assertThat(service.findFirstMatch("createOrder", " ", List.of(101))).isEmpty();
        verify(operationRepository, never()).findArchOperationsForMatching(any(), any(), anyList());
    }

    private ArchOperationProjection projection(int opId, String name) {
        ArchOperationProjection projection = org.mockito.Mockito.mock(ArchOperationProjection.class);
        when(projection.getOpId()).thenReturn(opId);
        when(projection.getOpName()).thenReturn(name);
        return projection;
    }
}
