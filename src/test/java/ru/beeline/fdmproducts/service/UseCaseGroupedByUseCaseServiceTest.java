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
import ru.beeline.fdmproducts.client.ProjectClient;
import ru.beeline.fdmproducts.domain.DiscoveredInterface;
import ru.beeline.fdmproducts.domain.DiscoveredOperation;
import ru.beeline.fdmproducts.domain.Operation;
import ru.beeline.fdmproducts.domain.Product;
import ru.beeline.fdmproducts.dto.TcDTO;
import ru.beeline.fdmproducts.dto.project.UseCaseOperationsGroupedDTO;
import ru.beeline.fdmproducts.dto.usecase.UseCaseGroupedByUseCaseDTO;
import ru.beeline.fdmproducts.exception.EntityNotFoundException;
import ru.beeline.fdmproducts.exception.ExternalServiceException;
import ru.beeline.fdmproducts.mapper.E2eGroupedByE2eMapper;
import ru.beeline.fdmproducts.mapper.UseCaseGroupedByUseCaseMapper;
import ru.beeline.fdmproducts.repository.DiscoveredInterfaceRepository;
import ru.beeline.fdmproducts.repository.DiscoveredOperationRepository;
import ru.beeline.fdmproducts.repository.OperationRepository;
import ru.beeline.fdmproducts.repository.ProductRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static ru.beeline.fdmproducts.service.UseCaseDiscoveredInterfaceService.SOURCE;

@ExtendWith(MockitoExtension.class)
class UseCaseGroupedByUseCaseServiceTest {

    private static final Integer PRODUCT_ID = 1;
    private static final String ALIAS = "orders-product";

    @Mock
    private ProductRepository productRepository;
    @Mock
    private DiscoveredInterfaceRepository discoveredInterfaceRepository;
    @Mock
    private DiscoveredOperationRepository discoveredOperationRepository;
    @Mock
    private OperationRepository operationRepository;
    @Mock
    private ProjectClient projectClient;
    @Mock
    private CapabilityClient capabilityClient;
    @Spy
    private E2eGroupedByE2eMapper operationMapper = new E2eGroupedByE2eMapper();
    @Spy
    private UseCaseGroupedByUseCaseMapper useCaseMapper = new UseCaseGroupedByUseCaseMapper();

    @InjectMocks
    private UseCaseGroupedByUseCaseService service;

    @Test
    @DisplayName("Интерфейс другого source не берётся")
    void otherSourceInterfaceExcluded() {
        stubProductById();
        when(discoveredInterfaceRepository.findAllByProductAndSourceIgnoreCaseAndDeletedDateIsNull(
                any(Product.class), eq(SOURCE))).thenReturn(List.of());

        assertThat(service.findGroupedByUseCase("1", null)).isEmpty();
        verify(discoveredOperationRepository, never()).findAllByInterfaceIdInAndDeletedDateIsNull(anyList());
        verify(projectClient, never()).findOperationsGroupedByUseCase(anyList());
        verify(capabilityClient, never()).getTcs(anyList());
    }

    @Test
    @DisplayName("Интерфейс другого продукта не берётся")
    void otherProductInterfaceNotSelected() {
        stubProductById();
        when(discoveredInterfaceRepository.findAllByProductAndSourceIgnoreCaseAndDeletedDateIsNull(
                any(Product.class), eq(SOURCE))).thenReturn(List.of());

        assertThat(service.findGroupedByUseCase("1", null)).isEmpty();
        verify(discoveredInterfaceRepository).findAllByProductAndSourceIgnoreCaseAndDeletedDateIsNull(
                any(Product.class), eq(SOURCE));
    }

