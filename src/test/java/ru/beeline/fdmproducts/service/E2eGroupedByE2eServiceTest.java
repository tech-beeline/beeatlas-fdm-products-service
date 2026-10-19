/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.beeline.fdmproducts.client.CapabilityClient;
import ru.beeline.fdmproducts.domain.DiscoveredInterface;
import ru.beeline.fdmproducts.domain.DiscoveredOperation;
import ru.beeline.fdmproducts.domain.E2e;
import ru.beeline.fdmproducts.domain.Operation;
import ru.beeline.fdmproducts.domain.OperationRelation;
import ru.beeline.fdmproducts.domain.Product;
import ru.beeline.fdmproducts.dto.TcDTO;
import ru.beeline.fdmproducts.dto.e2e.E2eGroupedByE2eDTO;
import ru.beeline.fdmproducts.dto.e2e.E2eRelationParticipation;
import ru.beeline.fdmproducts.exception.EntityNotFoundException;
import ru.beeline.fdmproducts.exception.ExternalServiceException;
import ru.beeline.fdmproducts.mapper.E2eGroupedByE2eMapper;
import ru.beeline.fdmproducts.repository.DiscoveredInterfaceRepository;
import ru.beeline.fdmproducts.repository.DiscoveredOperationRepository;
import ru.beeline.fdmproducts.repository.E2eRepository;
import ru.beeline.fdmproducts.repository.OperationRelationRepository;
import ru.beeline.fdmproducts.repository.OperationRepository;
import ru.beeline.fdmproducts.repository.ProductRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_GATEWAY;

@ExtendWith(MockitoExtension.class)
class E2eGroupedByE2eServiceTest {

    private static final Integer PRODUCT_ID = 1;
    private static final String ALIAS = "orders-product";

    @Mock
    private ProductRepository productRepository;
    @Mock
    private E2eRepository e2eRepository;
    @Mock
    private DiscoveredInterfaceRepository discoveredInterfaceRepository;
    @Mock
    private DiscoveredOperationRepository discoveredOperationRepository;
    @Mock
    private OperationRelationRepository operationRelationRepository;
    @Mock
    private OperationRepository operationRepository;
    @Mock
    private CapabilityClient capabilityClient;
    @Spy
    private E2eGroupedByE2eMapper mapper = new E2eGroupedByE2eMapper();
    @Mock
    private E2eOwnerService e2eOwnerService;

    @InjectMocks
    private E2eGroupedByE2eService service;

    @Test
    @DisplayName("SPARX и sparx — один источник; интерфейс sparx берётся")
    void caseInsensitiveSourcesTakeMatchingInterface() {
        stubProductById();
        when(e2eRepository.findAllSources()).thenReturn(List.of("SPARX", "sparx", "  "));
        DiscoveredInterface iface = interfaceRow(5, "sparx");
        when(discoveredInterfaceRepository.findActiveByProductIdAndSourceInIgnoreCase(eq(PRODUCT_ID), eq(Set.of("sparx"))))
                .thenReturn(List.of(iface));
        DiscoveredOperation method = discoveredRow(20, 5, null);
        when(discoveredOperationRepository.findAllByInterfaceIdInAndDeletedDateIsNull(List.of(5)))
                .thenReturn(List.of(method));
        when(operationRelationRepository.findDiscoveredOperationParticipations(List.of(20), "main"))
                .thenReturn(List.of(relation(1, 20, null)));
        when(e2eRepository.findAllByIdIn(Set.of(1))).thenReturn(List.of(e2e(1, "SPARX")));

        List<E2eGroupedByE2eDTO> result = service.findGroupedByE2e("1", null, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSource()).isEqualTo("SPARX");
        assertThat(result.get(0).getOperations()).extracting(op -> op.getId()).containsExactly(20);
        verify(capabilityClient, never()).getTcs(anyList());
    }

