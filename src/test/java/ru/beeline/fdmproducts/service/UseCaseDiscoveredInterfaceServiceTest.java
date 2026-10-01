/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import ru.beeline.fdmproducts.client.ProjectClient;
import ru.beeline.fdmproducts.domain.DiscoveredInterface;
import ru.beeline.fdmproducts.domain.DiscoveredOperation;
import ru.beeline.fdmproducts.domain.Operation;
import ru.beeline.fdmproducts.domain.Product;
import ru.beeline.fdmproducts.dto.discovered.UseCaseDiscoveredInterfaceDTO;
import ru.beeline.fdmproducts.dto.discovered.UseCaseDiscoveredInterfaceResultDTO;
import ru.beeline.fdmproducts.dto.discovered.UseCaseDiscoveredOperationDTO;
import ru.beeline.fdmproducts.dto.project.OperationUseCaseDTO;
import ru.beeline.fdmproducts.dto.project.OperationUseCasesResponseDTO;
import ru.beeline.fdmproducts.exception.ExternalServiceException;
import ru.beeline.fdmproducts.repository.DiscoveredInterfaceRepository;
import ru.beeline.fdmproducts.repository.DiscoveredOperationRepository;
import ru.beeline.fdmproducts.repository.OperationRepository;
import ru.beeline.fdmproducts.repository.ProductRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UseCaseDiscoveredInterfaceServiceTest {

    private static final String USE_CASE_ID = "42";
    private static final Integer USE_CASE_ID_INT = 42;
    private static final Integer OTHER_USE_CASE_ID = 99;
    private static final String ALIAS = "my-product";
    private static final Integer PRODUCT_ID = 1;
    private static final Integer NEW_INTERFACE_ID = 500;
    private static final Integer STORED_INTERFACE_ID = 700;

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

    @InjectMocks
    private UseCaseDiscoveredInterfaceService service;

    private final Product product = Product.builder().id(PRODUCT_ID).alias(ALIAS).build();
    private final AtomicInteger operationIdSequence = new AtomicInteger(900);

    @BeforeEach
    void setUp() {
        when(productRepository.findByAliasInIgnoreCase(anyList())).thenReturn(List.of(product));
        when(discoveredInterfaceRepository.findAllBySourceAndExternalIdInIgnoreCase(anyString(), any()))
                .thenReturn(List.of());
        when(discoveredOperationRepository.findAllByInterfaceIdIn(anyList())).thenReturn(List.of());
        when(discoveredInterfaceRepository.save(any(DiscoveredInterface.class))).thenAnswer(invocation -> {
            DiscoveredInterface saved = invocation.getArgument(0);
            if (saved.getId() == null) {
                saved.setId(NEW_INTERFACE_ID);
            }
            return saved;
        });
        when(discoveredOperationRepository.save(any(DiscoveredOperation.class))).thenAnswer(invocation -> {
            DiscoveredOperation saved = invocation.getArgument(0);
            if (saved.getId() == null) {
                saved.setId(operationIdSequence.incrementAndGet());
            }
            return saved;
        });
    }

    @Test
    @DisplayName("Создание: интерфейс с source=UseCase, product_planned и методы с is_planned")
    void createsInterfaceAndOperations() {
        UseCaseDiscoveredInterfaceDTO body = UseCaseDiscoveredInterfaceDTO.builder()
                .interfaceCode("orders-api")
                .product(ALIAS)
                .operations(new ArrayList<>(List.of(
                        UseCaseDiscoveredOperationDTO.builder()
                                .name("getOrder")
                                .type("REST")
                                .descriptionTc("описание ТС")
                                .tcCode("TC-1")
                                .descriptionOperation("описание метода")
                                .isPlanned(true)
                                .build(),
                        UseCaseDiscoveredOperationDTO.builder().name("putOrder").type("REST").build())))
                .build();

        List<UseCaseDiscoveredInterfaceResultDTO> result =
                service.syncUseCaseOperations(USE_CASE_ID, List.of(body));

        assertThat(result).hasSize(1);
        UseCaseDiscoveredInterfaceResultDTO iface = result.get(0);
        assertThat(iface.getInterfaceCode()).isEqualTo("orders-api");
        assertThat(iface.getProduct()).isEqualTo(ALIAS);
        assertThat(iface.getProductPlanned()).isNull();
        assertThat(iface.getOperations()).extracting(op -> op.getDiscoveredOperationId())
                .containsExactly(901, 902);
        assertThat(iface.getOperations()).extracting(op -> op.getIsPlanned())
                .containsExactly(true, false);

        ArgumentCaptor<DiscoveredInterface> ifaceCaptor = ArgumentCaptor.forClass(DiscoveredInterface.class);
        verify(discoveredInterfaceRepository).save(ifaceCaptor.capture());
        DiscoveredInterface savedInterface = ifaceCaptor.getValue();
        assertThat(savedInterface.getName()).isEqualTo("orders-api");
        assertThat(savedInterface.getExternalId()).isEqualTo("orders-api");
        assertThat(savedInterface.getSource()).isEqualTo("UseCase");
        assertThat(savedInterface.getProduct().getId()).isEqualTo(PRODUCT_ID);
        assertThat(savedInterface.getProductPlanned()).isNull();
        assertThat(savedInterface.getCreatedDate()).isNotNull();
        assertThat(savedInterface.getDeletedDate()).isNull();

        ArgumentCaptor<DiscoveredOperation> opCaptor = ArgumentCaptor.forClass(DiscoveredOperation.class);
        verify(discoveredOperationRepository, times(2)).save(opCaptor.capture());
        DiscoveredOperation first = opCaptor.getAllValues().get(0);
        assertThat(first.getDescription()).isEqualTo("описание метода");
        assertThat(first.getTcCode()).isEqualTo("TC-1");
        assertThat(first.getTcDescription()).isEqualTo("описание ТС");
        assertThat(first.getIsPlanned()).isTrue();
        assertThat(opCaptor.getAllValues().get(1).getIsPlanned()).isFalse();
        assertThat(first.getConnectionOperationId()).isNull();
        verify(operationRepository, never()).findAllByIdIn(anyList());
        verify(projectClient, never()).findUseCasesByOperations(anyList());
    }

    @Test
    @DisplayName("Создание с connectionOperationId пишет связь в discovered_operation")
    void createWritesConnectionOperationId() {
        Integer archOpId = 123;
        when(operationRepository.findAllByIdIn(List.of(archOpId)))
                .thenReturn(List.of(Operation.builder().id(archOpId).build()));
        UseCaseDiscoveredInterfaceDTO body = request("orders-api");
        body.getOperations().get(0).setConnectionOperationId(archOpId);

        service.syncUseCaseOperations(USE_CASE_ID, List.of(body));

        ArgumentCaptor<DiscoveredOperation> captor = ArgumentCaptor.forClass(DiscoveredOperation.class);
        verify(discoveredOperationRepository).save(captor.capture());
        assertThat(captor.getValue().getConnectionOperationId()).isEqualTo(archOpId);
    }

    @Test
    @DisplayName("Обновление с connectionOperationId перезаписывает связь")
    void updateWritesConnectionOperationId() {
        Integer archOpId = 456;
        when(operationRepository.findAllByIdIn(List.of(archOpId)))
                .thenReturn(List.of(Operation.builder().id(archOpId).build()));
        DiscoveredInterface stored = storedInterface("orders-api", null);
        when(discoveredInterfaceRepository.findAllBySourceAndExternalIdInIgnoreCase(eq("UseCase"), any()))
                .thenReturn(List.of(stored));
        DiscoveredOperation existing = storedOperation(777, "getorder", "rest");
        existing.setConnectionOperationId(111);
        when(discoveredOperationRepository.findAllByInterfaceIdIn(anyList())).thenReturn(List.of(existing));

        UseCaseDiscoveredInterfaceDTO body = request("orders-api");
        body.getOperations().get(0).setConnectionOperationId(archOpId);

        service.syncUseCaseOperations(USE_CASE_ID, List.of(body));

        assertThat(existing.getConnectionOperationId()).isEqualTo(archOpId);
        assertThat(existing.getUpdatedDate()).isNotNull();
        verify(discoveredOperationRepository).save(existing);
    }

    @Test
    @DisplayName("Обновление без connectionOperationId не затирает ранее записанную связь")
    void updateWithoutConnectionOperationIdKeepsExistingLink() {
        Integer previousLink = 789;
        DiscoveredInterface stored = storedInterface("orders-api", null);
        when(discoveredInterfaceRepository.findAllBySourceAndExternalIdInIgnoreCase(eq("UseCase"), any()))
                .thenReturn(List.of(stored));
        DiscoveredOperation existing = storedOperation(777, "getorder", "rest");
        existing.setConnectionOperationId(previousLink);
        existing.setDescription("старое");
        when(discoveredOperationRepository.findAllByInterfaceIdIn(anyList())).thenReturn(List.of(existing));

        UseCaseDiscoveredInterfaceDTO body = request("orders-api");
        body.getOperations().get(0).setDescriptionOperation("новое");

        service.syncUseCaseOperations(USE_CASE_ID, List.of(body));

        assertThat(existing.getConnectionOperationId()).isEqualTo(previousLink);
        assertThat(existing.getDescription()).isEqualTo("новое");
        verify(operationRepository, never()).findAllByIdIn(anyList());
    }

    @Test
    @DisplayName("Несуществующий connectionOperationId — 400, запись не создаётся")
    void rejectsUnknownConnectionOperationId() {
        when(operationRepository.findAllByIdIn(List.of(999_999))).thenReturn(List.of());
        UseCaseDiscoveredInterfaceDTO body = request("orders-api");
        body.getOperations().get(0).setConnectionOperationId(999_999);

        assertThatThrownBy(() -> service.syncUseCaseOperations(USE_CASE_ID, List.of(body)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Для одной из операцией не существует id = connectionOperationId заявленной сопоставленной операции в архитектуре");
        verify(discoveredInterfaceRepository, never()).save(any());
        verify(discoveredOperationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Удалённая operation подходит для connectionOperationId: связь записывается")
    void acceptsDeletedArchitectureOperation() {
        Integer archOpId = 321;
        when(operationRepository.findAllByIdIn(List.of(archOpId)))
                .thenReturn(List.of(Operation.builder()
                        .id(archOpId)
                        .deletedDate(LocalDateTime.now().minusDays(1))
                        .build()));
        UseCaseDiscoveredInterfaceDTO body = request("orders-api");
        body.getOperations().get(0).setConnectionOperationId(archOpId);

        service.syncUseCaseOperations(USE_CASE_ID, List.of(body));

        ArgumentCaptor<DiscoveredOperation> captor = ArgumentCaptor.forClass(DiscoveredOperation.class);
        verify(discoveredOperationRepository).save(captor.capture());
        assertThat(captor.getValue().getConnectionOperationId()).isEqualTo(archOpId);
    }

    @Test
    @DisplayName("Обновление: удалённый метод оживает, is_planned меняется, productPlanned сбрасывается")
    void updatesExistingInterfaceAndOperation() {
        DiscoveredInterface stored = storedInterface("orders-api", "старый-план");
        when(discoveredInterfaceRepository.findAllBySourceAndExternalIdInIgnoreCase(eq("UseCase"), any()))
                .thenReturn(List.of(stored));
        DiscoveredOperation existing = storedOperation(777, "getorder", "rest");
        existing.setDescription("старое описание");
        existing.setDeletedDate(LocalDateTime.now().minusDays(1));
        when(discoveredOperationRepository.findAllByInterfaceIdIn(anyList())).thenReturn(List.of(existing));

        UseCaseDiscoveredInterfaceDTO body = UseCaseDiscoveredInterfaceDTO.builder()
                .interfaceCode("orders-api")
                .product(ALIAS)
                .operations(List.of(UseCaseDiscoveredOperationDTO.builder()
                        .name("getOrder").type("REST").descriptionOperation("новое описание").isPlanned(true).build()))
                .build();

        List<UseCaseDiscoveredInterfaceResultDTO> result =
                service.syncUseCaseOperations(USE_CASE_ID, List.of(body));

        assertThat(result.get(0).getOperations().get(0).getDiscoveredOperationId()).isEqualTo(777);
        assertThat(stored.getProductPlanned()).isNull();
        assertThat(stored.getDeletedDate()).isNull();
        assertThat(stored.getUpdatedDate()).isNotNull();
        assertThat(existing.getDescription()).isEqualTo("новое описание");
        assertThat(existing.getIsPlanned()).isTrue();
        assertThat(existing.getDeletedDate()).isNull();
        assertThat(existing.getUpdatedDate()).isNotNull();

        ArgumentCaptor<DiscoveredInterface> captor = ArgumentCaptor.forClass(DiscoveredInterface.class);
        verify(discoveredInterfaceRepository).save(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(STORED_INTERFACE_ID);
    }

    @Test
    @DisplayName("Продукта нет в каталоге: 400")
    void failsWhenProductNotFound() {
        when(productRepository.findByAliasInIgnoreCase(anyList())).thenReturn(List.of());

        UseCaseDiscoveredInterfaceDTO body = UseCaseDiscoveredInterfaceDTO.builder()
                .interfaceCode("new-api")
                .product("unknown-product")
                .operations(List.of(UseCaseDiscoveredOperationDTO.builder().name("op").type("REST").build()))
                .build();

        assertThatThrownBy(() -> service.syncUseCaseOperations(USE_CASE_ID, List.of(body)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Продукт не найден")
                .hasMessageContaining("unknown-product");
        verify(discoveredInterfaceRepository, never()).save(any());
    }

    @Test
    @DisplayName("Без product_id интерфейс ищется по product_planned без учёта регистра")
    void matchesStoredInterfaceByProductPlanned() {
        when(productRepository.findByAliasInIgnoreCase(anyList())).thenReturn(List.of());
        DiscoveredInterface stored = DiscoveredInterface.builder()
                .id(STORED_INTERFACE_ID)
                .externalId("new-api")
                .source("UseCase")
                .productPlanned("PLANNED")
                .build();
        when(discoveredInterfaceRepository.findAllBySourceAndExternalIdInIgnoreCase(eq("UseCase"), any()))
                .thenReturn(List.of(stored));

        UseCaseDiscoveredInterfaceDTO body = UseCaseDiscoveredInterfaceDTO.builder()
                .interfaceCode("new-api")
                .productPlanned("planned")
                .operations(List.of())
                .build();

        service.syncUseCaseOperations(USE_CASE_ID, List.of(body));

        assertThat(stored.getUpdatedDate()).isNotNull();
        assertThat(stored.getProductPlanned()).isEqualTo("planned");
        ArgumentCaptor<DiscoveredInterface> captor = ArgumentCaptor.forClass(DiscoveredInterface.class);
        verify(discoveredInterfaceRepository).save(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(STORED_INTERFACE_ID);
    }

    @Test
    @DisplayName("Несколько живых интерфейсов по ключу — 400")
    void failsOnMultipleLiveInterfaces() {
        DiscoveredInterface first = storedInterface("orders-api", null);
        DiscoveredInterface second = storedInterface("orders-api", null);
        when(discoveredInterfaceRepository.findAllBySourceAndExternalIdInIgnoreCase(eq("UseCase"), any()))
                .thenReturn(List.of(first, second));

        UseCaseDiscoveredInterfaceDTO body = UseCaseDiscoveredInterfaceDTO.builder()
                .interfaceCode("orders-api")
                .product(ALIAS)
                .operations(List.of())
                .build();

        assertThatThrownBy(() -> service.syncUseCaseOperations(USE_CASE_ID, List.of(body)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("несколько");
    }

    @Test
    @DisplayName("Кандидат только из текущего use case удаляется; чужой и неоднозначный остаются живыми")
    void softDeletesOnlyCandidatesOfCurrentUseCase() {
        DiscoveredInterface stored = storedInterface("orders-api", null);
        when(discoveredInterfaceRepository.findAllBySourceAndExternalIdInIgnoreCase(eq("UseCase"), any()))
                .thenReturn(List.of(stored));
        DiscoveredOperation ownCandidate = storedOperation(801, "own", "REST");
        DiscoveredOperation foreignCandidate = storedOperation(802, "foreign", "REST");
        DiscoveredOperation ambiguousCandidate = storedOperation(803, "ambiguous", "REST");
        when(discoveredOperationRepository.findAllByInterfaceIdIn(anyList()))
                .thenReturn(List.of(ownCandidate, foreignCandidate, ambiguousCandidate));
        when(projectClient.findUseCasesByOperations(anyList())).thenReturn(List.of(
                response(801, List.of(useCase(USE_CASE_ID_INT))),
                response(802, List.of(useCase(OTHER_USE_CASE_ID))),
                response(803, List.of(useCase(USE_CASE_ID_INT), useCase(OTHER_USE_CASE_ID)))));

        UseCaseDiscoveredInterfaceDTO body = UseCaseDiscoveredInterfaceDTO.builder()
                .interfaceCode("orders-api")
                .product(ALIAS)
                .operations(List.of())
                .build();

        service.syncUseCaseOperations(USE_CASE_ID, List.of(body));

        assertThat(ownCandidate.getDeletedDate()).isNotNull();
        assertThat(ownCandidate.getUpdatedDate()).isNotNull();
        assertThat(foreignCandidate.getDeletedDate()).isNull();
        assertThat(ambiguousCandidate.getDeletedDate()).isNull();
    }

    @Test
    @DisplayName("Кандидат с пустым списком use case не удаляется")
    void keepsCandidateWithoutUseCases() {
        DiscoveredInterface stored = storedInterface("orders-api", null);
        when(discoveredInterfaceRepository.findAllBySourceAndExternalIdInIgnoreCase(eq("UseCase"), any()))
                .thenReturn(List.of(stored));
        DiscoveredOperation candidate = storedOperation(801, "orphan", "REST");
        when(discoveredOperationRepository.findAllByInterfaceIdIn(anyList())).thenReturn(List.of(candidate));
        when(projectClient.findUseCasesByOperations(anyList())).thenReturn(List.of());

        UseCaseDiscoveredInterfaceDTO body = UseCaseDiscoveredInterfaceDTO.builder()
                .interfaceCode("orders-api")
                .product(ALIAS)
                .operations(List.of())
                .build();

        service.syncUseCaseOperations(USE_CASE_ID, List.of(body));

        assertThat(candidate.getDeletedDate()).isNull();
    }

    @Test
    @DisplayName("Пустой массив: ответ пуст, сервис project не вызывается")
    void emptyBodyReturnsEmptyList() {
        List<UseCaseDiscoveredInterfaceResultDTO> result =
                service.syncUseCaseOperations(USE_CASE_ID, List.of());

        assertThat(result).isEmpty();
        verify(projectClient, never()).findUseCasesByOperations(anyList());
        verify(discoveredInterfaceRepository, never()).save(any(DiscoveredInterface.class));
    }

    @Test
    @DisplayName("Ошибка сервиса project пробрасывается (транзакция откатывается)")
    void propagatesProjectClientError() {
        DiscoveredInterface stored = storedInterface("orders-api", null);
        when(discoveredInterfaceRepository.findAllBySourceAndExternalIdInIgnoreCase(eq("UseCase"), any()))
                .thenReturn(List.of(stored));
        DiscoveredOperation candidate = storedOperation(801, "orphan", "REST");
        when(discoveredOperationRepository.findAllByInterfaceIdIn(anyList())).thenReturn(List.of(candidate));
        when(projectClient.findUseCasesByOperations(anyList()))
                .thenThrow(new ExternalServiceException(org.springframework.http.HttpStatus.BAD_GATEWAY, "boom"));

        UseCaseDiscoveredInterfaceDTO body = UseCaseDiscoveredInterfaceDTO.builder()
                .interfaceCode("orders-api")
                .product(ALIAS)
                .operations(List.of())
                .build();

        assertThatThrownBy(() -> service.syncUseCaseOperations(USE_CASE_ID, List.of(body)))
                .isInstanceOf(ExternalServiceException.class);
        assertThat(candidate.getDeletedDate()).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "abc", "0", "-5"})
    @DisplayName("use-case-id отсутствует, пуст или не целое положительное — 400")
    void failsOnInvalidUseCaseId(String useCaseId) {
        assertThatThrownBy(() -> service.syncUseCaseOperations(useCaseId, List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Не передан обязательный параметр use-case-id");
    }

    @Test
    @DisplayName("Отсутствует тело — 400")
    void failsOnMissingBody() {
        assertThatThrownBy(() -> service.syncUseCaseOperations(USE_CASE_ID, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Отсутствует тело запроса");
    }

    @Test
    @DisplayName("Только productPlanned без product: интерфейс создаётся с пустым product_id")
    void createsWithProductPlannedOnly() {
        UseCaseDiscoveredInterfaceDTO body = UseCaseDiscoveredInterfaceDTO.builder()
                .interfaceCode("new-api")
                .productPlanned("planned-only")
                .operations(List.of(UseCaseDiscoveredOperationDTO.builder().name("op").type("REST").build()))
                .build();

        List<UseCaseDiscoveredInterfaceResultDTO> result =
                service.syncUseCaseOperations(USE_CASE_ID, List.of(body));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getProduct()).isNull();
        assertThat(result.get(0).getProductPlanned()).isEqualTo("planned-only");

        ArgumentCaptor<DiscoveredInterface> captor = ArgumentCaptor.forClass(DiscoveredInterface.class);
        verify(discoveredInterfaceRepository).save(captor.capture());
        assertThat(captor.getValue().getProduct()).isNull();
        assertThat(captor.getValue().getProductPlanned()).isEqualTo("planned-only");
        verify(productRepository, never()).findByAliasInIgnoreCase(anyList());
    }

    @Test
    @DisplayName("Пустой элемент, незаполненные обязательные поля и дубли — 400")
    void failsOnInvalidElements() {
        assertThatThrownBy(() -> service.syncUseCaseOperations(USE_CASE_ID, listWith(null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Пустой элемент");

        UseCaseDiscoveredInterfaceDTO noCode = request("orders-api");
        noCode.setInterfaceCode("  ");
        assertThatThrownBy(() -> service.syncUseCaseOperations(USE_CASE_ID, List.of(noCode)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("interfaceCode");

        UseCaseDiscoveredInterfaceDTO noProductAndPlanned = request("orders-api");
        noProductAndPlanned.setProduct(null);
        noProductAndPlanned.setProductPlanned("  ");
        assertThatThrownBy(() -> service.syncUseCaseOperations(USE_CASE_ID, List.of(noProductAndPlanned)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("обязательно должен быть указан существующий product либо планируемый productPlanned");

        UseCaseDiscoveredInterfaceDTO bothProductFields = request("orders-api");
        bothProductFields.setProductPlanned("planned");
        assertThatThrownBy(() -> service.syncUseCaseOperations(USE_CASE_ID, List.of(bothProductFields)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("обязательно должен быть указан существующий product либо планируемый productPlanned");

        UseCaseDiscoveredInterfaceDTO noOperations = request("orders-api");
        noOperations.setOperations(null);
        assertThatThrownBy(() -> service.syncUseCaseOperations(USE_CASE_ID, List.of(noOperations)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("operations");

        UseCaseDiscoveredInterfaceDTO noType = request("orders-api");
        noType.getOperations().get(0).setType(null);
        assertThatThrownBy(() -> service.syncUseCaseOperations(USE_CASE_ID, List.of(noType)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("operations.type");

        UseCaseDiscoveredInterfaceDTO duplicateOperation = request("orders-api");
        duplicateOperation.getOperations().add(UseCaseDiscoveredOperationDTO.builder()
                .name("GETORDER").type("rest").build());
        assertThatThrownBy(() -> service.syncUseCaseOperations(USE_CASE_ID, List.of(duplicateOperation)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("дважды");

        UseCaseDiscoveredInterfaceDTO duplicateInterface = request("ORDERS-API");
        assertThatThrownBy(() -> service.syncUseCaseOperations(USE_CASE_ID,
                List.of(request("orders-api"), duplicateInterface)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("дважды");

        UseCaseDiscoveredInterfaceDTO plannedA = UseCaseDiscoveredInterfaceDTO.builder()
                .interfaceCode("api")
                .productPlanned("plan-a")
                .operations(List.of())
                .build();
        UseCaseDiscoveredInterfaceDTO plannedB = UseCaseDiscoveredInterfaceDTO.builder()
                .interfaceCode("api")
                .productPlanned("PLAN-A")
                .operations(List.of())
                .build();
        assertThatThrownBy(() -> service.syncUseCaseOperations(USE_CASE_ID, List.of(plannedA, plannedB)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("дважды");
    }

    private List<UseCaseDiscoveredInterfaceDTO> listWith(UseCaseDiscoveredInterfaceDTO dto) {
        List<UseCaseDiscoveredInterfaceDTO> list = new ArrayList<>();
        list.add(dto);
        return list;
    }

    private UseCaseDiscoveredInterfaceDTO request(String interfaceCode) {
        return UseCaseDiscoveredInterfaceDTO.builder()
                .interfaceCode(interfaceCode)
                .product(ALIAS)
                .operations(new ArrayList<>(List.of(
                        UseCaseDiscoveredOperationDTO.builder().name("getOrder").type("REST").build())))
                .build();
    }

    private DiscoveredInterface storedInterface(String externalId, String productPlanned) {
        return DiscoveredInterface.builder()
                .id(STORED_INTERFACE_ID)
                .name(externalId)
                .externalId(externalId)
                .product(product)
                .productPlanned(productPlanned)
                .source("UseCase")
                .build();
    }

    private DiscoveredOperation storedOperation(Integer id, String name, String type) {
        return DiscoveredOperation.builder()
                .id(id)
                .interfaceId(STORED_INTERFACE_ID)
                .name(name)
                .type(type)
                .build();
    }

    private OperationUseCasesResponseDTO response(Integer operationId, List<OperationUseCaseDTO> useCases) {
        return OperationUseCasesResponseDTO.builder()
                .operationId(operationId)
                .useCases(useCases)
                .build();
    }

    private OperationUseCaseDTO useCase(Integer id) {
        return OperationUseCaseDTO.builder().id(id).name("uc-" + id).build();
    }
}