    @Test
    @DisplayName("Удалённые интерфейс и метод не берутся")
    void deletedInterfaceAndMethodExcluded() {
        stubProductById();
        when(discoveredInterfaceRepository.findAllByProductAndSourceIgnoreCaseAndDeletedDateIsNull(
                any(Product.class), eq(SOURCE))).thenReturn(List.of());

        assertThat(service.findGroupedByUseCase("1", null)).isEmpty();

        when(discoveredInterfaceRepository.findAllByProductAndSourceIgnoreCaseAndDeletedDateIsNull(
                any(Product.class), eq(SOURCE))).thenReturn(List.of(interfaceRow(5)));
        when(discoveredOperationRepository.findAllByInterfaceIdInAndDeletedDateIsNull(List.of(5)))
                .thenReturn(List.of());

        assertThat(service.findGroupedByUseCase("1", null)).isEmpty();
        verify(projectClient, never()).findOperationsGroupedByUseCase(anyList());
        verify(capabilityClient, never()).getTcs(anyList());
    }

    @Test
    @DisplayName("Метод без участия в use case не попадает в ответ и не обогащается")
    void methodWithoutUseCaseStepExcluded() {
        stubProductById();
        when(discoveredInterfaceRepository.findAllByProductAndSourceIgnoreCaseAndDeletedDateIsNull(
                any(Product.class), eq(SOURCE))).thenReturn(List.of(interfaceRow(5)));
        DiscoveredOperation participating = discoveredRow(20, 5, 100);
        DiscoveredOperation orphan = discoveredRow(30, 5, 200);
        when(discoveredOperationRepository.findAllByInterfaceIdInAndDeletedDateIsNull(List.of(5)))
                .thenReturn(List.of(participating, orphan));
        when(projectClient.findOperationsGroupedByUseCase(List.of(20, 30)))
                .thenReturn(List.of(useCaseCard(7, List.of(20))));
        when(operationRepository.findAllByIdIn(List.of(100))).thenReturn(List.of(operationRow(100, null)));

        List<UseCaseGroupedByUseCaseDTO> result = service.findGroupedByUseCase("1", null);

        assertThat(result.get(0).getOperations()).extracting(op -> op.getId()).containsExactly(20);
        verify(operationRepository).findAllByIdIn(List.of(100));
        verify(capabilityClient, never()).getTcs(anyList());
    }

    @Test
    @DisplayName("Обогащение archOperation и techCapability")
    void enrichesArchOperationAndTechCapability() {
        stubProductById();
        when(discoveredInterfaceRepository.findAllByProductAndSourceIgnoreCaseAndDeletedDateIsNull(
                any(Product.class), eq(SOURCE))).thenReturn(List.of(interfaceRow(5)));
        when(discoveredOperationRepository.findAllByInterfaceIdInAndDeletedDateIsNull(List.of(5)))
                .thenReturn(List.of(discoveredRow(20, 5, 100)));
        when(projectClient.findOperationsGroupedByUseCase(List.of(20)))
                .thenReturn(List.of(useCaseCard(7, List.of(20))));
        Operation arch = operationRow(100, 3);
        when(operationRepository.findAllByIdIn(List.of(100))).thenReturn(List.of(arch));
        TcDTO tc = TcDTO.builder().id(3).code("TC.01").name("Оформление заказа").build();
        when(capabilityClient.getTcs(List.of(3))).thenReturn(List.of(tc));

        List<UseCaseGroupedByUseCaseDTO> result = service.findGroupedByUseCase("1", null);

        assertThat(result.get(0).getOperations().get(0).getArchOperation()).isNotNull();
        assertThat(result.get(0).getOperations().get(0).getArchOperation().getId()).isEqualTo(100);
        assertThat(result.get(0).getOperations().get(0).getArchOperation().getTechCapability().getId()).isEqualTo(3);
    }

