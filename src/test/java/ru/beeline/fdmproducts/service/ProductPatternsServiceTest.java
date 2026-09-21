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
import ru.beeline.fdmproducts.client.TechradarClient;
import ru.beeline.fdmproducts.domain.EnumSourceType;
import ru.beeline.fdmproducts.domain.Product;
import ru.beeline.fdmproducts.domain.ProductPatterns;
import ru.beeline.fdmproducts.dto.PatternDTO;
import ru.beeline.fdmproducts.dto.PostPatternProductDTO;
import ru.beeline.fdmproducts.dto.ProductPatternV2DTO;
import ru.beeline.fdmproducts.exception.EntityNotFoundException;
import ru.beeline.fdmproducts.exception.ValidationException;
import ru.beeline.fdmproducts.repository.EnumSourceTypeRepository;
import ru.beeline.fdmproducts.repository.ProductPatternsRepository;
import ru.beeline.fdmproducts.repository.ProductRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductPatternsServiceTest {

    private static final String ALIAS = "fdmshowcaseapp";
    private static final String SOURCE_NFR = "nfr";
    private static final String SOURCE_PIPELINE = "pipeline";

    @Mock
    private ProductRepository productRepository;
    @Mock
    private EnumSourceTypeRepository enumSourceTypeRepository;
    @Mock
    private ProductPatternsRepository productPatternsRepository;
    @Mock
    private TechradarClient techradarClient;

    @InjectMocks
    private ProductPatternsService service;

    @Test
    @DisplayName("Первый вызов создаёт актуальную запись с branch=main")
    void firstCallInsertsActualWithMainBranch() {
        givenProduct();
        givenSource(SOURCE_NFR, false);
        when(productPatternsRepository.findActual(eq(ALIAS), eq("main"), eq(2), isNull(), eq("PAT-001")))
                .thenReturn(Optional.empty());
        when(productPatternsRepository.save(any())).thenAnswer(inv -> {
            ProductPatterns p = inv.getArgument(0);
            p.setId(10);
            return p;
        });

        service.postPatternProductV2(ALIAS, SOURCE_NFR, null, null,
                List.of(item("PAT-001", true, "ok")));

        ArgumentCaptor<ProductPatterns> captor = ArgumentCaptor.forClass(ProductPatterns.class);
        verify(productPatternsRepository).save(captor.capture());
        ProductPatterns saved = captor.getValue();
        assertThat(saved.getProductAlias()).isEqualTo(ALIAS);
        assertThat(saved.getProductBranch()).isEqualTo("main");
        assertThat(saved.getPatternCode()).isEqualTo("PAT-001");
        assertThat(saved.getIsCheck()).isTrue();
        assertThat(saved.getResultDetails()).isEqualTo("ok");
        assertThat(saved.getSourceTypeId()).isEqualTo(2);
        assertThat(saved.getSourceId()).isNull();
        assertThat(saved.getIsActual()).isTrue();
        assertThat(saved.getCreatedDate()).isNotNull();
    }

    @Test
    @DisplayName("Повторный вызов: прежняя is_actual=false, новая is_actual=true")
    void secondCallDeactivatesPrevious() {
        givenProduct();
        givenSource(SOURCE_NFR, false);
        ProductPatterns previous = ProductPatterns.builder()
                .id(1)
                .patternCode("PAT-001")
                .isCheck(false)
                .productAlias(ALIAS)
                .productBranch("main")
                .sourceTypeId(2)
                .isActual(true)
                .build();
        when(productPatternsRepository.findActual(eq(ALIAS), eq("main"), eq(2), isNull(), eq("PAT-001")))
                .thenReturn(Optional.of(previous));
        when(productPatternsRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.postPatternProductV2(ALIAS, SOURCE_NFR, "MAIN", null,
                List.of(item("PAT-001", true, "new")));

        ArgumentCaptor<ProductPatterns> captor = ArgumentCaptor.forClass(ProductPatterns.class);
        verify(productPatternsRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues().get(0).getIsActual()).isFalse();
        ProductPatterns created = captor.getAllValues().get(1);
        assertThat(created.getIsActual()).isTrue();
        assertThat(created.getIsCheck()).isTrue();
        assertThat(created.getResultDetails()).isEqualTo("new");
        assertThat(created.getProductBranch()).isEqualTo("main");
    }

    @Test
    @DisplayName("Пустой массив — успех без записи в БД")
    void emptyBodyDoesNothing() {
        givenProduct();
        givenSource(SOURCE_NFR, false);

        service.postPatternProductV2(ALIAS, SOURCE_NFR, null, null, List.of());

        verify(productPatternsRepository, never()).save(any());
        verify(productPatternsRepository, never()).findActual(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("identifySource=true без source-id — 400")
    void identifySourceRequiresSourceId() {
        givenProduct();
        givenSource(SOURCE_PIPELINE, true);

        assertThatThrownBy(() -> service.postPatternProductV2(ALIAS, SOURCE_PIPELINE, null, null,
                List.of(item("PAT-001", true, null))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Для указанного источника обязательна передача идентификатора");
        verify(productPatternsRepository, never()).save(any());
    }

    @Test
    @DisplayName("Неизвестный продукт — 404")
    void unknownProduct404() {
        when(productRepository.findByAliasCaseInsensitive(ALIAS)).thenReturn(null);

        assertThatThrownBy(() -> service.postPatternProductV2(ALIAS, SOURCE_NFR, null, null, List.of()))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Указанный продукт не существует");
    }

    @Test
    @DisplayName("Неизвестный source-type — 400")
    void unknownSourceType400() {
        givenProduct();
        when(enumSourceTypeRepository.findByName("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.postPatternProductV2(ALIAS, "unknown", null, null, List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("невозможный источник");
    }

    @Test
    @DisplayName("Невалидное тело — ValidationException как в v1")
    void invalidBody409() {
        assertThatThrownBy(() -> service.postPatternProductV2(ALIAS, SOURCE_NFR, null, null,
                List.of(PostPatternProductDTO.builder().code(" ").isCheck(null).build())))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("code")
                .hasMessageContaining("isCheck");
        verify(productRepository, never()).findByAliasCaseInsensitive(any());
    }

    @Test
    @DisplayName("GET v2: актуальные записи с обогащением из Techradar и без него")
    void getReturnsActualWithAndWithoutTechradarCard() {
        givenProduct();
        when(productPatternsRepository.findAllByProductAliasAndProductBranchAndIsActualTrue(ALIAS, "main"))
                .thenReturn(List.of(
                        ProductPatterns.builder()
                                .patternCode("PAT-001")
                                .isCheck(true)
                                .resultDetails("ok")
                                .build(),
                        ProductPatterns.builder()
                                .patternCode("PAT-002")
                                .isCheck(false)
                                .resultDetails(null)
                                .build()));
        when(techradarClient.getPatternsAutoCheck()).thenReturn(List.of(
                PatternDTO.builder().id(11).code("PAT-001").name("Pattern One").rule("r1").isAntiPattern(false).build()));

        List<ProductPatternV2DTO> result = service.getProductPatternsV2(ALIAS, null);

        assertThat(result).hasSize(2);
        ProductPatternV2DTO enriched = result.get(0);
        assertThat(enriched.getCode()).isEqualTo("PAT-001");
        assertThat(enriched.getIsCheck()).isTrue();
        assertThat(enriched.getResultDetails()).isEqualTo("ok");
        assertThat(enriched.getId()).isEqualTo(11);
        assertThat(enriched.getName()).isEqualTo("Pattern One");
        ProductPatternV2DTO onlyDb = result.get(1);
        assertThat(onlyDb.getCode()).isEqualTo("PAT-002");
        assertThat(onlyDb.getIsCheck()).isFalse();
        assertThat(onlyDb.getId()).isNull();
        assertThat(onlyDb.getName()).isNull();
    }

    @Test
    @DisplayName("GET v2: нет актуальных — 200, []")
    void getEmptyWhenNoActual() {
        givenProduct();
        when(productPatternsRepository.findAllByProductAliasAndProductBranchAndIsActualTrue(ALIAS, "feature"))
                .thenReturn(List.of());

        assertThat(service.getProductPatternsV2(ALIAS, "FEATURE")).isEmpty();
        verify(techradarClient, never()).getPatternsAutoCheck();
    }

    @Test
    @DisplayName("GET v2: неизвестный alias — 404")
    void getUnknownProduct404() {
        when(productRepository.findByAliasCaseInsensitive(ALIAS)).thenReturn(null);

        assertThatThrownBy(() -> service.getProductPatternsV2(ALIAS, null))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Продукт c alias 'fdmshowcaseapp' не найден");
    }

    @Test
    @DisplayName("GET v2: Techradar null — ответ всё равно из БД")
    void getWorksWhenTechradarNull() {
        givenProduct();
        when(productPatternsRepository.findAllByProductAliasAndProductBranchAndIsActualTrue(ALIAS, "main"))
                .thenReturn(List.of(ProductPatterns.builder()
                        .patternCode("PAT-001")
                        .isCheck(true)
                        .build()));
        when(techradarClient.getPatternsAutoCheck()).thenReturn(null);

        List<ProductPatternV2DTO> result = service.getProductPatternsV2(ALIAS, " ");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCode()).isEqualTo("PAT-001");
        assertThat(result.get(0).getIsCheck()).isTrue();
        assertThat(result.get(0).getId()).isNull();
    }

    private void givenProduct() {
        when(productRepository.findByAliasCaseInsensitive(ALIAS))
                .thenReturn(Product.builder().id(1).alias(ALIAS).name("Showcase").build());
    }

    private void givenSource(String name, boolean identifySource) {
        when(enumSourceTypeRepository.findByName(name))
                .thenReturn(Optional.of(EnumSourceType.builder()
                        .id(identifySource ? 1 : 2)
                        .name(name)
                        .identifySource(identifySource)
                        .build()));
    }

    private PostPatternProductDTO item(String code, Boolean isCheck, String details) {
        return PostPatternProductDTO.builder()
                .code(code)
                .isCheck(isCheck)
                .resultDetails(details)
                .build();
    }
}