    @Test
    @DisplayName("Интерфейс UseCase не берётся, если такого source нет у e2e")
    void useCaseInterfaceExcludedWhenNoSuchE2eSource() {
        stubProductById();
        when(e2eRepository.findAllSources()).thenReturn(List.of("SPARX"));
        when(discoveredInterfaceRepository.findActiveByProductIdAndSourceInIgnoreCase(eq(PRODUCT_ID), eq(Set.of("sparx"))))
                .thenReturn(List.of());

        assertThat(service.findGroupedByE2e("1", null, null)).isEmpty();
        verify(discoveredOperationRepository, never()).findAllByInterfaceIdInAndDeletedDateIsNull(anyList());
        verify(capabilityClient, never()).getTcs(anyList());
    }

    @Test
    @DisplayName("Интерфейс другого продукта не берётся")
    void otherProductInterfaceNotSelected() {
        stubProductById();
        when(e2eRepository.findAllSources()).thenReturn(List.of("SPARX"));
        when(discoveredInterfaceRepository.findActiveByProductIdAndSourceInIgnoreCase(eq(PRODUCT_ID), eq(Set.of("sparx"))))
                .thenReturn(List.of());

        assertThat(service.findGroupedByE2e("1", null, null)).isEmpty();
        verify(discoveredInterfaceRepository).findActiveByProductIdAndSourceInIgnoreCase(PRODUCT_ID, Set.of("sparx"));
    }

    @Test
    @DisplayName("Удалённые интерфейс и метод не берутся")
    void deletedInterfaceAndMethodExcluded() {
        stubProductById();
        when(e2eRepository.findAllSources()).thenReturn(List.of("SPARX"));
        when(discoveredInterfaceRepository.findActiveByProductIdAndSourceInIgnoreCase(eq(PRODUCT_ID), eq(Set.of("sparx"))))
                .thenReturn(List.of());

        assertThat(service.findGroupedByE2e("1", null, null)).isEmpty();

        when(discoveredInterfaceRepository.findActiveByProductIdAndSourceInIgnoreCase(eq(PRODUCT_ID), eq(Set.of("sparx"))))
                .thenReturn(List.of(interfaceRow(5, "SPARX")));
        when(discoveredOperationRepository.findAllByInterfaceIdInAndDeletedDateIsNull(List.of(5)))
                .thenReturn(List.of());

        assertThat(service.findGroupedByE2e("1", null, null)).isEmpty();
        verify(capabilityClient, never()).getTcs(anyList());
    }

    @Test
    @DisplayName("Метод на обеих сторонах одного e2e — один раз")
    void methodOnBothSidesAppearsOnce() {
        stubProductById();
        stubHappyPathWithoutArch(20);
        when(operationRelationRepository.findDiscoveredOperationParticipations(List.of(20), "main"))
                .thenReturn(List.of(relation(1, 20, 20)));
        when(e2eRepository.findAllByIdIn(Set.of(1))).thenReturn(List.of(e2e(1, "SPARX")));

        List<E2eGroupedByE2eDTO> result = service.findGroupedByE2e("1", null, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getOperations()).hasSize(1);
        assertThat(result.get(0).getOperations().get(0).getId()).isEqualTo(20);
    }

    @Test
    @DisplayName("Один метод в двух e2e")
    void sameMethodInTwoE2e() {
        stubProductById();
        stubHappyPathWithoutArch(20);
        when(operationRelationRepository.findDiscoveredOperationParticipations(List.of(20), "main"))
                .thenReturn(List.of(
                        relation(2, 20, null),
                        relation(1, null, 20)));
        when(e2eRepository.findAllByIdIn(Set.of(1, 2)))
                .thenReturn(List.of(e2e(1, "SPARX"), e2e(2, "sparx")));

        List<E2eGroupedByE2eDTO> result = service.findGroupedByE2e("1", null, null);

        assertThat(result).extracting(E2eGroupedByE2eDTO::getId).containsExactly(1, 2);
        assertThat(result.get(0).getOperations()).extracting(op -> op.getId()).containsExactly(20);
        assertThat(result.get(1).getOperations()).extracting(op -> op.getId()).containsExactly(20);
        assertThat(result.get(0).getSource()).isEqualTo("SPARX");
        assertThat(result.get(1).getSource()).isEqualTo("sparx");
    }