    @Test
    @DisplayName("connection_operation_id пустой или operation нет — archOperation = null")
    void archOperationNullWhenNoConnection() {
        stubProductById();
        stubHappyPathWithoutArch(20);
        when(projectClient.findOperationsGroupedByUseCase(List.of(20)))
                .thenReturn(List.of(useCaseCard(7, List.of(20))));

        assertThat(service.findGroupedByUseCase("1", null).get(0).getOperations().get(0).getArchOperation()).isNull();

        when(discoveredOperationRepository.findAllByInterfaceIdInAndDeletedDateIsNull(List.of(5)))
                .thenReturn(List.of(discoveredRow(20, 5, 100)));
        when(operationRepository.findAllByIdIn(List.of(100))).thenReturn(List.of());

        assertThat(service.findGroupedByUseCase("1", null).get(0).getOperations().get(0).getArchOperation()).isNull();
    }

    @Test
    @DisplayName("Один ТС на две операции")
    void sameTechCapabilityForTwoOperations() {
        stubProductById();
        when(discoveredInterfaceRepository.findAllByProductAndSourceIgnoreCaseAndDeletedDateIsNull(
                any(Product.class), eq(SOURCE))).thenReturn(List.of(interfaceRow(5)));
        when(discoveredOperationRepository.findAllByInterfaceIdInAndDeletedDateIsNull(List.of(5)))
                .thenReturn(List.of(discoveredRow(10, 5, 100), discoveredRow(20, 5, 101)));
        when(projectClient.findOperationsGroupedByUseCase(List.of(10, 20)))
                .thenReturn(List.of(useCaseCard(7, List.of(10, 20))));
        when(operationRepository.findAllByIdIn(List.of(100, 101)))
                .thenReturn(List.of(operationRow(100, 3), operationRow(101, 3)));
        TcDTO tc = TcDTO.builder().id(3).code("TC.01").name("ТС").build();
        when(capabilityClient.getTcs(List.of(3))).thenReturn(List.of(tc));

        List<UseCaseGroupedByUseCaseDTO> result = service.findGroupedByUseCase("1", null);

        assertThat(result.get(0).getOperations()).hasSize(2);
        assertThat(result.get(0).getOperations().get(0).getArchOperation().getTechCapability().getId()).isEqualTo(3);
        assertThat(result.get(0).getOperations().get(1).getArchOperation().getTechCapability().getId()).isEqualTo(3);
        verify(capabilityClient).getTcs(List.of(3));
    }

    @Test
    @DisplayName("Пустой набор методов без вызова project и capability")
    void emptyMethodsDoNotCallProject() {
        stubProductById();
        when(discoveredInterfaceRepository.findAllByProductAndSourceIgnoreCaseAndDeletedDateIsNull(
                any(Product.class), eq(SOURCE))).thenReturn(List.of(interfaceRow(5)));
        when(discoveredOperationRepository.findAllByInterfaceIdInAndDeletedDateIsNull(List.of(5)))
                .thenReturn(List.of());

        assertThat(service.findGroupedByUseCase("1", null)).isEmpty();
        verify(projectClient, never()).findOperationsGroupedByUseCase(anyList());
        verify(capabilityClient, never()).getTcs(anyList());
    }

    @Test
    @DisplayName("Пустой ответ project без вызова capability")
    void emptyProjectResponseDoesNotCallCapability() {
        stubProductById();
        stubHappyPathWithoutArch(20);
        when(projectClient.findOperationsGroupedByUseCase(List.of(20))).thenReturn(List.of());

        assertThat(service.findGroupedByUseCase("1", null)).isEmpty();
        verify(capabilityClient, never()).getTcs(anyList());
        verify(operationRepository, never()).findAllByIdIn(anyList());
    }

    @Test
    @DisplayName("404 продукта по id и alias")
    void productNotFound() {
        when(productRepository.findById(99)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.findGroupedByUseCase("99", null))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Продукт не найден.");

        when(productRepository.findByAliasCaseInsensitive("unknown")).thenReturn(null);
        assertThatThrownBy(() -> service.findGroupedByUseCase(null, "unknown"))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Продукт не найден.");
    }

