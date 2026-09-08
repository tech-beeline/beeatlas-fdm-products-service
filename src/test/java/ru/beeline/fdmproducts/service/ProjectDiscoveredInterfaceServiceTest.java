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

import java.util.List;
import java.util.Optional;
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

/** Юнит-тесты upsert обнаруженных интерфейсов и операций из задачи проекта (SFDM-4081). */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProjectDiscoveredInterfaceServiceTest {

    private static final Integer PROJECT_ID = 11;
    private static final String ALIAS = "my-product";
    private static final Integer PRODUCT_ID = 1;
    private static final Integer INTERFACE_ID = 500;

    @Mock
    private ProductRepository productRepository;
    @Mock
    private DiscoveredInterfaceRepository discoveredInterfaceRepository;
    @Mock
    private DiscoveredOperationRepository discoveredOperationRepository;

    @InjectMocks
    private ProjectDiscoveredInterfaceService service;

    /** Раздаёт создаваемым операциям предсказуемые id: 901, 902, ... — в порядке сохранения. */
    private final AtomicInteger operationIdSequence = new AtomicInteger(900);

    @BeforeEach
    void setUp() {
        when(productRepository.findByAliasCaseInsensitive(anyString()))
                .thenReturn(Product.builder().id(PRODUCT_ID).alias(ALIAS).build());
        when(discoveredInterfaceRepository.findBySourceAndProductIdAndExternalIdIgnoreCase(anyString(), anyInt(), anyString()))
                .thenReturn(Optional.empty());
        when(discoveredInterfaceRepository.save(any(DiscoveredInterface.class))).thenAnswer(invocation -> {
            DiscoveredInterface saved = invocation.getArgument(0);
            if (saved.getId() == null) {
                saved.setId(INTERFACE_ID);
            }
            return saved;
        });
        when(discoveredOperationRepository.findByInterfaceIdAndNameAndTypeAllIgnoreCase(anyInt(), anyString(), anyString()))
                .thenReturn(Optional.empty());
        when(discoveredOperationRepository.save(any(DiscoveredOperation.class))).thenAnswer(invocation -> {
            DiscoveredOperation saved = invocation.getArgument(0);
            if (saved.getId() == null) {
                saved.setId(operationIdSequence.incrementAndGet());
            }
            return saved;
        });
    }

    @Test
    @DisplayName("Создание: интерфейс с project_id и операции со снимком tc-атрибутов, в ответе id операций")
    void createsInterfaceWithOperations() {
        List<ProjectDiscoveredInterfaceResultDTO> result =
                service.upsertProjectInterfaces(PROJECT_ID, "ProjectTask", List.of(request()));

        assertThat(result).hasSize(1);
        ProjectDiscoveredInterfaceResultDTO iface = result.get(0);
        assertThat(iface.getId()).isEqualTo(INTERFACE_ID);
        assertThat(iface.getInterfaceCode()).isEqualTo("iface-1");
        assertThat(iface.getProduct()).isEqualTo(ALIAS);
        assertThat(iface.getOperations()).extracting(op -> op.getId()).containsExactly(901, 902);
        assertThat(iface.getOperations()).extracting(op -> op.getName()).containsExactly("getOrder", "putOrder");
        assertThat(iface.getOperations()).extracting(op -> op.getType()).containsExactly("REST", "REST");

        ArgumentCaptor<DiscoveredInterface> ifaceCaptor = ArgumentCaptor.forClass(DiscoveredInterface.class);
        verify(discoveredInterfaceRepository).save(ifaceCaptor.capture());
        DiscoveredInterface saved = ifaceCaptor.getValue();
        assertThat(saved.getExternalId()).isEqualTo("iface-1");
        assertThat(saved.getName()).isEqualTo("iface-1");
        assertThat(saved.getProjectId()).isEqualTo(PROJECT_ID);
        assertThat(saved.getSource()).isEqualTo("ProjectTask");
        assertThat(saved.getProduct().getId()).isEqualTo(PRODUCT_ID);

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
    @DisplayName("source не передан — подставляется ProjectTask")
    void defaultsSourceToProjectTask() {
        service.upsertProjectInterfaces(PROJECT_ID, null, List.of(request()));

        verify(discoveredInterfaceRepository)
                .findBySourceAndProductIdAndExternalIdIgnoreCase(eq("ProjectTask"), eq(PRODUCT_ID), eq("iface-1"));
    }

    @Test
    @DisplayName("Обновление: существующий интерфейс не пересоздаётся, атрибуты операции перезаписываются")
    void updatesExistingInterfaceAndOperation() {
        DiscoveredInterface existingInterface = DiscoveredInterface.builder()
                .id(INTERFACE_ID)
                .externalId("iface-1")
                .name("iface-1")
                .projectId(PROJECT_ID)
                .source("ProjectTask")
                .build();
        when(discoveredInterfaceRepository.findBySourceAndProductIdAndExternalIdIgnoreCase(anyString(), anyInt(), anyString()))
                .thenReturn(Optional.of(existingInterface));
        DiscoveredOperation existingOperation = DiscoveredOperation.builder()
                .id(777)
                .name("getOrder")
                .type("REST")
                .description("старое описание")
                .tcCode("TC-OLD")
                .build();
        when(discoveredOperationRepository.findByInterfaceIdAndNameAndTypeAllIgnoreCase(anyInt(), eq("getOrder"), anyString()))
                .thenReturn(Optional.of(existingOperation));

        List<ProjectDiscoveredInterfaceResultDTO> result =
                service.upsertProjectInterfaces(PROJECT_ID, "ProjectTask", List.of(request()));

        assertThat(result.get(0).getOperations().get(0).getId()).isEqualTo(777);
        assertThat(existingOperation.getDescription()).isEqualTo("описание операции");
        assertThat(existingOperation.getTcCode()).isEqualTo("TC-1");
        assertThat(existingOperation.getTcDescription()).isEqualTo("описание ТС");
        // project_id и deleted_date не менялись — лишнего UPDATE интерфейса нет
        verify(discoveredInterfaceRepository, never()).save(any(DiscoveredInterface.class));
    }

    @Test
    @DisplayName("Неизвестный alias продукта — 404")
    void failsOnUnknownProduct() {
        when(productRepository.findByAliasCaseInsensitive(anyString())).thenReturn(null);

        assertThatThrownBy(() -> service.upsertProjectInterfaces(PROJECT_ID, "ProjectTask", List.of(request())))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining(ALIAS);
    }

    @Test
    @DisplayName("Пустое тело и незаполненные обязательные поля — 400")
    void failsOnInvalidBody() {
        assertThatThrownBy(() -> service.upsertProjectInterfaces(PROJECT_ID, "ProjectTask", null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.upsertProjectInterfaces(PROJECT_ID, "ProjectTask", List.of()))
                .isInstanceOf(IllegalArgumentException.class);

        ProjectDiscoveredInterfaceDTO noCode = request();
        noCode.setInterfaceCode("  ");
        assertThatThrownBy(() -> service.upsertProjectInterfaces(PROJECT_ID, "ProjectTask", List.of(noCode)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("interfaceCode");

        ProjectDiscoveredInterfaceDTO noProduct = request();
        noProduct.setProduct(null);
        assertThatThrownBy(() -> service.upsertProjectInterfaces(PROJECT_ID, "ProjectTask", List.of(noProduct)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("product");

        ProjectDiscoveredInterfaceDTO noOperations = request();
        noOperations.setOperations(List.of());
        assertThatThrownBy(() -> service.upsertProjectInterfaces(PROJECT_ID, "ProjectTask", List.of(noOperations)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("operations");

        ProjectDiscoveredInterfaceDTO noOperationType = request();
        noOperationType.getOperations().get(0).setType(null);
        assertThatThrownBy(() -> service.upsertProjectInterfaces(PROJECT_ID, "ProjectTask", List.of(noOperationType)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("operations.type");
    }

    private ProjectDiscoveredInterfaceDTO request() {
        return ProjectDiscoveredInterfaceDTO.builder()
                .interfaceCode("iface-1")
                .product(ALIAS)
                .operations(new java.util.ArrayList<>(List.of(
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
}
