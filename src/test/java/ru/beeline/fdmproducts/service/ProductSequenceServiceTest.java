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
import ru.beeline.fdmproducts.domain.Product;
import ru.beeline.fdmproducts.domain.ProductBranch;
import ru.beeline.fdmproducts.domain.ProductSequence;
import ru.beeline.fdmproducts.domain.SeqProductStep;
import ru.beeline.fdmproducts.domain.SequenceStepOperation;
import ru.beeline.fdmproducts.dto.sequence.SequenceCallDTO;
import ru.beeline.fdmproducts.dto.sequence.SequenceInfoDTO;
import ru.beeline.fdmproducts.dto.sequence.SequenceOperationDTO;
import ru.beeline.fdmproducts.dto.sequence.SequenceUpsertRequestDTO;
import ru.beeline.fdmproducts.dto.sequence.SequenceUpsertResponseDTO;
import ru.beeline.fdmproducts.exception.EntityNotFoundException;
import ru.beeline.fdmproducts.repository.OperationRepository;
import ru.beeline.fdmproducts.repository.ProductBranchRepository;
import ru.beeline.fdmproducts.repository.ProductRepository;
import ru.beeline.fdmproducts.repository.ProductSequenceRepository;
import ru.beeline.fdmproducts.repository.SeqProductStepRepository;
import ru.beeline.fdmproducts.repository.SequenceStepOperationRepository;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Юнит-тесты upsert product sequence (SFDM-4080). */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProductSequenceServiceTest {

    private static final String ALIAS = "my-product";
    private static final Integer BRANCH_ID = 42;
    private static final Integer SEQUENCE_ID = 7;

    @Mock
    private ProductRepository productRepository;
    @Mock
    private ProductBranchRepository productBranchRepository;
    @Mock
    private ProductSequenceRepository productSequenceRepository;
    @Mock
    private SeqProductStepRepository seqProductStepRepository;
    @Mock
    private SequenceStepOperationRepository sequenceStepOperationRepository;
    @Mock
    private OperationRepository operationRepository;

    @InjectMocks
    private ProductSequenceService service;

    /** Раздаёт снимкам методов предсказуемые id: 101, 102, ... — в порядке сохранения. */
    private final AtomicInteger stepOperationIdSequence = new AtomicInteger(100);

    @BeforeEach
    void setUp() {
        when(productRepository.findByAliasCaseInsensitive(anyString()))
                .thenReturn(Product.builder().id(1).alias(ALIAS).build());
        when(productBranchRepository.findByAliasAndBranchName(anyString(), anyString()))
                .thenReturn(Optional.of(ProductBranch.builder().id(BRANCH_ID).alias(ALIAS).branchName("main").build()));
        when(productSequenceRepository.findByProductBranchIdAndCodeIgnoreCase(anyInt(), anyString()))
                .thenReturn(Optional.empty());
        when(productSequenceRepository.save(any(ProductSequence.class))).thenAnswer(invocation -> {
            ProductSequence saved = invocation.getArgument(0);
            if (saved.getId() == null) {
                saved.setId(SEQUENCE_ID);
            }
            return saved;
        });
        when(sequenceStepOperationRepository.save(any(SequenceStepOperation.class))).thenAnswer(invocation -> {
            SequenceStepOperation saved = invocation.getArgument(0);
            saved.setId(stepOperationIdSequence.incrementAndGet());
            return saved;
        });
        when(seqProductStepRepository.findAllBySeqId(anyInt())).thenReturn(List.of());
        when(operationRepository.findIdsForSequenceStep(anyString(), anyString(), any(), any(), anyString(), anyString()))
                .thenReturn(List.of());
    }

    @Test
    @DisplayName("Создание: карточка, снимки методов и шаги сохраняются, в ответе id/code/productBranchId")
    void createsSequenceWithStepsAndSnapshots() {
        when(operationRepository.findIdsForSequenceStep(eq("my-product"), eq("main"), eq("cont"), eq("iface"),
                eq("getOrder"), eq("REST"))).thenReturn(List.of(555));

        SequenceUpsertResponseDTO response = service.upsert(ALIAS, "main", request());

        assertThat(response.getId()).isEqualTo(SEQUENCE_ID);
        assertThat(response.getCode()).isEqualTo("seq-1");
        assertThat(response.getProductBranchId()).isEqualTo(BRANCH_ID);

        ArgumentCaptor<ProductSequence> cardCaptor = ArgumentCaptor.forClass(ProductSequence.class);
        verify(productSequenceRepository).save(cardCaptor.capture());
        ProductSequence card = cardCaptor.getValue();
        assertThat(card.getProductBranchId()).isEqualTo(BRANCH_ID);
        assertThat(card.getCode()).isEqualTo("seq-1");
        assertThat(card.getName()).isEqualTo("Оформление заказа");
        assertThat(card.getDescription()).isEqualTo("описание sequence");
        assertThat(card.getTcCode()).isEqualTo("TC-1");

        ArgumentCaptor<SequenceStepOperation> snapshotCaptor = ArgumentCaptor.forClass(SequenceStepOperation.class);
        verify(sequenceStepOperationRepository, org.mockito.Mockito.times(2)).save(snapshotCaptor.capture());
        List<SequenceStepOperation> snapshots = snapshotCaptor.getAllValues();
        // Первым сохраняется вызывающий метод второго шага (guid-a), затем вызываемый (guid-b):
        // порядок — первое появление guid в sequenceCall.
        assertThat(snapshots.get(0).getOperationName()).isEqualTo("createOrder");
        assertThat(snapshots.get(0).getOperationProductAlias()).isEqualTo("my-product");
        assertThat(snapshots.get(1).getOperationName()).isEqualTo("getOrder");
        assertThat(snapshots.get(1).getOperationContainerCode()).isEqualTo("cont");
        assertThat(snapshots.get(1).getOperationInterfaceCode()).isEqualTo("iface");
        assertThat(snapshots.get(1).getOperationType()).isEqualTo("REST");
        assertThat(snapshots.get(1).getOperationId()).isEqualTo(555);

        List<SeqProductStep> steps = capturedSteps();
        assertThat(steps).hasSize(2);
        assertThat(steps.get(0).getSeqId()).isEqualTo(SEQUENCE_ID);
        assertThat(steps.get(0).getOrder()).isEqualTo(1);
        assertThat(steps.get(0).getRawDescription()).isEqualTo("старт");
        assertThat(steps.get(0).getSeqStepOperationId()).isNull();
        assertThat(steps.get(0).getSeqStepRelatedOperationId()).isEqualTo(101);
        assertThat(steps.get(1).getOrder()).isEqualTo(2);
        assertThat(steps.get(1).getSeqStepOperationId()).isEqualTo(101);
        assertThat(steps.get(1).getSeqStepRelatedOperationId()).isEqualTo(102);
    }

    @Test
    @DisplayName("Снимок метода создаётся один раз на guid, даже если на него ссылается несколько шагов")
    void reusesSnapshotPerGuid() {
        service.upsert(ALIAS, "main", request());

        verify(sequenceStepOperationRepository, org.mockito.Mockito.times(2)).save(any(SequenceStepOperation.class));
    }

    @Test
    @DisplayName("Пустой branch — резолв и поиск ветки идут по main")
    void fallsBackToMainBranch() {
        service.upsert(ALIAS, "  ", request());

        verify(productBranchRepository).findByAliasAndBranchName(ALIAS, "main");
        verify(operationRepository).findIdsForSequenceStep(anyString(), eq("main"), any(), any(),
                eq("createOrder"), anyString());
    }

    @Test
    @DisplayName("Имя ветки приводится к нижнему регистру")
    void normalizesBranchName() {
        service.upsert(ALIAS, " Design ", request());

        verify(productBranchRepository).findByAliasAndBranchName(ALIAS, "design");
        verify(operationRepository, org.mockito.Mockito.times(2)).findIdsForSequenceStep(anyString(), eq("design"),
                any(), any(), anyString(), anyString());
    }

    @Test
    @DisplayName("Правило 3: пустые containerCode/interfaceCode уходят в резолв как NULL")
    void passesBlankCodesAsNull() {
        SequenceUpsertRequestDTO request = request();
        request.getOperations().get(1).setContainerCode("");
        request.getOperations().get(1).setInterfaceCode(null);

        service.upsert(ALIAS, "main", request);

        verify(operationRepository).findIdsForSequenceStep(eq("my-product"), eq("main"), isNull(), isNull(),
                eq("getOrder"), eq("REST"));
    }

    @Test
    @DisplayName("Неоднозначный резолв (несколько совпадений) — operation_id остаётся NULL, ошибки нет")
    void leavesOperationIdNullWhenAmbiguous() {
        when(operationRepository.findIdsForSequenceStep(anyString(), anyString(), any(), any(), anyString(), anyString()))
                .thenReturn(Arrays.asList(1, 2));

        service.upsert(ALIAS, "main", request());

        ArgumentCaptor<SequenceStepOperation> captor = ArgumentCaptor.forClass(SequenceStepOperation.class);
        verify(sequenceStepOperationRepository, org.mockito.Mockito.times(2)).save(captor.capture());
        assertThat(captor.getAllValues()).allSatisfy(snapshot -> assertThat(snapshot.getOperationId()).isNull());
    }

    @Test
    @DisplayName("Обновление: карточка обновляется, прежние шаги и их снимки удаляются")
    void rewritesCompositionOnUpdate() {
        ProductSequence existing = ProductSequence.builder()
                .id(SEQUENCE_ID)
                .productBranchId(BRANCH_ID)
                .code("SEQ-1")
                .name("старое имя")
                .build();
        when(productSequenceRepository.findByProductBranchIdAndCodeIgnoreCase(BRANCH_ID, "seq-1"))
                .thenReturn(Optional.of(existing));
        when(seqProductStepRepository.findAllBySeqId(SEQUENCE_ID)).thenReturn(List.of(
                SeqProductStep.builder().id(1).seqId(SEQUENCE_ID).order(1)
                        .seqStepOperationId(null).seqStepRelatedOperationId(11).build(),
                SeqProductStep.builder().id(2).seqId(SEQUENCE_ID).order(2)
                        .seqStepOperationId(11).seqStepRelatedOperationId(12).build()));

        service.upsert(ALIAS, "main", request());

        assertThat(existing.getName()).isEqualTo("Оформление заказа");
        assertThat(existing.getCode()).isEqualTo("seq-1");
        verify(seqProductStepRepository).deleteAllBySeqId(SEQUENCE_ID);
        ArgumentCaptor<Collection<Integer>> orphanCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(sequenceStepOperationRepository).deleteOrphansByIdIn(orphanCaptor.capture());
        assertThat(orphanCaptor.getValue()).containsExactlyInAnyOrder(11, 12);
        assertThat(capturedSteps()).hasSize(2);
    }

    @Test
    @DisplayName("Новый sequence: удаление прежнего состава не вызывается")
    void doesNotDeleteAnythingForNewSequence() {
        service.upsert(ALIAS, "main", request());

        verify(seqProductStepRepository, never()).deleteAllBySeqId(anyInt());
        verify(sequenceStepOperationRepository, never()).deleteOrphansByIdIn(any());
    }

    @Test
    @DisplayName("Продукт не найден — 404")
    void failsWhenProductNotFound() {
        when(productRepository.findByAliasCaseInsensitive(anyString())).thenReturn(null);

        assertThatThrownBy(() -> service.upsert("no-such", "main", request()))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Продукт с указанным alias не найден");
        verifyNoInteractions(productSequenceRepository);
    }

    @Test
    @DisplayName("Ветка не найдена — 404")
    void failsWhenBranchNotFound() {
        when(productBranchRepository.findByAliasAndBranchName(anyString(), anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.upsert(ALIAS, "develop", request()))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Ветка продукта не найдена");
        verifyNoInteractions(productSequenceRepository);
    }

    @Test
    @DisplayName("Отсутствует тело запроса — 400")
    void failsWithoutBody() {
        assertThatThrownBy(() -> service.upsert(ALIAS, "main", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Отсутствует тело запроса");
    }

    @Test
    @DisplayName("Пустые обязательные поля карточки — 400")
    void failsOnBlankMandatoryCardFields() {
        SequenceUpsertRequestDTO noSequence = request();
        noSequence.setSequence(null);
        assertThatThrownBy(() -> service.upsert(ALIAS, "main", noSequence))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Отсутствует обязательное поле sequence");

        SequenceUpsertRequestDTO blankUid = request();
        blankUid.getSequence().setUid("  ");
        assertThatThrownBy(() -> service.upsert(ALIAS, "main", blankUid))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Отсутствует обязательное поле sequence.uid");

        SequenceUpsertRequestDTO blankName = request();
        blankName.getSequence().setName(null);
        assertThatThrownBy(() -> service.upsert(ALIAS, "main", blankName))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Отсутствует обязательное поле sequence.name");
    }

    @Test
    @DisplayName("sequenceCall или operations отсутствуют / operations пуст — 400")
    void failsOnMissingCollections() {
        SequenceUpsertRequestDTO noCalls = request();
        noCalls.setSequenceCall(null);
        assertThatThrownBy(() -> service.upsert(ALIAS, "main", noCalls))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Отсутствует обязательное поле sequenceCall");

        SequenceUpsertRequestDTO emptyOperations = request();
        emptyOperations.setOperations(List.of());
        assertThatThrownBy(() -> service.upsert(ALIAS, "main", emptyOperations))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Отсутствует обязательное непустое поле operations");
    }

    @Test
    @DisplayName("Обязательные атрибуты элемента operations — 400")
    void failsOnBlankOperationAttributes() {
        SequenceUpsertRequestDTO blankType = request();
        blankType.getOperations().get(0).setType(" ");

        assertThatThrownBy(() -> service.upsert(ALIAS, "main", blankType))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Отсутствует обязательное поле operations.type");
    }

    @Test
    @DisplayName("Дубликат guid в operations — 400")
    void failsOnDuplicateGuid() {
        SequenceUpsertRequestDTO request = request();
        request.getOperations().get(1).setGuid("guid-a");

        assertThatThrownBy(() -> service.upsert(ALIAS, "main", request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Дубликат guid в operations: guid-a");
    }

    @Test
    @DisplayName("relatedOperationGuid не найден среди operations — 400 с текстом из постановки")
    void failsOnUnknownRelatedGuid() {
        SequenceUpsertRequestDTO request = request();
        request.getSequenceCall().get(0).setRelatedOperationGuid("guid-unknown");

        assertThatThrownBy(() -> service.upsert(ALIAS, "main", request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("sequenceCall ссылается на неизвестный guid операции");
        verifyNoInteractions(productSequenceRepository);
    }

    @Test
    @DisplayName("operationGuid не найден среди operations — 400 с текстом из постановки")
    void failsOnUnknownOperationGuid() {
        SequenceUpsertRequestDTO request = request();
        request.getSequenceCall().get(1).setOperationGuid("guid-unknown");

        assertThatThrownBy(() -> service.upsert(ALIAS, "main", request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("sequenceCall ссылается на неизвестный guid операции");
    }

    @Test
    @DisplayName("Обязательные поля шага: relatedOperationGuid и order — 400")
    void failsOnIncompleteCall() {
        SequenceUpsertRequestDTO noRelated = request();
        noRelated.getSequenceCall().get(0).setRelatedOperationGuid(null);
        assertThatThrownBy(() -> service.upsert(ALIAS, "main", noRelated))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Отсутствует обязательное поле sequenceCall.relatedOperationGuid");

        SequenceUpsertRequestDTO noOrder = request();
        noOrder.getSequenceCall().get(0).setOrder(null);
        assertThatThrownBy(() -> service.upsert(ALIAS, "main", noOrder))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Отсутствует обязательное поле sequenceCall.order");
    }

    @Test
    @DisplayName("Метод, на который нет ссылок из sequenceCall, не сохраняется и не резолвится")
    void ignoresUnreferencedOperations() {
        SequenceUpsertRequestDTO request = request();
        request.setOperations(List.of(
                request.getOperations().get(0),
                request.getOperations().get(1),
                SequenceOperationDTO.builder()
                        .guid("guid-unused").productAlias("other").name("ping").type("REST").build()));

        service.upsert(ALIAS, "main", request);

        verify(sequenceStepOperationRepository, org.mockito.Mockito.times(2)).save(any(SequenceStepOperation.class));
        verify(operationRepository, never()).findIdsForSequenceStep(eq("other"), anyString(), any(), any(),
                anyString(), anyString());
    }

    @SuppressWarnings("unchecked")
    private List<SeqProductStep> capturedSteps() {
        ArgumentCaptor<List<SeqProductStep>> captor = ArgumentCaptor.forClass(List.class);
        verify(seqProductStepRepository).saveAll(captor.capture());
        return captor.getValue();
    }

    /**
     * Два шага: стартовый вызов createOrder (без вызывающей стороны) и вызов getOrder со стороны
     * createOrder — минимальная форма, покрывающая и NULL-вызывающего, и переиспользование guid.
     */
    private SequenceUpsertRequestDTO request() {
        return SequenceUpsertRequestDTO.builder()
                .sequence(SequenceInfoDTO.builder()
                        .uid("seq-1")
                        .name("Оформление заказа")
                        .description("описание sequence")
                        .tcCode("TC-1")
                        .build())
                .sequenceCall(new java.util.ArrayList<>(List.of(
                        SequenceCallDTO.builder()
                                .relatedOperationGuid("guid-a")
                                .order(1)
                                .description("старт")
                                .build(),
                        SequenceCallDTO.builder()
                                .operationGuid("guid-a")
                                .relatedOperationGuid("guid-b")
                                .order(2)
                                .description("вызов getOrder")
                                .build())))
                .operations(new java.util.ArrayList<>(List.of(
                        SequenceOperationDTO.builder()
                                .guid("guid-a")
                                .productAlias("my-product")
                                .name("createOrder")
                                .type("REST")
                                .build(),
                        SequenceOperationDTO.builder()
                                .guid("guid-b")
                                .productAlias("my-product")
                                .containerCode("cont")
                                .interfaceCode("iface")
                                .name("getOrder")
                                .type("REST")
                                .build())))
                .build();
    }
}
