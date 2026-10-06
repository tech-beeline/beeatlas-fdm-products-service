/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.beeline.fdmproducts.domain.E2e;
import ru.beeline.fdmproducts.dto.e2e.E2eByBiStepCodesGroupDTO;
import ru.beeline.fdmproducts.repository.E2eRepository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class E2eByBiStepCodesServiceTest {

    @Mock
    private E2eRepository e2eRepository;

    @InjectMocks
    private E2eByBiStepCodesService service;

    @Test
    @DisplayName("Группирует e2e по коду из запроса: id, name, code, source; сортировка по id")
    void groupsE2eByRequestCodeSortedById() {
        when(e2eRepository.findAllByBiStepCodeInIgnoreCase(anyCollection())).thenReturn(List.of(
                e2e(4, "checkout-flow-mapic", "Оформление заказа, запасной контур", "Step.00.00.02.77", "MAPIC"),
                e2e(1, "checkout-flow", "Оформление заказа", "Step.00.00.02.77", "SPARX"),
                e2e(2, "other", "Другой", "Step.00.00.02.99", "SPARX")
        ));

        List<E2eByBiStepCodesGroupDTO> result = service.getByBiStepCodes(
                List.of("Step.00.00.02.77", "Step.00.00.02.84"));

        assertThat(result).hasSize(2);

        E2eByBiStepCodesGroupDTO first = result.get(0);
        assertThat(first.getBiStepCode()).isEqualTo("Step.00.00.02.77");
        assertThat(first.getE2e()).hasSize(2);
        assertThat(first.getE2e().get(0).getId()).isEqualTo(1);
        assertThat(first.getE2e().get(0).getName()).isEqualTo("Оформление заказа");
        assertThat(first.getE2e().get(0).getCode()).isEqualTo("checkout-flow");
        assertThat(first.getE2e().get(0).getSource()).isEqualTo("SPARX");
        assertThat(first.getE2e().get(1).getId()).isEqualTo(4);
        assertThat(first.getE2e().get(1).getCode()).isEqualTo("checkout-flow-mapic");
        assertThat(first.getE2e().get(1).getSource()).isEqualTo("MAPIC");

        E2eByBiStepCodesGroupDTO second = result.get(1);
        assertThat(second.getBiStepCode()).isEqualTo("Step.00.00.02.84");
        assertThat(second.getE2e()).isEmpty();
    }

    @Test
    @DisplayName("Сопоставление bi_step_code без учёта регистра; ключ группы — код из запроса")
    void matchesBiStepCodeIgnoreCasePreservingRequestCasing() {
        when(e2eRepository.findAllByBiStepCodeInIgnoreCase(anyCollection())).thenReturn(List.of(
                e2e(1, "checkout-flow", "Оформление заказа", "step.00.00.02.77", "SPARX")
        ));

        List<E2eByBiStepCodesGroupDTO> result = service.getByBiStepCodes(List.of("Step.00.00.02.77"));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getBiStepCode()).isEqualTo("Step.00.00.02.77");
        assertThat(result.get(0).getE2e()).hasSize(1);
        assertThat(result.get(0).getE2e().get(0).getId()).isEqualTo(1);
    }

    @Test
    @DisplayName("name может быть null; description и biStepCode карточки не отдаются")
    void allowsNullNameAndOmitsDescriptionAndBiStepCode() {
        when(e2eRepository.findAllByBiStepCodeInIgnoreCase(anyCollection())).thenReturn(List.of(
                e2e(1, "checkout-flow", null, "Step.00.00.02.77", "SPARX")
        ));

        List<E2eByBiStepCodesGroupDTO> result = service.getByBiStepCodes(List.of("Step.00.00.02.77"));

        assertThat(result.get(0).getE2e().get(0).getName()).isNull();
        assertThat(result.get(0).getE2e().get(0).getCode()).isEqualTo("checkout-flow");
    }

    @Test
    @DisplayName("Дубликаты после trim схлопываются, порядок — первое вхождение; в репозиторий уходят lower-коды")
    void deduplicatesAfterTrimAndPreservesOrder() {
        when(e2eRepository.findAllByBiStepCodeInIgnoreCase(anyCollection())).thenReturn(List.of());

        List<E2eByBiStepCodesGroupDTO> result = service.getByBiStepCodes(
                List.of("  Step.B  ", "Step.A", "Step.B", "Step.A"));

        assertThat(result).extracting(E2eByBiStepCodesGroupDTO::getBiStepCode)
                .containsExactly("Step.B", "Step.A");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<String>> codesCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(e2eRepository).findAllByBiStepCodeInIgnoreCase(codesCaptor.capture());
        assertThat(codesCaptor.getValue()).containsExactly("step.b", "step.a");
    }

    @Test
    @DisplayName("Валидация тела: null, пустой список, null/пустой элемент")
    void validatesRequestBody() {
        assertThatThrownBy(() -> service.getByBiStepCodes(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Отсутствует тело запроса");
        assertThatThrownBy(() -> service.getByBiStepCodes(List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Список кодов bi-step не может быть пустым");

        List<String> withNull = new ArrayList<>();
        withNull.add(null);
        assertThatThrownBy(() -> service.getByBiStepCodes(withNull))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Код bi-step не может быть пустым");

        assertThatThrownBy(() -> service.getByBiStepCodes(List.of("   ")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Код bi-step не может быть пустым");
    }

    private static E2e e2e(Integer id, String code, String name, String biStepCode, String source) {
        return E2e.builder()
                .id(id)
                .code(code)
                .name(name)
                .description("не должно попасть в ответ")
                .biStepCode(biStepCode)
                .source(source)
                .build();
    }
}
