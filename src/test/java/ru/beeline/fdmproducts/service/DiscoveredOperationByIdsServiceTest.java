/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.beeline.fdmproducts.client.CapabilityClient;
import ru.beeline.fdmproducts.domain.ContainerProduct;
import ru.beeline.fdmproducts.domain.DiscoveredInterface;
import ru.beeline.fdmproducts.domain.DiscoveredOperation;
import ru.beeline.fdmproducts.domain.Interface;
import ru.beeline.fdmproducts.domain.Operation;
import ru.beeline.fdmproducts.domain.Product;
import ru.beeline.fdmproducts.domain.Sla;
import ru.beeline.fdmproducts.dto.TcDTO;
import ru.beeline.fdmproducts.dto.discovered.DiscoveredConnectedOperationDTO;
import ru.beeline.fdmproducts.dto.discovered.DiscoveredOperationByIdsDTO;
import ru.beeline.fdmproducts.exception.ExternalServiceException;
import ru.beeline.fdmproducts.mapper.DiscoveredOperationByIdsMapper;
import ru.beeline.fdmproducts.repository.ContainerRepository;
import ru.beeline.fdmproducts.repository.DiscoveredOperationRepository;
import ru.beeline.fdmproducts.repository.InterfaceRepository;
import ru.beeline.fdmproducts.repository.OperationRepository;
import ru.beeline.fdmproducts.repository.SlaRepository;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_GATEWAY;