    @Test
    @DisplayName("Нет участий в связях — пустой ответ без чтения e2e")
    void operationEntityTypeDoesNotParticipate() {
        stubProductById();
        stubHappyPathWithoutArch(20);
        when(operationRelationRepository.findDiscoveredOperationParticipations(List.of(20), "main"))
                .thenReturn(List.of());

        assertThat(service.findGroupedByE2e("1", null, null)).isEmpty();
        verify(e2eRepository, never()).findAllByIdIn(any());
        verify(capabilityClient, never()).getTcs(anyList());
    }

    @Test
    @DisplayName("Метод без шага не попадает в ответ и не обогащается")
    void methodWithoutStepExcluded() {
        stubProductById();
        when(e2eRepository.findAllSources()).thenReturn(List.of("SPARX"));
        when(discoveredInterfaceRepository.findActiveByProductIdAndSourceInIgnoreCase(eq(PRODUCT_ID), eq(Set.of("sparx"))))
                .thenReturn(List.of(interfaceRow(5, "SPARX")));
        DiscoveredOperation participating = discoveredRow(20, 5, 100);
        DiscoveredOperation orphan = discoveredRow(30, 5, 200);
        when(discoveredOperationRepository.findAllByInterfaceIdInAndDeletedDateIsNull(List.of(5)))
                .thenReturn(List.of(participating, orphan));
        when(operationRelationRepository.findDiscoveredOperationParticipations(List.of(20, 30), "main"))
                .thenReturn(List.of(relation(1, 20, null)));
        when(e2eRepository.findAllByIdIn(Set.of(1))).thenReturn(List.of(e2e(1, "SPARX")));
        when(operationRepository.findAllByIdIn(List.of(100))).thenReturn(List.of(operationRow(100, null)));

        List<E2eGroupedByE2eDTO> result = service.findGroupedByE2e("1", null, null);

        assertThat(result.get(0).getOperations()).extracting(op -> op.getId()).containsExactly(20);
        verify(operationRepository).findAllByIdIn(List.of(100));
        verify(capabilityClient, never()).getTcs(anyList());
    }

    @Test
    @DisplayName("Обогащение archOperation и techCapability")
    void enrichesArchOperationAndTechCapability() {
        stubProductById();
        when(e2eRepository.findAllSources()).thenReturn(List.of("SPARX"));
        when(discoveredInterfaceRepository.findActiveByProductIdAndSourceInIgnoreCase(eq(PRODUCT_ID), eq(Set.of("sparx"))))
                .thenReturn(List.of(interfaceRow(5, "SPARX")));
        when(discoveredOperationRepository.findAllByInterfaceIdInAndDeletedDateIsNull(List.of(5)))
                .thenReturn(List.of(discoveredRow(20, 5, 100)));
        when(operationRelationRepository.findDiscoveredOperationParticipations(List.of(20), "main"))
                .thenReturn(List.of(relation(1, 20, null)));
        when(e2eRepository.findAllByIdIn(Set.of(1))).thenReturn(List.of(e2e(1, "SPARX")));
        Operation arch = operationRow(100, 3);
        when(operationRepository.findAllByIdIn(List.of(100))).thenReturn(List.of(arch));
        TcDTO tc = TcDTO.builder().id(3).code("TC.01").name("Оформление заказа").build();
        when(capabilityClient.getTcs(List.of(3))).thenReturn(List.of(tc));

        List<E2eGroupedByE2eDTO> result = service.findGroupedByE2e("1", null, null);

        assertThat(result.get(0).getOperations().get(0).getArchOperation()).isNotNull();
        assertThat(result.get(0).getOperations().get(0).getArchOperation().getId()).isEqualTo(100);
        assertThat(result.get(0).getOperations().get(0).getArchOperation().getTechCapability().getId()).isEqualTo(3);
    }

    @Test
    @DisplayName("connection_operation_id пустой или operation нет — archOperation = null")
    void archOperationNullWhenNoConnection() {
        stubProductById();
        stubHappyPathWithoutArch(20);
        when(operationRelationRepository.findDiscoveredOperationParticipations(List.of(20), "main"))
                .thenReturn(List.of(relation(1, 20, null)));
        when(e2eRepository.findAllByIdIn(Set.of(1))).thenReturn(List.of(e2e(1, "SPARX")));

        assertThat(service.findGroupedByE2e("1", null, null).get(0).getOperations().get(0).getArchOperation()).isNull();

        when(discoveredOperationRepository.findAllByInterfaceIdInAndDeletedDateIsNull(List.of(5)))
                .thenReturn(List.of(discoveredRow(20, 5, 100)));
        when(operationRepository.findAllByIdIn(List.of(100))).thenReturn(List.of());

        assertThat(service.findGroupedByE2e("1", null, null).get(0).getOperations().get(0).getArchOperation()).isNull();
    }

