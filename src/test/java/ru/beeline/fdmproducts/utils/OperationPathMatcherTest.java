package ru.beeline.fdmproducts.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OperationPathMatcherTest {

    @Test
    @DisplayName("Разные имена path-параметров считаются одним методом")
    void treatsDifferentParameterNamesAsTheSameMethod() {
        assertThat(OperationPathMatcher.matches("/api/v1/product/{code}", "/api/v1/product/{cmdb}")).isTrue();
        assertThat(OperationPathMatcher.matches("/api/v1/info/{userId}", "/api/v1/info/{employId}")).isTrue();
    }

    @Test
    @DisplayName("Подчёркивание и процент — обычные символы, а не джокеры")
    void doesNotTreatLikeWildcardsAsWildcards() {
        assertThat(OperationPathMatcher.matches("/api/v1/dpXchannelXprovider/getXtp",
                "/api/v1/dp_channel_provider/get_tp")).isFalse();
        assertThat(OperationPathMatcher.matches("/api/v1/product/{code}/source", "/api/v1/product/%")).isFalse();
    }

    @Test
    @DisplayName("Query-параметры отбрасываются с обеих сторон")
    void ignoresQueryParameters() {
        assertThat(OperationPathMatcher.matches("/api/v1/product/{code}",
                "/api/v1/product/{cmdb}?force=true")).isTrue();
        assertThat(OperationPathMatcher.matches("/api/v1/systems?limit=10", "/api/v1/systems")).isTrue();
    }

    @Test
    @DisplayName("Слэш в конце значим")
    void keepsTheTrailingSlashSignificant() {
        assertThat(OperationPathMatcher.matches("/api/v1/product/{code}", "/api/v1/product/{code}/")).isFalse();
        assertThat(OperationPathMatcher.matches("/api/v1/calls/", "/api/v1/calls/")).isTrue();
    }

    @Test
    @DisplayName("Конкретное значение совпадает с шаблоном каталога")
    void matchesAConcreteValueAgainstACatalogTemplate() {
        assertThat(OperationPathMatcher.matches("/api/v1/product/{code}", "/api/v1/product/123")).isTrue();
        assertThat(OperationPathMatcher.matches("/api/v4/systems/{code}/purpose",
                "/api/v4/systems/BLN/purpose")).isTrue();
    }

    @Test
    @DisplayName("Шаблон запроса не совпадает с конкретным значением каталога")
    void doesNotMatchARequestTemplateAgainstAConcreteCatalogValue() {
        assertThat(OperationPathMatcher.matches("/api/v1/product/info", "/api/v1/product/{code}")).isFalse();
        assertThat(OperationPathMatcher.matches("/api/v1/product/current", "/api/v1/product/{id}")).isFalse();
    }

    @Test
    @DisplayName("Шаблон каталога не поглощает несколько сегментов")
    void doesNotLetATemplateSpanSeveralSegments() {
        assertThat(OperationPathMatcher.matches("/api/v1/product/{code}", "/api/v1/product/123/workspace")).isFalse();
        assertThat(OperationPathMatcher.matches("/api/v1/product/{code}", "/api/v1/product")).isFalse();
    }

    @Test
    @DisplayName("Регистр пути и типа не важен")
    void ignoresCase() {
        assertThat(OperationPathMatcher.matches("/api/v1/product/{code}", "/API/V1/Product/{code}")).isTrue();
        assertThat(OperationPathMatcher.matches("/api/v1/product/{code}", "/API/V1/PRODUCT/123")).isTrue();
        assertThat(OperationPathMatcher.typeMatches("get", "GET")).isTrue();
        assertThat(OperationPathMatcher.typeMatches("GET", "POST")).isFalse();
    }

    @Test
    @DisplayName("Пустой тип и пустое имя ничему не соответствуют")
    void rejectsMissingValues() {
        assertThat(OperationPathMatcher.matches(null, "/api/v1/product/{code}")).isFalse();
        assertThat(OperationPathMatcher.matches("/api/v1/product/{code}", null)).isFalse();
        assertThat(OperationPathMatcher.typeMatches(null, "GET")).isFalse();
        assertThat(OperationPathMatcher.typeMatches("GET", null)).isFalse();
    }

    @Test
    @DisplayName("Регулярные метасимволы в пути каталога не ломают сравнение")
    void escapesRegexMetaCharactersFromTheCatalogPath() {
        assertThat(OperationPathMatcher.matches("/api/v1/a.b/{code}", "/api/v1/a.b/123")).isTrue();
        assertThat(OperationPathMatcher.matches("/api/v1/a.b/{code}", "/api/v1/axb/123")).isFalse();
    }
}