@ExtendWith(MockitoExtension.class)
class DiscoveredOperationByIdsServiceTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final LocalDateTime CREATED = LocalDateTime.of(2026, 1, 15, 10, 0);

    @Mock
    private DiscoveredOperationRepository discoveredOperationRepository;
    @Mock
    private OperationRepository operationRepository;
    @Mock
    private SlaRepository slaRepository;
    @Mock
    private InterfaceRepository interfaceRepository;
    @Mock
    private ContainerRepository containerRepository;
    @Mock
    private CapabilityClient capabilityClient;
    @Spy
    private DiscoveredOperationByIdsMapper mapper = new DiscoveredOperationByIdsMapper();

    @InjectMocks
    private DiscoveredOperationByIdsService service;

    @Test
    @DisplayName("operation со sla, interface, container и techCapability указанного состава")
    void enrichesOperationWithSlaInterfaceContainerAndTechCapability() {
        DiscoveredInterface iface = interfaceRow(5);
        DiscoveredOperation discovered = discoveredRow(20, iface, 100, null);
        Operation operation = operationRow(100, 3, 8, null);
        TcDTO tc = TcDTO.builder().id(3).code("TC.01").name("Оформление заказа").build();
        Sla sla = slaRow(15, 100, 100.0, 50.0, 0.1);
        Interface archInterface = archInterfaceRow(8, "orders-rest", 4, null);
        ContainerProduct container = containerRow(4, "orders-api", null);

        when(discoveredOperationRepository.findAllByIdInWithInterface(List.of(20))).thenReturn(List.of(discovered));
        when(operationRepository.findAllByIdIn(List.of(100))).thenReturn(List.of(operation));
        when(slaRepository.findAllByOperationIdIn(List.of(100))).thenReturn(List.of(sla));
        when(interfaceRepository.findAllByIdIn(List.of(8))).thenReturn(List.of(archInterface));
        when(containerRepository.findAllByIdIn(List.of(4))).thenReturn(List.of(container));
        when(capabilityClient.getTcs(List.of(3))).thenReturn(List.of(tc));

        List<DiscoveredOperationByIdsDTO> result = service.getByIds(ids(20));

        assertThat(result).hasSize(1);
        DiscoveredOperationByIdsDTO dto = result.get(0);
        assertThat(dto.getDiscoveredInterface()).isNotNull();
        assertThat(dto.getDiscoveredInterface().getProduct().getAlias()).isEqualTo("MYAPP");

        DiscoveredConnectedOperationDTO op = dto.getOperation();
        assertThat(op).isNotNull();
        assertThat(op.getId()).isEqualTo(100);
        assertThat(op.getName()).isEqualTo("createOrder");
        assertThat(op.getType()).isEqualTo("POST");
        assertThat(op.getTcId()).isEqualTo(3);
        assertThat(op.getTechCapability()).isSameAs(tc);
        assertThat(op.getSla()).isNotNull();
        assertThat(op.getSla().getId()).isEqualTo(15);
        assertThat(op.getSla().getOperationId()).isEqualTo(100);
        assertThat(op.getSla().getRps()).isEqualTo(100.0);
        assertThat(op.getSla().getLatency()).isEqualTo(50.0);
        assertThat(op.getSla().getErrorRate()).isEqualTo(0.1);
        assertThat(op.getConnectedInterface()).isNotNull();
        assertThat(op.getConnectedInterface().getId()).isEqualTo(8);
        assertThat(op.getConnectedInterface().getCode()).isEqualTo("orders-rest");
        assertThat(op.getConnectedInterface().getContainer()).isNotNull();
        assertThat(op.getConnectedInterface().getContainer().getId()).isEqualTo(4);
        assertThat(op.getConnectedInterface().getContainer().getCode()).isEqualTo("orders-api");
    }

    @Test
    @DisplayName("operation без sla — sla = null")
    void operationWithoutSlaLeavesSlaNull() {
        stubConnectedWithoutSla();

        DiscoveredConnectedOperationDTO op = service.getByIds(ids(20)).get(0).getOperation();

        assertThat(op).isNotNull();
        assertThat(op.getSla()).isNull();
        assertThat(op.getConnectedInterface().getCode()).isEqualTo("orders-rest");
        assertThat(op.getConnectedInterface().getContainer().getCode()).isEqualTo("orders-api");
    }

    @Test
    @DisplayName("interface без container — container = null")
    void interfaceWithoutContainerLeavesContainerNull() {
        DiscoveredOperation discovered = discoveredRow(20, interfaceRow(5), 100, null);
        Operation operation = operationRow(100, 3, 8, null);
        Interface archInterface = archInterfaceRow(8, "orders-rest", null, null);
        TcDTO tc = TcDTO.builder().id(3).code("TC.01").name("Оформление заказа").build();

        when(discoveredOperationRepository.findAllByIdInWithInterface(List.of(20))).thenReturn(List.of(discovered));
        when(operationRepository.findAllByIdIn(List.of(100))).thenReturn(List.of(operation));
        when(slaRepository.findAllByOperationIdIn(List.of(100))).thenReturn(List.of());
        when(interfaceRepository.findAllByIdIn(List.of(8))).thenReturn(List.of(archInterface));
        when(capabilityClient.getTcs(List.of(3))).thenReturn(List.of(tc));

        DiscoveredConnectedOperationDTO op = service.getByIds(ids(20)).get(0).getOperation();

        assertThat(op.getConnectedInterface()).isNotNull();
        assertThat(op.getConnectedInterface().getContainer()).isNull();
        verify(containerRepository, never()).findAllByIdIn(anyList());
    }

    @Test
    @DisplayName("operation = null не читает sla и interface")
    void nullOperationDoesNotReadSlaOrInterface() {
        DiscoveredOperation discovered = discoveredRow(20, interfaceRow(5), null, null);
        when(discoveredOperationRepository.findAllByIdInWithInterface(List.of(20))).thenReturn(List.of(discovered));

        List<DiscoveredOperationByIdsDTO> result = service.getByIds(ids(20));

        assertThat(result.get(0).getOperation()).isNull();
        assertThat(result.get(0).getDiscoveredInterface()).isNotNull();
        verify(operationRepository, never()).findAllByIdIn(anyList());
        verify(slaRepository, never()).findAllByOperationIdIn(anyList());
        verify(interfaceRepository, never()).findAllByIdIn(anyList());
        verify(containerRepository, never()).findAllByIdIn(anyList());
        verify(capabilityClient, never()).getTcs(anyList());
    }

    @Test
    @DisplayName("Нет tc_id — capability не вызывается, techCapability = null")
    void operationWithoutTcIdDoesNotCallCapability() {
        DiscoveredOperation discovered = discoveredRow(20, interfaceRow(5), 100, null);
        Operation operation = operationRow(100, null, 8, null);
        Interface archInterface = archInterfaceRow(8, "orders-rest", 4, null);
        ContainerProduct container = containerRow(4, "orders-api", null);

        when(discoveredOperationRepository.findAllByIdInWithInterface(List.of(20))).thenReturn(List.of(discovered));
        when(operationRepository.findAllByIdIn(List.of(100))).thenReturn(List.of(operation));
        when(slaRepository.findAllByOperationIdIn(List.of(100))).thenReturn(List.of());
        when(interfaceRepository.findAllByIdIn(List.of(8))).thenReturn(List.of(archInterface));
        when(containerRepository.findAllByIdIn(List.of(4))).thenReturn(List.of(container));

        DiscoveredConnectedOperationDTO op = service.getByIds(ids(20)).get(0).getOperation();

        assertThat(op).isNotNull();
        assertThat(op.getTechCapability()).isNull();
        verify(capabilityClient, never()).getTcs(anyList());
    }

    @Test
    @DisplayName("Удалённые interface и container входят в ответ без лишних колонок")
    void deletedInterfaceAndContainerAreReturned() {
        LocalDateTime deleted = LocalDateTime.of(2026, 3, 1, 8, 0);
        DiscoveredOperation discovered = discoveredRow(20, interfaceRow(5), 100, null);
        Operation operation = operationRow(100, 3, 8, deleted);
        Interface archInterface = archInterfaceRow(8, "orders-rest", 4, deleted);
        ContainerProduct container = containerRow(4, "orders-api", deleted);
        TcDTO tc = TcDTO.builder().id(3).code("TC.01").name("Оформление заказа").build();

        when(discoveredOperationRepository.findAllByIdInWithInterface(List.of(20))).thenReturn(List.of(discovered));
        when(operationRepository.findAllByIdIn(List.of(100))).thenReturn(List.of(operation));
        when(slaRepository.findAllByOperationIdIn(List.of(100))).thenReturn(List.of());
        when(interfaceRepository.findAllByIdIn(List.of(8))).thenReturn(List.of(archInterface));
        when(containerRepository.findAllByIdIn(List.of(4))).thenReturn(List.of(container));
        when(capabilityClient.getTcs(List.of(3))).thenReturn(List.of(tc));

        DiscoveredConnectedOperationDTO op = service.getByIds(ids(20)).get(0).getOperation();

        assertThat(op).isNotNull();
        assertThat(op.getId()).isEqualTo(100);
        assertThat(op.getConnectedInterface().getId()).isEqualTo(8);
        assertThat(op.getConnectedInterface().getCode()).isEqualTo("orders-rest");
        assertThat(op.getConnectedInterface().getContainer().getId()).isEqualTo(4);
        assertThat(op.getConnectedInterface().getContainer().getCode()).isEqualTo("orders-api");
    }

    @Test
    @DisplayName("Две операции с одним connection_operation_id — один и тот же вложенный operation")
    void sameConnectionOperationIdSharesNestedOperation() {
        DiscoveredInterface iface = interfaceRow(5);
        DiscoveredOperation first = discoveredRow(20, iface, 100, null);
        DiscoveredOperation second = discoveredRow(21, iface, 100, null);
        Operation operation = operationRow(100, 3, 8, null);
        TcDTO tc = TcDTO.builder().id(3).code("TC.01").name("Оформление заказа").build();
        Sla sla = slaRow(15, 100, 100.0, 50.0, 0.1);
        Interface archInterface = archInterfaceRow(8, "orders-rest", 4, null);
        ContainerProduct container = containerRow(4, "orders-api", null);

        when(discoveredOperationRepository.findAllByIdInWithInterface(List.of(20, 21)))
                .thenReturn(List.of(first, second));
        when(operationRepository.findAllByIdIn(List.of(100))).thenReturn(List.of(operation));
        when(slaRepository.findAllByOperationIdIn(List.of(100))).thenReturn(List.of(sla));
        when(interfaceRepository.findAllByIdIn(List.of(8))).thenReturn(List.of(archInterface));
        when(containerRepository.findAllByIdIn(List.of(4))).thenReturn(List.of(container));
        when(capabilityClient.getTcs(List.of(3))).thenReturn(List.of(tc));

        List<DiscoveredOperationByIdsDTO> result = service.getByIds(ids(20, 21));

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getOperation()).isSameAs(result.get(1).getOperation());
        assertThat(result.get(0).getOperation().getSla().getId()).isEqualTo(15);
        assertThat(result.get(0).getOperation().getTechCapability()).isSameAs(tc);
        verify(operationRepository).findAllByIdIn(List.of(100));
        verify(capabilityClient).getTcs(List.of(3));
    }

    @Test
    @DisplayName("Несколько sla — строка с наименьшим id")
    void prefersSlaWithSmallestId() {
        DiscoveredOperation discovered = discoveredRow(20, interfaceRow(5), 100, null);
        Operation operation = operationRow(100, null, null, null);
        Sla older = slaRow(20, 100, 200.0, 10.0, 0.2);
        Sla newer = slaRow(5, 100, 100.0, 50.0, 0.1);

        when(discoveredOperationRepository.findAllByIdInWithInterface(List.of(20))).thenReturn(List.of(discovered));
        when(operationRepository.findAllByIdIn(List.of(100))).thenReturn(List.of(operation));
        when(slaRepository.findAllByOperationIdIn(List.of(100))).thenReturn(List.of(older, newer));

        DiscoveredConnectedOperationDTO op = service.getByIds(ids(20)).get(0).getOperation();

        assertThat(op.getSla().getId()).isEqualTo(5);
        assertThat(op.getSla().getRps()).isEqualTo(100.0);
        assertThat(op.getConnectedInterface()).isNull();
        verify(interfaceRepository, never()).findAllByIdIn(anyList());
        verify(capabilityClient, never()).getTcs(anyList());
    }

    @Test
    @DisplayName("Строки operation нет — operation = null, sla/interface не читаются")
    void missingOperationRowLeavesNullAndSkipsEnrichment() {
        DiscoveredOperation discovered = discoveredRow(20, interfaceRow(5), 100, null);
        when(discoveredOperationRepository.findAllByIdInWithInterface(List.of(20))).thenReturn(List.of(discovered));
        when(operationRepository.findAllByIdIn(List.of(100))).thenReturn(List.of());

        List<DiscoveredOperationByIdsDTO> result = service.getByIds(ids(20));

        assertThat(result.get(0).getOperation()).isNull();
        verify(slaRepository, never()).findAllByOperationIdIn(anyList());
        verify(interfaceRepository, never()).findAllByIdIn(anyList());
        verify(capabilityClient, never()).getTcs(anyList());
    }

    @Test
    @DisplayName("Неизвестный id пропускается, дубликат схлопывается, удалённые строки входят в ответ")
    void skipsUnknownDeduplicatesAndKeepsDeleted() {
        LocalDateTime deleted = LocalDateTime.of(2026, 3, 1, 8, 0);
        DiscoveredOperation deletedRow = discoveredRow(20, interfaceRow(5), null, deleted);
        when(discoveredOperationRepository.findAllByIdInWithInterface(List.of(20, 21)))
                .thenReturn(List.of(deletedRow));

        List<DiscoveredOperationByIdsDTO> result = service.getByIds(ids(20, 21, 20));

        assertThat(result).extracting(DiscoveredOperationByIdsDTO::getId).containsExactly(20);
        assertThat(result.get(0).getDeletedDate()).isEqualTo(deleted);
        verify(discoveredOperationRepository).findAllByIdInWithInterface(List.of(20, 21));
        verify(capabilityClient, never()).getTcs(anyList());
    }

    @Test
    @DisplayName("Нет тела, не массив, пустой массив, null в массиве — 400 с оговорённым текстом")
    void validatesRequestBody() throws Exception {
        assertThatThrownBy(() -> service.getByIds(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Отсутствует тело запроса");
        assertThatThrownBy(() -> service.getByIds(MAPPER.readTree("{}")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Отсутствует тело запроса");
        assertThatThrownBy(() -> service.getByIds(ids()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Список идентификаторов методов не может быть пустым");
        assertThatThrownBy(() -> service.getByIds(ids(20, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Идентификатор метода не может быть пустым");
        assertThatThrownBy(() -> service.getByIds(MAPPER.readTree("[20, \"abc\"]")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Идентификатор метода не может быть пустым");
    }

    @Test
    @DisplayName("Пустой ответ capability — ТС не сопоставляются, 502 нет")
    void emptyCapabilityResponseLeavesTechCapabilityNull() {
        DiscoveredOperation discovered = discoveredRow(20, interfaceRow(5), 100, null);
        when(discoveredOperationRepository.findAllByIdInWithInterface(List.of(20))).thenReturn(List.of(discovered));
        when(operationRepository.findAllByIdIn(List.of(100))).thenReturn(List.of(operationRow(100, 3, null, null)));
        when(slaRepository.findAllByOperationIdIn(List.of(100))).thenReturn(List.of());
        when(capabilityClient.getTcs(List.of(3))).thenReturn(List.of());

        List<DiscoveredOperationByIdsDTO> result = service.getByIds(ids(20));

        assertThat(result.get(0).getOperation()).isNotNull();
        assertThat(result.get(0).getOperation().getTechCapability()).isNull();
    }

    @Test
    @DisplayName("Исключение при запросе capability — 502")
    void capabilityExceptionIsBadGateway() {
        DiscoveredOperation discovered = discoveredRow(20, interfaceRow(5), 100, null);
        when(discoveredOperationRepository.findAllByIdInWithInterface(List.of(20))).thenReturn(List.of(discovered));
        when(operationRepository.findAllByIdIn(List.of(100))).thenReturn(List.of(operationRow(100, 3, null, null)));
        when(slaRepository.findAllByOperationIdIn(List.of(100))).thenReturn(List.of());
        when(capabilityClient.getTcs(List.of(3))).thenThrow(new RuntimeException("capability down"));

        assertThatThrownBy(() -> service.getByIds(ids(20)))
                .isInstanceOf(ExternalServiceException.class)
                .hasMessage("Сервис capability недоступен")
                .extracting(ex -> ((ExternalServiceException) ex).getStatus())
                .isEqualTo(BAD_GATEWAY);
    }

    @Test
    @DisplayName("Строки интерфейса нет — discoveredInterface = null; ТС без совпадения — techCapability = null")
    void missingInterfaceAndUnmatchedTechCapabilityAreNull() {
        DiscoveredOperation discovered = discoveredRow(20, null, 100, null);
        discovered.setInterfaceId(5);
        Operation operation = operationRow(100, 3, null, null);
        TcDTO otherTc = TcDTO.builder().id(9).code("OTHER").name("Другая").build();

        when(discoveredOperationRepository.findAllByIdInWithInterface(List.of(20))).thenReturn(List.of(discovered));
        when(operationRepository.findAllByIdIn(List.of(100))).thenReturn(List.of(operation));
        when(slaRepository.findAllByOperationIdIn(List.of(100))).thenReturn(List.of());
        when(capabilityClient.getTcs(List.of(3))).thenReturn(List.of(otherTc));

        List<DiscoveredOperationByIdsDTO> result = service.getByIds(ids(20));

        assertThat(result.get(0).getDiscoveredInterface()).isNull();
        assertThat(result.get(0).getOperation().getTechCapability()).isNull();
        assertThat(result.get(0).getOperation().getConnectedInterface()).isNull();
    }

    private void stubConnectedWithoutSla() {
        DiscoveredOperation discovered = discoveredRow(20, interfaceRow(5), 100, null);
        Operation operation = operationRow(100, 3, 8, null);
        Interface archInterface = archInterfaceRow(8, "orders-rest", 4, null);
        ContainerProduct container = containerRow(4, "orders-api", null);
        TcDTO tc = TcDTO.builder().id(3).code("TC.01").name("Оформление заказа").build();

        when(discoveredOperationRepository.findAllByIdInWithInterface(List.of(20))).thenReturn(List.of(discovered));
        when(operationRepository.findAllByIdIn(List.of(100))).thenReturn(List.of(operation));
        when(slaRepository.findAllByOperationIdIn(List.of(100))).thenReturn(List.of());
        when(interfaceRepository.findAllByIdIn(List.of(8))).thenReturn(List.of(archInterface));
        when(containerRepository.findAllByIdIn(List.of(4))).thenReturn(List.of(container));
        when(capabilityClient.getTcs(List.of(3))).thenReturn(List.of(tc));
    }

    private JsonNode ids(Integer... values) {
        ArrayNode node = MAPPER.createArrayNode();
        for (Integer value : values) {
            if (value == null) {
                node.addNull();
            } else {
                node.add(value);
            }
        }
        return node;
    }

    private DiscoveredInterface interfaceRow(Integer id) {
        return DiscoveredInterface.builder()
                .id(id)
                .name("orders-api")
                .externalId("orders-api")
                .source("UseCase")
                .product(Product.builder()
                        .id(3)
                        .alias("MYAPP")
                        .name("Заказы")
                        .description("Продукт заказов")
                        .build())
                .createdDate(CREATED)
                .build();
    }

    private DiscoveredOperation discoveredRow(Integer id, DiscoveredInterface iface,
                                              Integer connectionOperationId, LocalDateTime deletedDate) {
        return DiscoveredOperation.builder()
                .id(id)
                .discoveredInterface(iface)
                .interfaceId(iface == null ? null : iface.getId())
                .connectionOperationId(connectionOperationId)
                .name("createOrder")
                .context("/orders")
                .description("Создание заказа")
                .type("POST")
                .returnType("Order")
                .isPlanned(false)
                .createdDate(CREATED)
                .deletedDate(deletedDate)
                .build();
    }

    private Operation operationRow(Integer id, Integer tcId, Integer interfaceId, LocalDateTime deletedDate) {
        return Operation.builder()
                .id(id)
                .interfaceId(interfaceId)
                .name("createOrder")
                .description("Каноническое создание заказа")
                .type("POST")
                .returnType("Order")
                .tcId(tcId)
                .isDeletedTc(false)
                .createdDate(LocalDateTime.of(2025, 11, 1, 9, 0))
                .deletedDate(deletedDate)
                .build();
    }

    private Sla slaRow(Integer id, Integer operationId, Double rps, Double latency, Double errorRate) {
        return Sla.builder()
                .id(id)
                .operationId(operationId)
                .rps(rps)
                .latency(latency)
                .errorRate(errorRate)
                .build();
    }

    private Interface archInterfaceRow(Integer id, String code, Integer containerId, LocalDateTime deletedDate) {
        Interface iface = new Interface();
        iface.setId(id);
        iface.setCode(code);
        iface.setContainerId(containerId);
        iface.setDeletedDate(deletedDate);
        return iface;
    }

    private ContainerProduct containerRow(Integer id, String code, LocalDateTime deletedDate) {
        ContainerProduct container = new ContainerProduct();
        container.setId(id);
        container.setCode(code);
        if (deletedDate != null) {
            container.setDeletedDate(java.sql.Timestamp.valueOf(deletedDate));
        }
        return container;
    }
}