    @Test
    @DisplayName("Оба query и ни одного — 400; невалидный id и пустой alias — 400")
    void validatesQueryParams() {
        assertThatThrownBy(() -> service.findGroupedByUseCase(null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Не передан идентификатор или alias продукта");
        assertThatThrownBy(() -> service.findGroupedByUseCase("1", "alias"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Нужно передать либо идентификатор, либо alias продукта");
        assertThatThrownBy(() -> service.findGroupedByUseCase("0", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Идентификатор продукта должен быть положительным числом");
        assertThatThrownBy(() -> service.findGroupedByUseCase("-1", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Идентификатор продукта должен быть положительным числом");
        assertThatThrownBy(() -> service.findGroupedByUseCase("abc", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Идентификатор продукта должен быть положительным числом");
        assertThatThrownBy(() -> service.findGroupedByUseCase(null, "   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Параметр alias не может быть пустым");
    }

    @Test
    @DisplayName("502 project: capability не вызывается")
    void projectUnavailableIsBadGatewayWithoutCapability() {
        stubProductById();
        stubHappyPathWithoutArch(20);
        when(projectClient.findOperationsGroupedByUseCase(List.of(20)))
                .thenThrow(new ExternalServiceException(BAD_GATEWAY, "Сервис project недоступен"));

        assertThatThrownBy(() -> service.findGroupedByUseCase("1", null))
                .isInstanceOf(ExternalServiceException.class)
                .hasMessage("Сервис project недоступен")
                .extracting(ex -> ((ExternalServiceException) ex).getStatus())
                .isEqualTo(BAD_GATEWAY);

        verify(capabilityClient, never()).getTcs(anyList());
        verify(operationRepository, never()).findAllByIdIn(anyList());
    }

    @Test
    @DisplayName("4xx project пробрасывается")
    void projectClientErrorPropagates4xx() {
        stubProductById();
        stubHappyPathWithoutArch(20);
        when(projectClient.findOperationsGroupedByUseCase(List.of(20)))
                .thenThrow(new ExternalServiceException(BAD_REQUEST,
                        "Список идентификаторов методов не может быть пустым"));

        assertThatThrownBy(() -> service.findGroupedByUseCase("1", null))
                .isInstanceOf(ExternalServiceException.class)
                .extracting(ex -> ((ExternalServiceException) ex).getStatus())
                .isEqualTo(BAD_REQUEST);
    }

    @Test
    @DisplayName("502 capability: исключение или пустое тело")
    void capabilityUnavailableIsBadGateway() {
        stubProductById();
        when(discoveredInterfaceRepository.findAllByProductAndSourceIgnoreCaseAndDeletedDateIsNull(
                any(Product.class), eq(SOURCE))).thenReturn(List.of(interfaceRow(5)));
        when(discoveredOperationRepository.findAllByInterfaceIdInAndDeletedDateIsNull(List.of(5)))
                .thenReturn(List.of(discoveredRow(20, 5, 100)));
        when(projectClient.findOperationsGroupedByUseCase(List.of(20)))
                .thenReturn(List.of(useCaseCard(7, List.of(20))));
        when(operationRepository.findAllByIdIn(List.of(100))).thenReturn(List.of(operationRow(100, 3)));

        doThrow(new RuntimeException("down")).when(capabilityClient).getTcs(anyList());
        assertThatThrownBy(() -> service.findGroupedByUseCase("1", null))
                .isInstanceOf(ExternalServiceException.class)
                .hasMessage("Сервис capability недоступен")
                .extracting(ex -> ((ExternalServiceException) ex).getStatus())
                .isEqualTo(BAD_GATEWAY);

        doReturn(List.of()).when(capabilityClient).getTcs(anyList());
        assertThatThrownBy(() -> service.findGroupedByUseCase("1", null))
                .isInstanceOf(ExternalServiceException.class)
                .hasMessage("Сервис capability недоступен");
    }

    @Test
    @DisplayName("Поиск по alias без учёта регистра; порядок карточек и operations как в project")
    void findsByAliasAndPreservesOrder() {
        Product product = product();
        when(productRepository.findByAliasCaseInsensitive(ALIAS)).thenReturn(product);
        when(discoveredInterfaceRepository.findAllByProductAndSourceIgnoreCaseAndDeletedDateIsNull(
                any(Product.class), eq(SOURCE))).thenReturn(List.of(interfaceRow(5)));
        when(discoveredOperationRepository.findAllByInterfaceIdInAndDeletedDateIsNull(List.of(5)))
                .thenReturn(List.of(discoveredRow(30, 5, null), discoveredRow(10, 5, null)));
        when(projectClient.findOperationsGroupedByUseCase(List.of(10, 30)))
                .thenReturn(List.of(
                        useCaseCard(8, List.of(30, 10, 99)),
                        useCaseCard(7, List.of(99, 10))));

        List<UseCaseGroupedByUseCaseDTO> result = service.findGroupedByUseCase(null, "  " + ALIAS + "  ");

        assertThat(result).extracting(UseCaseGroupedByUseCaseDTO::getId).containsExactly(8, 7);
        assertThat(result.get(0).getOperations()).extracting(op -> op.getId()).containsExactly(30, 10);
        assertThat(result.get(1).getOperations()).extracting(op -> op.getId()).containsExactly(10);
        verify(productRepository).findByAliasCaseInsensitive(ALIAS);
        verify(projectClient).findOperationsGroupedByUseCase(List.of(10, 30));
    }

    @Test
    @DisplayName("Удалённая каноническая operation всё равно отдаётся в archOperation")
    void deletedArchOperationStillReturned() {
        stubProductById();
        when(discoveredInterfaceRepository.findAllByProductAndSourceIgnoreCaseAndDeletedDateIsNull(
                any(Product.class), eq(SOURCE))).thenReturn(List.of(interfaceRow(5)));
        when(discoveredOperationRepository.findAllByInterfaceIdInAndDeletedDateIsNull(List.of(5)))
                .thenReturn(List.of(discoveredRow(20, 5, 100)));
        when(projectClient.findOperationsGroupedByUseCase(List.of(20)))
                .thenReturn(List.of(useCaseCard(7, List.of(20))));
        Operation deleted = operationRow(100, null);
        deleted.setDeletedDate(LocalDateTime.of(2026, 1, 1, 0, 0));
        when(operationRepository.findAllByIdIn(List.of(100))).thenReturn(List.of(deleted));

        assertThat(service.findGroupedByUseCase("1", null).get(0).getOperations().get(0).getArchOperation()).isNotNull();
        assertThat(service.findGroupedByUseCase("1", null).get(0).getOperations().get(0).getArchOperation().getDeletedDate())
                .isEqualTo(LocalDateTime.of(2026, 1, 1, 0, 0));
        verify(capabilityClient, never()).getTcs(anyList());
    }

    private void stubProductById() {
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product()));
    }

    private void stubHappyPathWithoutArch(Integer methodId) {
        when(discoveredInterfaceRepository.findAllByProductAndSourceIgnoreCaseAndDeletedDateIsNull(
                any(Product.class), eq(SOURCE))).thenReturn(List.of(interfaceRow(5)));
        when(discoveredOperationRepository.findAllByInterfaceIdInAndDeletedDateIsNull(List.of(5)))
                .thenReturn(List.of(discoveredRow(methodId, 5, null)));
    }

    private Product product() {
        return Product.builder().id(PRODUCT_ID).alias(ALIAS).name("Orders").build();
    }

    private DiscoveredInterface interfaceRow(Integer id) {
        return DiscoveredInterface.builder().id(id).source(SOURCE).product(product()).build();
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

    private UseCaseOperationsGroupedDTO useCaseCard(Integer id, List<Integer> operationIds) {
        return UseCaseOperationsGroupedDTO.builder()
                .id(id)
                .name("UC " + id)
                .code("UC-" + id)
                .operationIds(operationIds)
                .build();
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
