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
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import ru.beeline.fdmproducts.domain.DiscoveredInterface;
import ru.beeline.fdmproducts.domain.DiscoveredOperation;
import ru.beeline.fdmproducts.domain.Product;
import ru.beeline.fdmproducts.dto.discovered.ProjectDiscoveredInterfaceDTO;
import ru.beeline.fdmproducts.dto.discovered.ProjectDiscoveredInterfaceResultDTO;
import ru.beeline.fdmproducts.dto.discovered.ProjectDiscoveredOperationDTO;
import ru.beeline.fdmproducts.exception.EntityNotFoundException;
import ru.beeline.fdmproducts.repository.DiscoveredInterfaceRepository;
import ru.beeline.fdmproducts.repository.DiscoveredOperationRepository;
import ru.beeline.fdmproducts.repository.ProductRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Юнит-тесты синхронизации обнаруженных интерфейсов и операций проекта (SFDM-4095). */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProjectDiscoveredInterfaceServiceTest {

    private static final Integer PROJECT_ID = 11;
    private static final String ALIAS = "my-product";
    private static final Integer PRODUCT_ID = 1;
    private static final String SOURCE = "ProjectTask";
    private static final Integer NEW_INTERFACE_ID = 500;

    @Mock
    private ProductRepository productRepository;
    @Mock
    private DiscoveredInterfaceRepository discoveredInterfaceRepository;
    @Mock
    private DiscoveredOperationRepository discoveredOperationRepository;

    @InjectMocks
    private ProjectDiscoveredInterfaceService service;

    private final Product product = Product.builder().id(PRODUCT_ID).alias(ALIAS).build();

    /** Раздаёт создаваемым операциям предсказуемые id: 901, 902, ... — в порядке сохранения. */
    private final AtomicInteger operationIdSequence = new AtomicInteger(900);

    @BeforeEach
    void setUp() {
        when(productRepository.findByAliasCaseInsensitive(anyString())).thenReturn(product);
        when(discoveredInterfaceRepository.findAllByProjectIdAndSourceIgnoreCase(anyInt(), anyString()))
                .thenReturn(List.of());
        when(discoveredOperationRepository.findAllByInterfaceId(anyInt())).thenReturn(List.of());
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
    @DisplayName("Создание: интерфейс с project_id и операции со снимком tc-атрибутов, в ответе discoveredOperationId")
    void createsInterfaceWithOperations() {
        List<ProjectDiscoveredInterfaceResultDTO> result =
                service.syncProjectInterfaces(PROJECT_ID, SOURCE, List.of(request()));

        assertThat(result).hasSize(1);
        ProjectDiscoveredInterfaceResultDTO iface = result.get(0);
        assertThat(iface.getInterfaceCode()).isEqualTo("iface-1");
        assertThat(iface.getProduct()).isEqualTo(ALIAS);
        assertThat(iface.getOperations()).extracting(op -> op.getDiscoveredOperationId())
                .containsExactly(901, 902);
        assertThat(iface.getOperations()).extracting(op -> op.getName())
                .containsExactly("getOrder", "putOrder");
        assertThat(iface.getOperations()).extracting(op -> op.getType())
                .containsExactly("REST", "REST");

        ArgumentCaptor<DiscoveredInterface> ifaceCaptor = ArgumentCaptor.forClass(DiscoveredInterface.class);
        verify(discoveredInterfaceRepository).save(ifaceCaptor.capture());
        DiscoveredInterface saved = ifaceCaptor.getValue();
        assertThat(saved.getExternalId()).isEqualTo("iface-1");
        assertThat(saved.getName()).isEqualTo("iface-1");
        assertThat(saved.getProjectId()).isEqualTo(PROJECT_ID);
        assertThat(saved.getSource()).isEqualTo(SOURCE);
        assertThat(saved.getProduct().getId()).isEqualTo(PRODUCT_ID);
        assertThat(saved.getCreatedDate()).isNotNull();
        assertThat(saved.getDeletedDate()).isNull();

        ArgumentCaptor<DiscoveredOperation> opCaptor = ArgumentCaptor.forClass(DiscoveredOperation.class);
        verify(discoveredOperationRepository, org.mockito.Mockito.times(2)).save(opCaptor.capture());
        DiscoveredOperation firstOperation = opCaptor.getAllValues().get(0);
        assertThat(firstOperation.getName()).isEqualTo("getOrder");
        assertThat(firstOperation.getType()).isEqualTo("REST");
        assertThat(firstOperation.getDescription()).isEqualTo("описание операции");
        assertThat(firstOperation.getTcCode()).isEqualTo("TC-1");
        assertThat(firstOperation.getTcDescription()).isEqualTo("описание ТС");
    }

    @Test
    @DisplayName("source сравнивается без учёта регистра, а в БД пишется как в query после trim")
    void keepsQuerySourceCaseOnInsert() {
        service.syncProjectInterfaces(PROJECT_ID, "  SPARX  ", List.of(request()));

        verify(discoveredInterfaceRepository).findAllByProjectIdAndSourceIgnoreCase(eq(PROJECT_ID), eq("SPARX"));
        ArgumentCaptor<DiscoveredInterface> captor = ArgumentCaptor.forClass(DiscoveredInterface.class);
        verify(discoveredInterfaceRepository).save(captor.capture());
        assertThat(captor.getValue().getSource()).isEqualTo("SPARX");
    }

    @Test
    @DisplayName("Сохранённый интерфейс с другим регистром source и interfaceCode не пересоздаётся")
    void matchesStoredInterfaceIgnoringCase() {
        DiscoveredInterface stored = storedInterface(700, "IFACE-1");
        stored.setSource("projecttask");
        when(discoveredInterfaceRepository.findAllByProjectIdAndSourceIgnoreCase(anyInt(), anyString()))
                .thenReturn(List.of(stored));

        List<ProjectDiscoveredInterfaceResultDTO> result =
                service.syncProjectInterfaces(PROJECT_ID, SOURCE, List.of(request()));

        assertThat(result).hasSize(1);
        assertThat(stored.getDeletedDate()).isNull();
        assertThat(stored.getUpdatedDate()).isNotNull();
        // source / external_id остались прежними — они часть ключа поиска, а не обновляемые атрибуты
        assertThat(stored.getSource()).isEqualTo("projecttask");
        assertThat(stored.getExternalId()).isEqualTo("IFACE-1");
        verify(discoveredOperationRepository).findAllByInterfaceId(700);
    }

    @Test
    @DisplayName("Обновление операции: атрибуты из тела перезаписывают сохранённые, deleted_date снимается")
    void updatesExistingOperation() {
        DiscoveredInterface stored = storedInterface(700, "iface-1");
        when(discoveredInterfaceRepository.findAllByProjectIdAndSourceIgnoreCase(anyInt(), anyString()))
                .thenReturn(List.of(stored));
        DiscoveredOperation existing = storedOperation(777, "getorder", "rest");
        existing.setDescription("старое описание");
        existing.setTcCode("TC-OLD");
        existing.setDeletedDate(LocalDateTime.now());
        when(discoveredOperationRepository.findAllByInterfaceId(700)).thenReturn(List.of(existing));

        List<ProjectDiscoveredInterfaceResultDTO> result =
                service.syncProjectInterfaces(PROJECT_ID, SOURCE, List.of(request()));

        assertThat(result.get(0).getOperations().get(0).getDiscoveredOperationId()).isEqualTo(777);
        assertThat(existing.getDescription()).isEqualTo("описание операции");
        assertThat(existing.getTcCode()).isEqualTo("TC-1");
        assertThat(existing.getTcDescription()).isEqualTo("описание ТС");
        assertThat(existing.getDeletedDate()).isNull();
        assertThat(existing.getUpdatedDate()).isNotNull();
        // ключ операции не меняется даже при другом регистре в теле
        assertThat(existing.getName()).isEqualTo("getorder");
        assertThat(existing.getType()).isEqualTo("rest");
    }

    @Test
    @DisplayName("Атрибут, отсутствующий в теле, затирается в NULL; без изменений updated_date не двигается")
    void clearsAttributesMissingInBodyAndSkipsUnchanged() {
        DiscoveredInterface stored = storedInterface(700, "iface-1");
        when(discoveredInterfaceRepository.findAllByProjectIdAndSourceIgnoreCase(anyInt(), anyString()))
                .thenReturn(List.of(stored));
        DiscoveredOperation cleared = storedOperation(777, "getOrder", "REST");
        cleared.setDescription("описание, снятое в источнике");
        cleared.setTcCode("TC-OLD");
        DiscoveredOperation unchanged = storedOperation(778, "putOrder", "REST");
        when(discoveredOperationRepository.findAllByInterfaceId(700)).thenReturn(List.of(cleared, unchanged));

        ProjectDiscoveredInterfaceDTO body = ProjectDiscoveredInterfaceDTO.builder()
                .interfaceCode("iface-1")
                .product(ALIAS)
                .operations(new ArrayList<>(List.of(
                        ProjectDiscoveredOperationDTO.builder().name("getOrder").type("REST").build(),
                        ProjectDiscoveredOperationDTO.builder().name("putOrder").type("REST").build())))
                .build();

        service.syncProjectInterfaces(PROJECT_ID, SOURCE, List.of(body));

        assertThat(cleared.getDescription()).isNull();
        assertThat(cleared.getTcCode()).isNull();
        assertThat(cleared.getUpdatedDate()).isNotNull();
        assertThat(unchanged.getUpdatedDate()).isNull();
        assertThat(unchanged.getDeletedDate()).isNull();
    }

    @Test
    @DisplayName("Интерфейс проекта, которого нет в теле, помечается удалённым")
    void softDeletesInterfaceMissingFromBody() {
        DiscoveredInterface kept = storedInterface(700, "iface-1");
        DiscoveredInterface dropped = storedInterface(701, "iface-2");
        when(discoveredInterfaceRepository.findAllByProjectIdAndSourceIgnoreCase(anyInt(), anyString()))
                .thenReturn(List.of(kept, dropped));

        service.syncProjectInterfaces(PROJECT_ID, SOURCE, List.of(request()));

        assertThat(dropped.getDeletedDate()).isNotNull();
        assertThat(dropped.getUpdatedDate()).isNotNull();
        assertThat(kept.getDeletedDate()).isNull();
        // операции выбывшего интерфейса отдельно не трогаем
        verify(discoveredOperationRepository, never()).findAllByInterfaceId(701);
    }

    @Test
    @DisplayName("Пустой массив в теле: все активные интерфейсы проекта с этим source помечаются удалёнными")
    void emptyBodySoftDeletesEveryInterface() {
        DiscoveredInterface first = storedInterface(700, "iface-1");
        DiscoveredInterface second = storedInterface(701, "iface-2");
        DiscoveredInterface alreadyDeleted = storedInterface(702, "iface-3");
        LocalDateTime deletedEarlier = LocalDateTime.now().minusDays(1);
        alreadyDeleted.setDeletedDate(deletedEarlier);
        when(discoveredInterfaceRepository.findAllByProjectIdAndSourceIgnoreCase(anyInt(), anyString()))
                .thenReturn(List.of(first, second, alreadyDeleted));

        List<ProjectDiscoveredInterfaceResultDTO> result =
                service.syncProjectInterfaces(PROJECT_ID, SOURCE, List.of());

        assertThat(result).isEmpty();
        assertThat(first.getDeletedDate()).isNotNull();
        assertThat(second.getDeletedDate()).isNotNull();
        // повторно удалённое не трогаем
        assertThat(alreadyDeleted.getDeletedDate()).isEqualTo(deletedEarlier);
        assertThat(alreadyDeleted.getUpdatedDate()).isNull();
    }

    @Test
    @DisplayName("Интерфейс, ранее помеченный удалённым, оживает при появлении в теле")
    void revivesSoftDeletedInterface() {
        DiscoveredInterface stored = storedInterface(700, "iface-1");
        stored.setDeletedDate(LocalDateTime.now().minusDays(1));
        when(discoveredInterfaceRepository.findAllByProjectIdAndSourceIgnoreCase(anyInt(), anyString()))
                .thenReturn(List.of(stored));

        service.syncProjectInterfaces(PROJECT_ID, SOURCE, List.of(request()));

        assertThat(stored.getDeletedDate()).isNull();
        assertThat(stored.getUpdatedDate()).isNotNull();
    }

    @Test
    @DisplayName("Операция, которой нет в operations[] интерфейса, помечается удалённой")
    void softDeletesOperationMissingFromBody() {
        DiscoveredInterface stored = storedInterface(700, "iface-1");
        when(discoveredInterfaceRepository.findAllByProjectIdAndSourceIgnoreCase(anyInt(), anyString()))
                .thenReturn(List.of(stored));
        DiscoveredOperation dropped = storedOperation(779, "deleteOrder", "REST");
        when(discoveredOperationRepository.findAllByInterfaceId(700)).thenReturn(List.of(dropped));

        service.syncProjectInterfaces(PROJECT_ID, SOURCE, List.of(request()));

        assertThat(dropped.getDeletedDate()).isNotNull();
        assertThat(dropped.getUpdatedDate()).isNotNull();
    }

    @Test
    @DisplayName("Пустой operations[]: все активные операции интерфейса помечаются удалёнными")
    void emptyOperationsSoftDeletesEveryOperation() {
        DiscoveredInterface stored = storedInterface(700, "iface-1");
        when(discoveredInterfaceRepository.findAllByProjectIdAndSourceIgnoreCase(anyInt(), anyString()))
                .thenReturn(List.of(stored));
        DiscoveredOperation dropped = storedOperation(779, "getOrder", "REST");
        when(discoveredOperationRepository.findAllByInterfaceId(700)).thenReturn(List.of(dropped));

        ProjectDiscoveredInterfaceDTO body = ProjectDiscoveredInterfaceDTO.builder()
                .interfaceCode("iface-1")
                .product(ALIAS)
                .operations(List.of())
                .build();

        List<ProjectDiscoveredInterfaceResultDTO> result =
                service.syncProjectInterfaces(PROJECT_ID, SOURCE, List.of(body));

        assertThat(result.get(0).getOperations()).isEmpty();
        assertThat(dropped.getDeletedDate()).isNotNull();
    }

    @Test
    @DisplayName("Неизвестный alias продукта — 404 с алиасом в сообщении")
    void failsOnUnknownProduct() {
        when(productRepository.findByAliasCaseInsensitive(anyString())).thenReturn(null);

        assertThatThrownBy(() -> service.syncProjectInterfaces(PROJECT_ID, SOURCE, List.of(request())))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Продукт с alias '" + ALIAS + "' не найден");
        // резолв продуктов идёт до записи — ни одного сохранения не случилось
        verify(discoveredInterfaceRepository, never()).save(any(DiscoveredInterface.class));
    }

    @Test
    @DisplayName("Пустой source — 400")
    void failsOnBlankSource() {
        assertThatThrownBy(() -> service.syncProjectInterfaces(PROJECT_ID, "   ", List.of(request())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Параметр source не может быть пустым");
        assertThatThrownBy(() -> service.syncProjectInterfaces(PROJECT_ID, null, List.of(request())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Параметр source не может быть пустым");
    }

    @Test
    @DisplayName("Отсутствие тела и незаполненные обязательные поля — 400")
    void failsOnInvalidBody() {
        assertThatThrownBy(() -> service.syncProjectInterfaces(PROJECT_ID, SOURCE, null))
                .isInstanceOf(IllegalArgumentException.class);

        ProjectDiscoveredInterfaceDTO noCode = request();
        noCode.setInterfaceCode("  ");
        assertThatThrownBy(() -> service.syncProjectInterfaces(PROJECT_ID, SOURCE, List.of(noCode)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("interfaceCode");

        ProjectDiscoveredInterfaceDTO noProduct = request();
        noProduct.setProduct(null);
        assertThatThrownBy(() -> service.syncProjectInterfaces(PROJECT_ID, SOURCE, List.of(noProduct)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("product");

        ProjectDiscoveredInterfaceDTO noOperations = request();
        noOperations.setOperations(null);
        assertThatThrownBy(() -> service.syncProjectInterfaces(PROJECT_ID, SOURCE, List.of(noOperations)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("operations");

        ProjectDiscoveredInterfaceDTO noOperationType = request();
        noOperationType.getOperations().get(0).setType(null);
        assertThatThrownBy(() -> service.syncProjectInterfaces(PROJECT_ID, SOURCE, List.of(noOperationType)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("operations.type");
    }

    @Test
    @DisplayName("Дубликат интерфейса в теле при том же продукте — 400, при разных продуктах — не ошибка")
    void failsOnDuplicateInterfaceOnlyWithinSameProduct() {
        ProjectDiscoveredInterfaceDTO duplicate = request();
        duplicate.setInterfaceCode("IFACE-1");
        assertThatThrownBy(() -> service.syncProjectInterfaces(PROJECT_ID, SOURCE, List.of(request(), duplicate)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("дважды");

        ProjectDiscoveredInterfaceDTO otherProduct = request();
        otherProduct.setProduct("other-product");
        when(productRepository.findByAliasCaseInsensitive("other-product"))
                .thenReturn(Product.builder().id(2).alias("other-product").build());

        List<ProjectDiscoveredInterfaceResultDTO> result =
                service.syncProjectInterfaces(PROJECT_ID, SOURCE, List.of(request(), otherProduct));

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("Дубликат (name, type) внутри operations[] — 400")
    void failsOnDuplicateOperation() {
        ProjectDiscoveredInterfaceDTO body = request();
        body.getOperations().add(ProjectDiscoveredOperationDTO.builder()
                .name("GETORDER")
                .type("rest")
                .build());

        assertThatThrownBy(() -> service.syncProjectInterfaces(PROJECT_ID, SOURCE, List.of(body)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("дважды");
    }

    private ProjectDiscoveredInterfaceDTO request() {
        return ProjectDiscoveredInterfaceDTO.builder()
                .interfaceCode("iface-1")
                .product(ALIAS)
                .operations(new ArrayList<>(List.of(
                        ProjectDiscoveredOperationDTO.builder()
                                .name("getOrder")
                                .type("REST")
                                .descriptionTc("описание ТС")
                                .tcCode("TC-1")
                                .descriptionOperation("описание операции")
                                .build(),
                        ProjectDiscoveredOperationDTO.builder()
                                .name("putOrder")
                                .type("REST")
                                .build())))
                .build();
    }

    private DiscoveredInterface storedInterface(Integer id, String externalId) {
        return DiscoveredInterface.builder()
                .id(id)
                .name(externalId)
                .externalId(externalId)
                .product(product)
                .projectId(PROJECT_ID)
                .source(SOURCE)
                .build();
    }

    private DiscoveredOperation storedOperation(Integer id, String name, String type) {
        return DiscoveredOperation.builder()
                .id(id)
                .interfaceId(700)
                .name(name)
                .type(type)
                .build();
    }
}
