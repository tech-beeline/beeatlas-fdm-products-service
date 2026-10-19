/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import ru.beeline.fdmproducts.dto.e2e.E2eCardResponseDTO;
import ru.beeline.fdmproducts.repository.E2eRepository;
import ru.beeline.fdmproducts.repository.E2eSearchRepository;
import ru.beeline.fdmproducts.repository.E2eSearchRepository.BiStepFilter;
import ru.beeline.fdmproducts.repository.E2eSearchRepository.Criteria;
import ru.beeline.fdmproducts.repository.OperationRepository;
import ru.beeline.fdmproducts.repository.SlaRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** SFDM-4240: GET /api/v1/e2e — проверки параметров и состав запроса. */
class E2eSearchTest {

    private E2eSearchRepository searchRepository;
    private E2eService service;

    @BeforeEach
    void setUp() {
        searchRepository = mock(E2eSearchRepository.class);
        service = new E2eService(searchRepository, mock(OperationRepository.class), mock(SlaRepository.class),
                mock(E2eRepository.class), mock(E2eBranchService.class), mock(E2eOwnerService.class));
    }

    @Test
    @DisplayName("Без параметров — все e2e: фильтр all, остальные не переданы")
    void noParametersReturnsEverything() {
        E2eCardResponseDTO card = E2eCardResponseDTO.builder().id(15).code("checkout-flow").build();
        when(searchRepository.search(any())).thenReturn(List.of(card));

        assertThat(service.search(null, null, null, null, null)).containsExactly(card);
        verify(searchRepository).search(new Criteria(BiStepFilter.ALL, null, null, null, null));
    }

    @Test
    @DisplayName("filter: all, пустой и непереданный равны all; with-bi-step и without-bi-step после trim без регистра")
    void parsesBiStepFilter() {
        assertThat(E2eService.criteria("", null, null, null, null).filter()).isEqualTo(BiStepFilter.ALL);
        assertThat(E2eService.criteria("  ", null, null, null, null).filter()).isEqualTo(BiStepFilter.ALL);
        assertThat(E2eService.criteria(" ALL ", null, null, null, null).filter()).isEqualTo(BiStepFilter.ALL);
        assertThat(E2eService.criteria(" With-Bi-Step ", null, null, null, null).filter())
                .isEqualTo(BiStepFilter.WITH_BI_STEP);
        assertThat(E2eService.criteria("without-bi-step", null, null, null, null).filter())
                .isEqualTo(BiStepFilter.WITHOUT_BI_STEP);
    }

    @Test
    @DisplayName("Неизвестный filter — 400, база не читается")
    void rejectsUnknownFilter() {
        assertThatThrownBy(() -> service.search("some", null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Некорректное значение параметра filter");
        verify(searchRepository, never()).search(any());
    }

    @Test
    @DisplayName("method вместе с operation-id — 400 независимо от значений, база не читается")
    void rejectsMethodWithOperationId() {
        assertThatThrownBy(() -> service.search(null, null, null, "GET getCart", "100"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Параметры method и operation-id нельзя передавать вместе");
        assertThatThrownBy(() -> service.search(null, null, null, "", ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Параметры method и operation-id нельзя передавать вместе");
        verify(searchRepository, never()).search(any());
    }

    @Test
    @DisplayName("Пустые alias, tc-code, method — 400 со своими сообщениями")
    void rejectsBlankTextParameters() {
        assertThatThrownBy(() -> service.search(null, "  ", null, null, null))
                .hasMessage("Параметр alias не может быть пустым");
        assertThatThrownBy(() -> service.search(null, null, "", null, null))
                .hasMessage("Параметр tc-code не может быть пустым");
        assertThatThrownBy(() -> service.search(null, null, null, " ", null))
                .hasMessage("Параметр method не может быть пустым");
        verify(searchRepository, never()).search(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "0", "-5", "abc", "1.5", "+3", "99999999999"})
    @DisplayName("operation-id не целое положительное — 400")
    void rejectsInvalidOperationId(String operationId) {
        assertThatThrownBy(() -> service.search(null, null, null, null, operationId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Параметр operation-id должен быть целым положительным числом");
        verify(searchRepository, never()).search(any());
    }

    @Test
    @DisplayName("Параметры передаются после trim; два пробела внутри method сохраняются")
    void trimsParameters() {
        Criteria criteria = E2eService.criteria(null, " CHECKOUT ", " MAPIC.checkout ", " GET  getCart ", null);

        assertThat(criteria.alias()).isEqualTo("CHECKOUT");
        assertThat(criteria.tcCode()).isEqualTo("MAPIC.checkout");
        assertThat(criteria.method()).isEqualTo("GET  getCart");
        assertThat(E2eService.criteria(null, null, null, null, " 20 ").operationId()).isEqualTo(20);
    }

    @Test
    @DisplayName("Только filter и alias — запрос без operation_relations, discovered_operation и product.operation")
    void filterAndAliasDoNotReadRelations() {
        E2eSearchRepository.Query query = E2eSearchRepository.build(
                new Criteria(BiStepFilter.WITHOUT_BI_STEP, "CHECKOUT", null, null, null));

        assertThat(query.sql()).doesNotContain("operation_relations", "discovered_operation", "product.operation ")
                .contains("btrim(e.bi_step_code) = ''", "lower(p.alias) = lower(:alias)", "ORDER BY e.code, e.id");
        assertThat(query.params().getValue("alias")).isEqualTo("CHECKOUT");
    }

    @Test
    @DisplayName("tc-code и operation-id смотрят только на корневую связь и не читают product.operation")
    void rootFiltersWithoutMethodDoNotReadOperation() {
        E2eSearchRepository.Query query = E2eSearchRepository.build(
                new Criteria(BiStepFilter.WITH_BI_STEP, null, "MAPIC.checkout", null, 20));

        assertThat(query.sql()).contains("r.operation_id IS NULL", "lower(btrim(d.tc_code)) = lower(:tcCode)",
                        "d.id = :operationId OR d.connection_operation_id = :operationId",
                        "e.bi_step_code IS NOT NULL AND btrim(e.bi_step_code) <> ''")
                .doesNotContain("product.operation o");
    }

    @Test
    @DisplayName("method сравнивается с сигнатурой discovered_operation и сопоставленной product.operation")
    void methodComparesBothSignatures() {
        E2eSearchRepository.Query query = E2eSearchRepository.build(
                new Criteria(BiStepFilter.ALL, null, null, "GET getItems", null));

        assertThat(query.sql()).contains("LEFT JOIN product.operation o ON o.id = d.connection_operation_id",
                "coalesce(d.type, '') || ' ' || coalesce(d.name, '')",
                "coalesce(o.type, '') || ' ' || coalesce(o.name, '')", "r.operation_id IS NULL");
        assertThat(query.params().getValue("method")).isEqualTo("GET getItems");
    }
}
