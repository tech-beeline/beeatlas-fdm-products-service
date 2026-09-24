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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import ru.beeline.fdmproducts.domain.Product;
import ru.beeline.fdmproducts.dto.search.MatchedArchOperationDTO;
import ru.beeline.fdmproducts.dto.search.OperationMatchCandidateDTO;
import ru.beeline.fdmproducts.dto.search.projection.ArchOperationProjection;
import ru.beeline.fdmproducts.mapper.ArchOperationMapper;
import ru.beeline.fdmproducts.repository.ProductRepository;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** POST /api/v1/operation/search-matched. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SearchServiceMatchedOperationsTest {

    private static final String CODE = "orders-product";
    private static final List<Integer> INTERFACE_IDS = List.of(101, 102);

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ArchOperationMatchingService archOperationMatchingService;

    @Spy
    private ArchOperationMapper archOperationMapper = new ArchOperationMapper();

    @InjectMocks
    private SearchService searchService;

    private Product givenProduct(String alias) {
        Product product = Product.builder().id(1).name("Заказы").alias(alias).build();
        when(productRepository.findByAliasCaseInsensitive(any())).thenReturn(product);
        when(archOperationMatchingService.resolveInterfaceIds(alias)).thenReturn(INTERFACE_IDS);
        return product;
    }

    private OperationMatchCandidateDTO candidate(String methodName, String methodType, String protocol, String code) {
        return OperationMatchCandidateDTO.builder()
                .methodName(methodName)
                .methodType(methodType)
                .protocol(protocol)
                .productCode(code)
                .build();
    }

    @Test
    @DisplayName("Найденные операции отдаются с цепочкой интерфейс/контейнер/продукт и кодом продукта из запроса")
    void returnsMatchedOperations() {
        givenProduct(CODE);
        List<ArchOperationProjection> matches = List.of(projection());
        when(archOperationMatchingService.findMatches("/api/v1/orders", "GET", "REST", INTERFACE_IDS))
                .thenReturn(matches);

        List<MatchedArchOperationDTO> result = searchService.searchMatchedOperations(
                List.of(candidate("/api/v1/orders", "GET", "REST", CODE)));

        assertThat(result).hasSize(1);
        MatchedArchOperationDTO operation = result.get(0);
        assertThat(operation.getId()).isEqualTo(5);
        assertThat(operation.getName()).isEqualTo("/api/v1/orders");
        assertThat(operation.getType()).isEqualTo("GET");
        assertThat(operation.getProductCode()).isEqualTo(CODE);
        assertThat(operation.getInterfaceObj().getId()).isEqualTo(101);
        assertThat(operation.getInterfaceObj().getCode()).isEqualTo("orders-api");
        assertThat(operation.getContainer().getId()).isEqualTo(11);
        assertThat(operation.getProduct().getAlias()).isEqualTo(CODE);
        assertThat(operation.getProduct().getBranchName()).isEqualTo("main");
        assertThat(operation.getError()).isNull();
        assertThat(operation.getNotFound()).isNull();
    }

    @Test
    @DisplayName("Незаполненные methodType и protocol снимают фильтр, а не ищутся как пустая строка")
    void blankOptionalFiltersBecomeNull() {
        givenProduct(CODE);

        searchService.searchMatchedOperations(List.of(candidate("createOrder", "  ", "", CODE)));

        verify(archOperationMatchingService).findMatches("createOrder", null, null, INTERFACE_IDS);
    }

    @Test
    @DisplayName("Неизвестный productCode — не 404, а элемент-признак в ответе 200")
    void unknownProductReturnsErrorElement() {
        when(productRepository.findByAliasCaseInsensitive("unknown-product")).thenReturn(null);

        List<MatchedArchOperationDTO> result = searchService.searchMatchedOperations(
                List.of(candidate("/api/v1/orders", "GET", "REST", "unknown-product")));

        assertThat(result).hasSize(1);
        MatchedArchOperationDTO error = result.get(0);
        assertThat(error.getProductCode()).isEqualTo("unknown-product");
        assertThat(error.getError()).isEqualTo("Продукт с кодом unknown-product не найден");
        assertThat(error.getNotFound()).isTrue();
        assertThat(error.getId()).isNull();
        verifyNoInteractions(archOperationMatchingService);
    }

    @Test
    @DisplayName("Кандидат без совпадений в ответ ничего не добавляет; отсутствие совпадений — не ошибка")
    void candidateWithoutMatchesAddsNothing() {
        givenProduct(CODE);
        when(archOperationMatchingService.findMatches(any(), any(), any(), anyList())).thenReturn(List.of());

        assertThat(searchService.searchMatchedOperations(List.of(candidate("/nope", "GET", null, CODE)))).isEmpty();
    }

    @Test
    @DisplayName("Результаты кандидатов идут подряд в порядке запроса, продукт резолвится один раз на код")
    void keepsCandidateOrderAndResolvesProductOnce() {
        givenProduct(CODE);
        List<ArchOperationProjection> first = List.of(projection(5, "first"));
        List<ArchOperationProjection> second = List.of(projection(6, "second"), projection(7, "second"));
        when(archOperationMatchingService.findMatches(eq("first"), any(), any(), anyList())).thenReturn(first);
        when(archOperationMatchingService.findMatches(eq("second"), any(), any(), anyList())).thenReturn(second);

        List<MatchedArchOperationDTO> result = searchService.searchMatchedOperations(List.of(
                candidate("first", "GET", null, CODE),
                candidate("second", "GET", null, CODE.toUpperCase())));

        assertThat(result).extracting(MatchedArchOperationDTO::getId).containsExactly(5, 6, 7);
        // Код продукта возвращается ровно тем, каким пришёл в кандидате, — по нему UI и связывает строки.
        assertThat(result).extracting(MatchedArchOperationDTO::getProductCode)
                .containsExactly(CODE, CODE.toUpperCase(), CODE.toUpperCase());
        verify(productRepository, times(1)).findByAliasCaseInsensitive(any());
    }

    @Test
    @DisplayName("Пустое тело и пустой массив — 400")
    void emptyBodyRejected() {
        assertThatThrownBy(() -> searchService.searchMatchedOperations(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("В теле запроса не передан обязательный атрибут");
        assertThatThrownBy(() -> searchService.searchMatchedOperations(List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Кандидат без methodName или productCode — 400, до запросов в БД дело не доходит")
    void missingRequiredFieldsRejected() {
        assertThatThrownBy(() -> searchService.searchMatchedOperations(
                List.of(candidate(null, "GET", null, CODE))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Отсутствует обязательное поле methodName");
        assertThatThrownBy(() -> searchService.searchMatchedOperations(
                List.of(candidate("/api/v1/orders", "GET", null, " "))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Отсутствует обязательное поле productCode");
        assertThatThrownBy(() -> searchService.searchMatchedOperations(Arrays.asList((OperationMatchCandidateDTO) null)))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(productRepository, archOperationMatchingService);
    }

    @Test
    @DisplayName("Невалидный кандидат в конце списка отменяет запрос целиком")
    void validatesWholeBodyBeforeQuerying() {
        givenProduct(CODE);

        assertThatThrownBy(() -> searchService.searchMatchedOperations(List.of(
                candidate("/api/v1/orders", "GET", null, CODE),
                candidate("", "GET", null, CODE))))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(productRepository, archOperationMatchingService);
    }

    private ArchOperationProjection projection() {
        return projection(5, "/api/v1/orders");
    }

    private ArchOperationProjection projection(int opId, String opName) {
        ArchOperationProjection projection = mock(ArchOperationProjection.class);
        when(projection.getOpId()).thenReturn(opId);
        when(projection.getOpName()).thenReturn(opName);
        when(projection.getOpType()).thenReturn("GET");
        when(projection.getInterfaceId()).thenReturn(101);
        when(projection.getInterfaceName()).thenReturn("Orders API");
        when(projection.getInterfaceCode()).thenReturn("orders-api");
        when(projection.getContainerId()).thenReturn(11);
        when(projection.getContainerName()).thenReturn("orders-backend");
        when(projection.getContainerCode()).thenReturn("obe");
        when(projection.getProductId()).thenReturn(1);
        when(projection.getProductName()).thenReturn("Заказы");
        when(projection.getProductAlias()).thenReturn(CODE);
        when(projection.getProductBranchName()).thenReturn("main");
        return projection;
    }
}
