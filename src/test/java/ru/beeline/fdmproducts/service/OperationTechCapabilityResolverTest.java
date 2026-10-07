/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.beeline.fdmproducts.client.CapabilityClient;
import ru.beeline.fdmproducts.domain.DiscoveredOperation;
import ru.beeline.fdmproducts.domain.Operation;
import ru.beeline.fdmproducts.dto.OperationTechCapabilityDTO;
import ru.beeline.fdmproducts.dto.TcDTO;
import ru.beeline.fdmproducts.exception.ExternalServiceException;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OperationTechCapabilityResolverTest {

    private static final TcDTO TC = TcDTO.builder().id(98).code("TC.00.00.00.98").name("Перенос средств").build();

    private final CapabilityClient capabilityClient = mock(CapabilityClient.class);
    private final OperationTechCapabilityResolver resolver = new OperationTechCapabilityResolver(capabilityClient);

    @Test
    @DisplayName("ТВ запрашиваются одним вызовом по уникальным tc_id")
    void loadsTechCapabilitiesOnceByDistinctIds() {
        when(capabilityClient.getTcs(List.of(98))).thenReturn(List.of(TC));

        Map<Integer, TcDTO> result = resolver.load(List.of(operation(98), operation(98), operation(null)));

        assertThat(result).containsEntry(98, TC);
    }

    @Test
    @DisplayName("Нет tc_id — capability не вызывается")
    void skipsCapabilityWithoutIds() {
        assertThat(resolver.load(List.of(operation(null)))).isEmpty();
        verify(capabilityClient, never()).getTcs(anyList());
    }

    @Test
    @DisplayName("Строгая загрузка: capability недоступен — 502")
    void failsStrictLoadWhenCapabilityIsUnavailable() {
        when(capabilityClient.getTcs(anyList())).thenThrow(new RuntimeException("timeout"));

        assertThatThrownBy(() -> resolver.load(List.of(operation(98))))
                .isInstanceOf(ExternalServiceException.class)
                .hasMessage("Сервис capability недоступен");
    }

    @Test
    @DisplayName("Мягкая загрузка: capability недоступен — пустая карта без ошибки")
    void returnsEmptyOnLenientLoadWhenCapabilityIsUnavailable() {
        when(capabilityClient.getTcs(anyList())).thenThrow(new RuntimeException("timeout"));

        assertThat(resolver.loadOrEmpty(List.of(operation(98)))).isEmpty();
    }

    @Test
    @DisplayName("ТВ архитектурного метода — по tc_id, без tc_id или без ответа capability — null")
    void resolvesAnArchitectureMethod() {
        OperationTechCapabilityDTO tc = OperationTechCapabilityResolver.of(operation(98), Map.of(98, TC));

        assertThat(tc.getId()).isEqualTo(98);
        assertThat(tc.getCode()).isEqualTo("TC.00.00.00.98");
        assertThat(tc.getName()).isEqualTo("Перенос средств");
        assertThat(OperationTechCapabilityResolver.of(operation(null), Map.of(98, TC))).isNull();
        assertThat(OperationTechCapabilityResolver.of(operation(98), Map.of())).isNull();
    }

    @Test
    @DisplayName("ТВ обнаруженного метода — от связанного метода, иначе tc_code и tc_description, иначе null")
    void resolvesADiscoveredMethod() {
        DiscoveredOperation withOwnCode = DiscoveredOperation.builder().tcCode("TC.07").tcDescription("Своя ТВ").build();
        DiscoveredOperation empty = DiscoveredOperation.builder().build();

        assertThat(OperationTechCapabilityResolver.of(withOwnCode, operation(98), Map.of(98, TC)).getId())
                .isEqualTo(98);
        OperationTechCapabilityDTO own = OperationTechCapabilityResolver.of(withOwnCode, null, Map.of());
        assertThat(own.getId()).isNull();
        assertThat(own.getCode()).isEqualTo("TC.07");
        assertThat(own.getName()).isEqualTo("Своя ТВ");
        assertThat(OperationTechCapabilityResolver.of(empty, null, Map.of())).isNull();
    }

    private static Operation operation(Integer tcId) {
        return Operation.builder().id(541).tcId(tcId).build();
    }
}