    @Test
    @DisplayName("Один ТС на две операции")
    void sameTechCapabilityForTwoOperations() {
        stubProductById();
        when(e2eRepository.findAllSources()).thenReturn(List.of("SPARX"));
        when(discoveredInterfaceRepository.findActiveByProductIdAndSourceInIgnoreCase(eq(PRODUCT_ID), eq(Set.of("sparx"))))
                .thenReturn(List.of(interfaceRow(5, "SPARX")));
        when(discoveredOperationRepository.findAllByInterfaceIdInAndDeletedDateIsNull(List.of(5)))
                .thenReturn(List.of(discoveredRow(10, 5, 100), discoveredRow(20, 5, 101)));
        when(operationRelationRepository.findDiscoveredOperationParticipations(List.of(10, 20), "main"))
                .thenReturn(List.of(
                        relation(1, 10, null),
                        relation(1, 20, null)));
        when(e2eRepository.findAllByIdIn(Set.of(1))).thenReturn(List.of(e2e(1, "SPARX")));
        when(operationRepository.findAllByIdIn(List.of(100, 101)))
                .thenReturn(List.of(operationRow(100, 3), operationRow(101, 3)));
        TcDTO tc = TcDTO.builder().id(3).code("TC.01").name("ТС").build();
        when(capabilityClient.getTcs(List.of(3))).thenReturn(List.of(tc));

        List<E2eGroupedByE2eDTO> result = service.findGroupedByE2e("1", null, null);

        assertThat(result.get(0).getOperations()).hasSize(2);
        assertThat(result.get(0).getOperations().get(0).getArchOperation().getTechCapability().getId()).isEqualTo(3);
        assertThat(result.get(0).getOperations().get(1).getArchOperation().getTechCapability().getId()).isEqualTo(3);
        verify(capabilityClient).getTcs(List.of(3));
    }

    @Test
    @DisplayName("Пустой набор без вызова capability")
    void emptyResultDoesNotCallCapability() {
        stubProductById();
        when(e2eRepository.findAllSources()).thenReturn(List.of());

        assertThat(service.findGroupedByE2e("1", null, null)).isEmpty();
        verify(capabilityClient, never()).getTcs(anyList());
        verify(discoveredInterfaceRepository, never()).findActiveByProductIdAndSourceInIgnoreCase(any(), any());
    }

    @Test
    @DisplayName("404 продукта по id и alias")
    void productNotFound() {
        when(productRepository.findById(99)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.findGroupedByE2e("99", null, null))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Продукт не найден.");

        when(productRepository.findByAliasCaseInsensitive("unknown")).thenReturn(null);
        assertThatThrownBy(() -> service.findGroupedByE2e(null, "unknown", null))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Продукт не найден.");
    }

