/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import ru.beeline.fdmproducts.domain.Interface;
import ru.beeline.fdmproducts.domain.Operation;
import ru.beeline.fdmproducts.dto.InterfaceDTO;
import ru.beeline.fdmproducts.dto.MethodDTO;
import ru.beeline.fdmproducts.mapper.OperationMapper;
import ru.beeline.fdmproducts.repository.OperationRelationRepository;
import ru.beeline.fdmproducts.repository.OperationRepository;
import ru.beeline.fdmproducts.repository.ParameterRepository;
import ru.beeline.fdmproducts.repository.SlaRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * PUT /api/v1/product/{code}/relations: метод интерфейса ищется без учёта регистра name и type — по тому же
 * правилу, что сопоставление discovered_operation с operation. Иначе каждая загрузка с другим регистром
 * создавала дубль, а прежняя операция уходила в deleted вместе со связями bi-step и discovered_operation.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProductServiceOperationKeyTest {

    private static final Integer INTERFACE_ID = 500;

    @Mock
    private OperationRepository operationRepository;
    @Mock
    private SlaRepository slaRepository;
    @Mock
    private ParameterRepository parameterRepository;
    @Mock
    private OperationRelationRepository operationRelationRepository;
    @Spy
    private OperationMapper operationMapper = new OperationMapper();

    @InjectMocks
    private ProductService service;

    private final List<Operation> saved = new ArrayList<>();

    @BeforeEach
    void setUp() {
        when(operationRepository.saveAll(anyList())).thenAnswer(invocation -> {
            Collection<Operation> operations = invocation.getArgument(0);
            saved.addAll(operations);
            return new ArrayList<>(operations);
        });
    }

    @Test
    @DisplayName("Ключ метода: имя и тип без учёта регистра, path-параметры и query как при сопоставлении")
    void operationKeyIgnoresCase() {
        assertThat(ProductService.operationKey("/api/v1/Cart", "GET"))
                .isEqualTo(ProductService.operationKey("/API/v1/cart", " get "));
        assertThat(ProductService.operationKey("/users/{id}", "GET"))
                .isEqualTo(ProductService.operationKey("/Users/{userId}?x=1", "get"));
        assertThat(ProductService.operationKey("/api/v1/cart", "GET"))
                .isNotEqualTo(ProductService.operationKey("/api/v1/cart", "POST"));
        assertThat(ProductService.operationKey(null, null)).isEqualTo("::");
    }

    @Test
    @DisplayName("Метод в другом регистре обновляет существующую операцию, дубль не создаётся и прежняя не удаляется")
    void otherCaseUpdatesExistingOperation() {
        Operation existing = operation(10, "/api/v1/Cart", "GET", null);
        givenOperations(existing);

        service.processInterfaceMethods(request(method("/api/v1/cart", "get")), iface(), Map.of());

        assertThat(saved).allMatch(op -> op.getId() != null);
        assertThat(existing.getDeletedDate()).isNull();
        assertThat(existing.getName()).isEqualTo("/api/v1/cart");
        assertThat(existing.getType()).isEqualTo("get");
    }

    @Test
    @DisplayName("Удалённая операция в другом регистре оживает, а не заменяется новой")
    void deletedOperationInOtherCaseIsRevived() {
        Operation deleted = operation(12, "/Cart", "POST", LocalDateTime.of(2026, 1, 1, 0, 0));
        givenOperations(deleted);

        service.processInterfaceMethods(request(method("/cart", "post")), iface(), Map.of());

        assertThat(saved).allMatch(op -> op.getId() != null);
        assertThat(deleted.getDeletedDate()).isNull();
    }

    @Test
    @DisplayName("Накопленный дубль: берётся живая операция, удалённая остаётся удалённой, новая не создаётся")
    void accumulatedDuplicatePrefersAliveOperation() {
        Operation alive = operation(20, "/API/cart", "GET", null);
        Operation deleted = operation(21, "/api/cart", "GET", LocalDateTime.of(2026, 1, 1, 0, 0));
        givenOperations(alive, deleted);

        service.processInterfaceMethods(request(method("/api/cart", "GET")), iface(), Map.of());

        assertThat(saved).allMatch(op -> op.getId() != null);
        assertThat(alive.getDeletedDate()).isNull();
        assertThat(deleted.getDeletedDate()).isNotNull();
    }

    @Test
    @DisplayName("Один метод дважды в запросе в разном регистре — создаётся одна операция")
    void sameMethodTwiceInRequestCreatesOneOperation() {
        givenOperations();

        service.processInterfaceMethods(request(method("/api/v1/pay", "POST"), method("/API/v1/Pay", "post")),
                iface(), Map.of());

        assertThat(saved.stream().filter(op -> op.getId() == null).distinct().count()).isEqualTo(1);
    }

    @Test
    @DisplayName("Разные методы остаются разными: метод, которого нет в запросе, помечается удалённым")
    void otherMethodsStayDistinct() {
        Operation get = operation(30, "/api/v1/cart", "GET", null);
        Operation post = operation(31, "/api/v1/cart", "POST", null);
        givenOperations(get, post);

        service.processInterfaceMethods(request(method("/API/v1/cart", "get")), iface(), Map.of());

        assertThat(get.getDeletedDate()).isNull();
        assertThat(post.getDeletedDate()).isNotNull();
        verify(operationRepository, atLeast(1)).saveAll(anyList());
        assertThat(saved).allMatch(op -> op.getId() != null);
    }

    private void givenOperations(Operation... operations) {
        when(operationRepository.findAllByInterfaceId(INTERFACE_ID)).thenReturn(Arrays.asList(operations));
    }

    private static Interface iface() {
        Interface iface = new Interface();
        iface.setId(INTERFACE_ID);
        return iface;
    }

    private static InterfaceDTO request(MethodDTO... methods) {
        InterfaceDTO dto = new InterfaceDTO();
        dto.setCode("cart-rest");
        dto.setMethods(new ArrayList<>(Arrays.asList(methods)));
        return dto;
    }

    private static MethodDTO method(String name, String type) {
        return MethodDTO.builder().name(name).type(type).build();
    }

    private static Operation operation(Integer id, String name, String type, LocalDateTime deletedDate) {
        Operation operation = Operation.builder().id(id).name(name).type(type).interfaceId(INTERFACE_ID).build();
        operation.setDeletedDate(deletedDate);
        return operation;
    }
}
