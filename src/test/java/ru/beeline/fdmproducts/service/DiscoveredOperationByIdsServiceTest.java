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
import ru.beeline.fdmproducts.domain.DiscoveredInterface;
import ru.beeline.fdmproducts.domain.DiscoveredOperation;
import ru.beeline.fdmproducts.domain.Operation;
import ru.beeline.fdmproducts.domain.Product;
import ru.beeline.fdmproducts.dto.TcDTO;
import ru.beeline.fdmproducts.dto.discovered.DiscoveredOperationByIdsDTO;
import ru.beeline.fdmproducts.exception.ExternalServiceException;
import ru.beeline.fdmproducts.mapper.DiscoveredOperationByIdsMapper;
import ru.beeline.fdmproducts.repository.DiscoveredOperationRepository;
import ru.beeline.fdmproducts.repository.OperationRepository;

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
    private CapabilityClient capabilityClient;
    @Spy
    private DiscoveredOperationByIdsMapper mapper = new DiscoveredOperationByIdsMapper();

    @InjectMocks
    private DiscoveredOperationByIdsService service;

    @Test
    @DisplayName("Обогащает интерфейсом, операцией и ТС")
    void enrichesInterfaceOperationAndTechCapability() {
        DiscoveredInterface iface = interfaceRow(5);
        DiscoveredOperation discovered = discoveredRow(20, iface, 100, null);
        Operation operation = operationRow(100, 3, null);
        TcDTO tc = TcDTO.builder().id(3).code("TC.01").name("Оформление заказа").build();

        when(discoveredOperationRepository.findAllByIdInWithInterface(List.of(20))).thenReturn(List.of(discovered));
        when(operationRepository.findAllByIdIn(List.of(100))).thenReturn(List.of(operation));
        when(capabilityClient.getTcs(List.of(3))).thenReturn(List.of(tc));

        List<DiscoveredOperationByIdsDTO> result = service.getByIds(ids(20));

        assertThat(result).hasSize(1);
        DiscoveredOperationByIdsDTO dto = result.get(0);
        assertThat(dto.getId()).isEqualTo(20);
        assertThat(dto.getInterfaceId()).isEqualTo(5);
        assertThat(dto.getDiscoveredInterface()).isNotNull();
        assertThat(dto.getDiscoveredInterface().getId()).isEqualTo(5);
        assertThat(dto.getDiscoveredInterface().getProductId()).isEqualTo(3);
        assertThat(dto.getDiscoveredInterface().getProduct()).isNotNull();
        assertThat(dto.getDiscoveredInterface().getProduct().getId()).isEqualTo(3);
        assertThat(dto.getDiscoveredInterface().getProduct().getAlias()).isEqualTo("MYAPP");
        assertThat(dto.getDiscoveredInterface().getProduct().getName()).isEqualTo("Заказы");
        assertThat(dto.getDiscoveredInterface().getProduct().getDescription()).isEqualTo("Продукт заказов");
        assertThat(dto.getDiscoveredInterface().getSource()).isEqualTo("UseCase");
        assertThat(dto.getOperation()).isNotNull();
        assertThat(dto.getOperation().getId()).isEqualTo(100);
        assertThat(dto.getOperation().getTcId()).isEqualTo(3);
        assertThat(dto.getOperation().getTechCapability()).isSameAs(tc);
        assertThat(dto.getDeletedDate()).isNull();
    }

    @Test
    @DisplayName("Пустой connection_operation_id — operation = null, capability не вызывается")
    void emptyConnectionLeavesOperationNullAndSkipsCapability() {
        DiscoveredOperation discovered = discoveredRow(20, interfaceRow(5), null, null);
        when(discoveredOperationRepository.findAllByIdInWithInterface(List.of(20))).thenReturn(List.of(discovered));

        List<DiscoveredOperationByIdsDTO> result = service.getByIds(ids(20));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getOperation()).isNull();
        assertThat(result.get(0).getDiscoveredInterface()).isNotNull();
        verify(operationRepository, never()).findAllByIdIn(anyList());
        verify(capabilityClient, never()).getTcs(anyList());
    }

    @Test
    @DisplayName("Один ТС ставится под каждую operation с этим tc_id")
    void sameTechCapabilityAttachedToTwoOperations() {
        DiscoveredInterface iface = interfaceRow(5);
        DiscoveredOperation first = discoveredRow(20, iface, 100, null);
        DiscoveredOperation second = discoveredRow(21, iface, 101, null);
        Operation op100 = operationRow(100, 3, null);
        Operation op101 = operationRow(101, 3, null);
        TcDTO tc = TcDTO.builder().id(3).code("TC.01").name("Оформление заказа").build();

        when(discoveredOperationRepository.findAllByIdInWithInterface(List.of(20, 21)))
                .thenReturn(List.of(first, second));
        when(operationRepository.findAllByIdIn(List.of(100, 101))).thenReturn(List.of(op100, op101));
        when(capabilityClient.getTcs(List.of(3))).thenReturn(List.of(tc));

        List<DiscoveredOperationByIdsDTO> result = service.getByIds(ids(20, 21));

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getOperation().getTechCapability()).isSameAs(tc);
        assertThat(result.get(1).getOperation().getTechCapability()).isSameAs(tc);
        verify(capabilityClient).getTcs(List.of(3));
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
    @DisplayName("Нет tc_id у operation — capability не вызывается")
    void operationWithoutTcIdDoesNotCallCapability() {
        DiscoveredOperation discovered = discoveredRow(20, interfaceRow(5), 100, null);
        when(discoveredOperationRepository.findAllByIdInWithInterface(List.of(20))).thenReturn(List.of(discovered));
        when(operationRepository.findAllByIdIn(List.of(100))).thenReturn(List.of(operationRow(100, null, null)));

        List<DiscoveredOperationByIdsDTO> result = service.getByIds(ids(20));

        assertThat(result.get(0).getOperation()).isNotNull();
        assertThat(result.get(0).getOperation().getTechCapability()).isNull();
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
        when(operationRepository.findAllByIdIn(List.of(100))).thenReturn(List.of(operationRow(100, 3, null)));
        when(capabilityClient.getTcs(List.of(3))).thenReturn(List.of());

        List<DiscoveredOperationByIdsDTO> result = service.getByIds(ids(20));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getOperation()).isNotNull();
        assertThat(result.get(0).getOperation().getTechCapability()).isNull();
    }

    @Test
    @DisplayName("Исключение при запросе capability — 502")
    void capabilityExceptionIsBadGateway() {
        DiscoveredOperation discovered = discoveredRow(20, interfaceRow(5), 100, null);
        when(discoveredOperationRepository.findAllByIdInWithInterface(List.of(20))).thenReturn(List.of(discovered));
        when(operationRepository.findAllByIdIn(List.of(100))).thenReturn(List.of(operationRow(100, 3, null)));
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
        Operation operation = operationRow(100, 3, null);
        TcDTO otherTc = TcDTO.builder().id(9).code("OTHER").name("Другая").build();

        when(discoveredOperationRepository.findAllByIdInWithInterface(List.of(20))).thenReturn(List.of(discovered));
        when(operationRepository.findAllByIdIn(List.of(100))).thenReturn(List.of(operation));
        when(capabilityClient.getTcs(List.of(3))).thenReturn(List.of(otherTc));

        List<DiscoveredOperationByIdsDTO> result = service.getByIds(ids(20));

        assertThat(result.get(0).getDiscoveredInterface()).isNull();
        assertThat(result.get(0).getOperation().getTechCapability()).isNull();
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

    private Operation operationRow(Integer id, Integer tcId, LocalDateTime deletedDate) {
        return Operation.builder()
                .id(id)
                .interfaceId(8)
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
}