    @Test
    @DisplayName("Оба query и ни одного — 400; невалидный id и пустой alias — 400")
    void validatesQueryParams() {
        assertThatThrownBy(() -> service.findGroupedByE2e(null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Не передан идентификатор или alias продукта");
        assertThatThrownBy(() -> service.findGroupedByE2e("1", "alias", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Нужно передать либо идентификатор, либо alias продукта");
        assertThatThrownBy(() -> service.findGroupedByE2e("0", null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Идентификатор продукта должен быть положительным числом");
        assertThatThrownBy(() -> service.findGroupedByE2e("-1", null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Идентификатор продукта должен быть положительным числом");
        assertThatThrownBy(() -> service.findGroupedByE2e("abc", null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Идентификатор продукта должен быть положительным числом");
        assertThatThrownBy(() -> service.findGroupedByE2e(null, "   ", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Параметр alias не может быть пустым");
    }

    @Test
    @DisplayName("502 capability: исключение или пустое тело")
    void capabilityUnavailableIsBadGateway() {
        stubProductById();
        when(e2eRepository.findAllSources()).thenReturn(List.of("SPARX"));
        when(discoveredInterfaceRepository.findActiveByProductIdAndSourceInIgnoreCase(eq(PRODUCT_ID), eq(Set.of("sparx"))))
                .thenReturn(List.of(interfaceRow(5, "SPARX")));
        when(discoveredOperationRepository.findAllByInterfaceIdInAndDeletedDateIsNull(List.of(5)))
                .thenReturn(List.of(discoveredRow(20, 5, 100)));
        when(operationRelationRepository.findDiscoveredOperationParticipations(List.of(20), "main"))
                .thenReturn(List.of(relation(1, 20, null)));
        when(e2eRepository.findAllByIdIn(Set.of(1))).thenReturn(List.of(e2e(1, "SPARX")));
        when(operationRepository.findAllByIdIn(List.of(100))).thenReturn(List.of(operationRow(100, 3)));

        doThrow(new RuntimeException("down")).when(capabilityClient).getTcs(anyList());
        assertThatThrownBy(() -> service.findGroupedByE2e("1", null, null))
                .isInstanceOf(ExternalServiceException.class)
                .hasMessage("Сервис capability недоступен")
                .extracting(ex -> ((ExternalServiceException) ex).getStatus())
                .isEqualTo(BAD_GATEWAY);

        doReturn(List.of()).when(capabilityClient).getTcs(anyList());
        assertThatThrownBy(() -> service.findGroupedByE2e("1", null, null))
                .isInstanceOf(ExternalServiceException.class)
                .hasMessage("Сервис capability недоступен");
    }

    @Test
    @DisplayName("Поиск по alias без учёта регистра; порядок e2e и методов по id")
    void findsByAliasAndSorts() {
        Product product = product();
        when(productRepository.findByAliasCaseInsensitive(ALIAS)).thenReturn(product);
        when(e2eRepository.findAllSources()).thenReturn(List.of("SPARX"));
        when(discoveredInterfaceRepository.findActiveByProductIdAndSourceInIgnoreCase(eq(PRODUCT_ID), eq(Set.of("sparx"))))
                .thenReturn(List.of(interfaceRow(5, "SPARX")));
        when(discoveredOperationRepository.findAllByInterfaceIdInAndDeletedDateIsNull(List.of(5)))
                .thenReturn(List.of(discoveredRow(30, 5, null), discoveredRow(10, 5, null)));
        when(operationRelationRepository.findDiscoveredOperationParticipations(List.of(30, 10), "main"))
                .thenReturn(List.of(
                        relation(5, 30, null),
                        relation(2, 10, null),
                        relation(5, 10, null)));
        when(e2eRepository.findAllByIdIn(Set.of(2, 5)))
                .thenReturn(List.of(e2e(5, "SPARX"), e2e(2, "SPARX")));

        List<E2eGroupedByE2eDTO> result = service.findGroupedByE2e(null, "  " + ALIAS + "  ", null);

        assertThat(result).extracting(E2eGroupedByE2eDTO::getId).containsExactly(2, 5);
        assertThat(result.get(0).getOperations()).extracting(op -> op.getId()).containsExactly(10);
        assertThat(result.get(1).getOperations()).extracting(op -> op.getId()).containsExactly(10, 30);
        verify(productRepository).findByAliasCaseInsensitive(ALIAS);
    }

    @Test
    @DisplayName("Удалённая каноническая operation всё равно отдаётся в archOperation")
    void deletedArchOperationStillReturned() {
        stubProductById();
        when(e2eRepository.findAllSources()).thenReturn(List.of("SPARX"));
        when(discoveredInterfaceRepository.findActiveByProductIdAndSourceInIgnoreCase(eq(PRODUCT_ID), eq(Set.of("sparx"))))
                .thenReturn(List.of(interfaceRow(5, "SPARX")));
        when(discoveredOperationRepository.findAllByInterfaceIdInAndDeletedDateIsNull(List.of(5)))
                .thenReturn(List.of(discoveredRow(20, 5, 100)));
        when(operationRelationRepository.findDiscoveredOperationParticipations(List.of(20), "main"))
                .thenReturn(List.of(relation(1, 20, null)));
        when(e2eRepository.findAllByIdIn(Set.of(1))).thenReturn(List.of(e2e(1, "SPARX")));
        Operation deleted = operationRow(100, null);
        deleted.setDeletedDate(LocalDateTime.of(2026, 1, 1, 0, 0));
        when(operationRepository.findAllByIdIn(List.of(100))).thenReturn(List.of(deleted));

        assertThat(service.findGroupedByE2e("1", null, null).get(0).getOperations().get(0).getArchOperation()).isNotNull();
        assertThat(service.findGroupedByE2e("1", null, null).get(0).getOperations().get(0).getArchOperation().getDeletedDate())
                .isEqualTo(LocalDateTime.of(2026, 1, 1, 0, 0));
        verify(capabilityClient, never()).getTcs(anyList());
    }

    @Test
    @DisplayName("SFDM-4244: участие считается по id discovered_operation на любой стороне, в карточке владелец")
    void countsParticipationOnAnySideAndAddsOwner() {
        stubProductById();
        stubHappyPathWithoutArch(20);
        when(operationRelationRepository.findDiscoveredOperationParticipations(List.of(20), "main"))
                .thenReturn(List.of(relation(1, null, 20), relation(2, 20, 77)));
        E2e owned = e2e(1, "SPARX");
        owned.setProductId(PRODUCT_ID);
        when(e2eRepository.findAllByIdIn(Set.of(1, 2))).thenReturn(List.of(owned, e2e(2, "SPARX")));
        when(e2eOwnerService.aliasesOf(any())).thenReturn(java.util.Map.of(PRODUCT_ID, ALIAS));

        List<E2eGroupedByE2eDTO> result = service.findGroupedByE2e("1", null, null);

        assertThat(result).extracting(E2eGroupedByE2eDTO::getId).containsExactly(1, 2);
        assertThat(result.get(0).getProductId()).isEqualTo(PRODUCT_ID);
        assertThat(result.get(0).getProductAlias()).isEqualTo(ALIAS);
        assertThat(result.get(1).getProductId()).isNull();
        assertThat(result.get(1).getProductAlias()).isNull();
    }

    private void stubProductById() {
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product()));
    }

    private void stubHappyPathWithoutArch(Integer methodId) {
        when(e2eRepository.findAllSources()).thenReturn(List.of("SPARX"));
        when(discoveredInterfaceRepository.findActiveByProductIdAndSourceInIgnoreCase(eq(PRODUCT_ID), eq(Set.of("sparx"))))
                .thenReturn(List.of(interfaceRow(5, "SPARX")));
        when(discoveredOperationRepository.findAllByInterfaceIdInAndDeletedDateIsNull(List.of(5)))
                .thenReturn(List.of(discoveredRow(methodId, 5, null)));
    }

    private Product product() {
        return Product.builder().id(PRODUCT_ID).alias(ALIAS).name("Orders").build();
    }

    private DiscoveredInterface interfaceRow(Integer id, String source) {
        return DiscoveredInterface.builder().id(id).source(source).product(product()).build();
    }

    private DiscoveredOperation discoveredRow(Integer id, Integer interfaceId, Integer connectionOperationId) {
        return DiscoveredOperation.builder()
                .id(id)
                .interfaceId(interfaceId)
                .connectionOperationId(connectionOperationId)
                .name("method-" + id)
                .type("GET")
                .build();
    }

    /** Ветка main карточки получила id карточки (SFDM-4250). */
    private E2eRelationParticipation relation(Integer e2eId, Integer operationId, Integer relatedOperationId) {
        return new E2eRelationParticipation(OperationRelation.builder()
                .e2eBranchId(e2eId)
                .operationId(operationId)
                .relatedOperationId(relatedOperationId == null ? 0 : relatedOperationId)
                .order(1)
                .build(), e2eId);
    }

    private E2e e2e(Integer id, String source) {
        return E2e.builder().id(id).code("code-" + id).name("E2E " + id).source(source).build();
    }

    private Operation operationRow(Integer id, Integer tcId) {
        return Operation.builder()
                .id(id)
                .interfaceId(8)
                .name("arch-" + id)
                .type("POST")
                .tcId(tcId)
                .isDeletedTc(false)
                .build();
    }
}
