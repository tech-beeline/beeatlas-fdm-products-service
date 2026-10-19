/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import ru.beeline.fdmproducts.client.CapabilityClient;
import ru.beeline.fdmproducts.domain.ContainerProduct;
import ru.beeline.fdmproducts.domain.DiscoveredInterface;
import ru.beeline.fdmproducts.domain.DiscoveredOperation;
import ru.beeline.fdmproducts.domain.E2e;
import ru.beeline.fdmproducts.domain.Interface;
import ru.beeline.fdmproducts.domain.Operation;
import ru.beeline.fdmproducts.domain.OperationRelation;
import ru.beeline.fdmproducts.domain.Product;
import ru.beeline.fdmproducts.domain.ProductBranch;
import ru.beeline.fdmproducts.dto.TcDTO;
import ru.beeline.fdmproducts.dto.e2e.ProductE2eOperationDTO;
import ru.beeline.fdmproducts.dto.e2e.ProductE2eStepDTO;
import ru.beeline.fdmproducts.exception.EntityNotFoundException;
import ru.beeline.fdmproducts.exception.ExternalServiceException;
import ru.beeline.fdmproducts.repository.ContainerRepository;
import ru.beeline.fdmproducts.repository.DiscoveredInterfaceRepository;
import ru.beeline.fdmproducts.repository.DiscoveredOperationRepository;
import ru.beeline.fdmproducts.repository.E2eRepository;
import ru.beeline.fdmproducts.repository.InterfaceRepository;
import ru.beeline.fdmproducts.repository.OperationRelationRepository;
import ru.beeline.fdmproducts.repository.OperationRepository;
import ru.beeline.fdmproducts.repository.ProductRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_GATEWAY;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProductE2eStepsServiceTest {

    private static final String OPERATION = "operation";
    private static final String DISCOVERED = "discovered_operation";

    @Mock
    private ProductRepository productRepository;
    @Mock
    private ProductBranchService productBranchService;
    @Mock
    private ContainerRepository containerRepository;
    @Mock
    private InterfaceRepository interfaceRepository;
    @Mock
    private OperationRepository operationRepository;
    @Mock
    private DiscoveredInterfaceRepository discoveredInterfaceRepository;
    @Mock
    private DiscoveredOperationRepository discoveredOperationRepository;
    @Mock
    private OperationRelationRepository operationRelationRepository;
    @Mock
    private E2eRepository e2eRepository;
    @Mock
    private CapabilityClient capabilityClient;

    private ProductE2eStepsService service;

    private final Product product = Product.builder().id(3).alias("fdmshowcaseapp").build();

    @BeforeEach
    void setUp() {
        service = new ProductE2eStepsService(productRepository, productBranchService, containerRepository,
                interfaceRepository, operationRepository, discoveredInterfaceRepository,
                discoveredOperationRepository, operationRelationRepository, e2eRepository,
                new OperationTechCapabilityResolver(capabilityClient), new E2eOwnerService(productRepository));

        when(productRepository.findByAliasCaseInsensitive("fdmshowcaseapp")).thenReturn(product);
        when(productBranchService.find("fdmshowcaseapp", null))
                .thenReturn(Optional.of(ProductBranch.builder().id(1).alias("fdmshowcaseapp").build()));
        when(containerRepository.findAllByProductBranchIdAndDeletedDateIsNull(1))
                .thenReturn(List.of(ContainerProduct.builder().id(10).build()));
        when(interfaceRepository.findAllByContainerIdInAndDeletedDateIsNull(List.of(10)))
                .thenReturn(List.of(Interface.builder().id(20).containerId(10).build()));
        when(operationRepository.findAllByInterfaceIdInAndDeletedDateIsNull(List.of(20)))
                .thenReturn(List.of(operation(541, "GET", "/api/v2/profile/balance", 98),
                        operation(542, "POST", "/api/v1/cart", null)));
        DiscoveredInterface discoveredInterface = DiscoveredInterface.builder().id(30).product(product).build();
        when(discoveredInterfaceRepository.findAllByProductAndDeletedDateIsNull(product))
                .thenReturn(List.of(discoveredInterface));
        when(discoveredOperationRepository.findAllByInterfaceIdInAndDeletedDateIsNull(List.of(30)))
                .thenReturn(List.of(discovered(77513, "POST", "/api/v1/projects", null, "TC.07", "Своя ТВ"),
                        discovered(77514, "GET", "/api/v1/linked", 541, null, null)));
        when(operationRepository.findAllByIdIn(anyList()))
                .thenReturn(List.of(operation(541, "GET", "/api/v2/profile/balance", 98)));
        when(capabilityClient.getTcs(anyList()))
                .thenReturn(List.of(TcDTO.builder().id(98).code("TC.00.00.00.98").name("Перенос средств").build()));
        when(discoveredOperationRepository.findAllByConnectionOperationIdInAndDeletedDateIsNull(any()))
                .thenReturn(List.of(discovered(88001, "GET", "/balance", 541, null, null),
                        discovered(88002, "POST", "/cart", 542, null, null)));
        when(operationRelationRepository.findDiscoveredOperationParticipations(any())).thenReturn(List.of());
    }

    @Test
    @DisplayName("Метод приложения на вызываемой стороне — E2E с этим методом и его ТВ из capability")
    void returnsAnE2eWhereTheAppMethodIsCalled() {
        givenRelations(relation(1, 5, 1, 900, 88001));
        givenE2es(e2e(5, "checkout-flow", "Step.00.00.02.77"));

        List<ProductE2eStepDTO> result = service.getE2eSteps("fdmshowcaseapp");

        assertThat(result).singleElement().satisfies(step -> {
            assertThat(step.getE2e().getCode()).isEqualTo("checkout-flow");
            assertThat(step.getE2e().getBiStepCode()).isEqualTo("Step.00.00.02.77");
            assertThat(step.getOperations()).singleElement().satisfies(operation -> {
                assertThat(operation.getId()).isEqualTo(541);
                assertThat(operation.getEntityType()).isEqualTo(OPERATION);
                assertThat(operation.getType()).isEqualTo("GET");
                assertThat(operation.getName()).isEqualTo("/api/v2/profile/balance");
                assertThat(operation.getTechCapability().getCode()).isEqualTo("TC.00.00.00.98");
                assertThat(operation.getTechCapability().getName()).isEqualTo("Перенос средств");
            });
        });
    }

    @Test
    @DisplayName("Метод приложения на вызывающей стороне учитывается, чужой метод E2E — нет")
    void countsTheCallerSideAndSkipsForeignMethods() {
        givenRelations(relation(2, 6, 1, 77513, 999));
        givenE2es(e2e(6, "push-flow", "Step.00.00.03.01"));

        List<ProductE2eStepDTO> result = service.getE2eSteps("fdmshowcaseapp");

        assertThat(result).singleElement().satisfies(step -> assertThat(step.getOperations())
                .extracting(ProductE2eOperationDTO::getId).containsExactly(77513));
    }

    @Test
    @DisplayName("Метод в нескольких шагах одного E2E — один раз, порядок по первому шагу")
    void listsAMethodOnceInOrderOfFirstAppearance() {
        givenRelations(relation(3, 5, 4, 900, 88001),
                relation(4, 5, 2, 900, 88002),
                relation(5, 5, 7, 901, 88001));
        givenE2es(e2e(5, "checkout-flow", "Step.00.00.02.77"));

        List<ProductE2eStepDTO> result = service.getE2eSteps("fdmshowcaseapp");

        assertThat(result.get(0).getOperations()).extracting(ProductE2eOperationDTO::getId)
                .containsExactly(542, 541);
    }

    @Test
    @DisplayName("Группы по коду шага BI, без кода — в конце, при равном коде — по коду E2E")
    void sortsGroupsByBiStepCodeThenCode() {
        givenRelations(relation(6, 7, 1, 900, 88001),
                relation(7, 8, 1, 900, 88001),
                relation(8, 9, 1, 900, 88001));
        givenE2es(e2e(7, "z-flow", null), e2e(8, "b-flow", "Step.00.00.01.00"), e2e(9, "a-flow", "Step.00.00.01.00"));

        List<ProductE2eStepDTO> result = service.getE2eSteps("fdmshowcaseapp");

        assertThat(result).extracting(step -> step.getE2e().getCode()).containsExactly("a-flow", "b-flow", "z-flow");
    }

    @Test
    @DisplayName("ТВ обнаруженного метода — от связанного архитектурного метода, иначе tc_code самого метода")
    void resolvesTechCapabilitiesOfDiscoveredMethods() {
        givenRelations(relation(9, 5, 1, 900, 77513),
                relation(10, 5, 2, 900, 77514));
        givenE2es(e2e(5, "checkout-flow", "Step.00.00.02.77"));

        List<ProductE2eOperationDTO> operations = service.getE2eSteps("fdmshowcaseapp").get(0).getOperations();

        assertThat(operations.get(0).getTechCapability().getCode()).isEqualTo("TC.07");
        assertThat(operations.get(0).getTechCapability().getName()).isEqualTo("Своя ТВ");
        assertThat(operations.get(0).getTechCapability().getId()).isNull();
        assertThat(operations.get(1).getTechCapability().getId()).isEqualTo(98);
        assertThat(operations.get(1).getEntityType()).isEqualTo(DISCOVERED);
    }

    @Test
    @DisplayName("Методы приложения нигде не участвуют — пустой список, E2E не читаются")
    void returnsAnEmptyListWithoutParticipation() {
        assertThat(service.getE2eSteps("fdmshowcaseapp")).isEmpty();
        verify(e2eRepository, never()).findAllById(any());
    }

    @Test
    @DisplayName("У приложения нет методов — пустой список без запросов связей")
    void returnsAnEmptyListWithoutMethods() {
        when(containerRepository.findAllByProductBranchIdAndDeletedDateIsNull(1)).thenReturn(List.of());
        when(discoveredInterfaceRepository.findAllByProductAndDeletedDateIsNull(product)).thenReturn(List.of());

        assertThat(service.getE2eSteps("fdmshowcaseapp")).isEmpty();
        verify(operationRelationRepository, never()).findDiscoveredOperationParticipations(any());
    }

    @Test
    @DisplayName("SFDM-4244: e2e находится и по discovered-методу продукта, и по connection_operation_id архитектурного")
    void findsE2eByDiscoveredMethodAndByConnectedArchitectureMethod() {
        givenRelations(relation(11, 5, 1, 88001, 77513));
        E2e owned = e2e(5, "checkout-flow", "Step.00.00.02.77");
        owned.setProductId(3);
        givenE2es(owned);
        when(productRepository.findAllById(List.of(3))).thenReturn(List.of(product));

        ProductE2eStepDTO step = service.getE2eSteps("fdmshowcaseapp").get(0);

        assertThat(step.getOperations()).extracting(ProductE2eOperationDTO::getId, ProductE2eOperationDTO::getEntityType)
                .containsExactly(org.assertj.core.groups.Tuple.tuple(77513, DISCOVERED),
                        org.assertj.core.groups.Tuple.tuple(541, OPERATION));
        assertThat(step.getE2e().getProductId()).isEqualTo(3);
        assertThat(step.getE2e().getProductAlias()).isEqualTo("fdmshowcaseapp");
    }

    @Test
    @DisplayName("SFDM-4244: без владельца productId и productAlias карточки шага равны null")
    void cardWithoutOwnerHasNullOwnerFields() {
        givenRelations(relation(12, 5, 1, null, 77513));
        givenE2es(e2e(5, "checkout-flow", "Step.00.00.02.77"));

        ProductE2eStepDTO step = service.getE2eSteps("fdmshowcaseapp").get(0);

        assertThat(step.getE2e().getProductId()).isNull();
        assertThat(step.getE2e().getProductAlias()).isNull();
    }

    @Test
    @DisplayName("Неизвестный продукт — 404 с мнемоникой в тексте")
    void rejectsAnUnknownProduct() {
        assertThatThrownBy(() -> service.getE2eSteps("unknown"))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Продукт с данным cmdb: unknown не найден.");
    }

    @Test
    @DisplayName("Capability недоступен — 502")
    void failsWhenCapabilityIsUnavailable() {
        givenRelations(relation(1, 5, 1, 900, 88001));
        givenE2es(e2e(5, "checkout-flow", "Step.00.00.02.77"));
        when(capabilityClient.getTcs(anyList())).thenThrow(new RuntimeException("timeout"));

        assertThatThrownBy(() -> service.getE2eSteps("fdmshowcaseapp"))
                .isInstanceOf(ExternalServiceException.class)
                .hasMessage("Сервис capability недоступен")
                .satisfies(e -> assertThat(((ExternalServiceException) e).getStatus()).isEqualTo(BAD_GATEWAY));
    }

    private void givenRelations(OperationRelation... relations) {
        when(operationRelationRepository.findDiscoveredOperationParticipations(any())).thenReturn(List.of(relations));
    }

    private void givenE2es(E2e... e2es) {
        when(e2eRepository.findAllById(any())).thenReturn(List.of(e2es));
    }

    private static Operation operation(Integer id, String type, String name, Integer tcId) {
        return Operation.builder().id(id).interfaceId(20).type(type).name(name).tcId(tcId).build();
    }

    private static DiscoveredOperation discovered(Integer id, String type, String name, Integer connectionOperationId,
                                                  String tcCode, String tcDescription) {
        return DiscoveredOperation.builder().id(id).interfaceId(30).type(type).name(name)
                .connectionOperationId(connectionOperationId).tcCode(tcCode).tcDescription(tcDescription).build();
    }

    private static OperationRelation relation(Integer id, Integer e2eId, Integer order, Integer operationId,
                                              Integer relatedOperationId) {
        return OperationRelation.builder().id(id).e2eId(e2eId).order(order).operationId(operationId)
                .relatedOperationId(relatedOperationId).build();
    }

    private static E2e e2e(Integer id, String code, String biStepCode) {
        return E2e.builder().id(id).code(code).name(code).biStepCode(biStepCode).source("SPARX").build();
    }
}
