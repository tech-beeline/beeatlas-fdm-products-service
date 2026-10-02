/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.beeline.fdmproducts.dto.discovered.UseCaseDiscoveredInterfaceDTO;
import ru.beeline.fdmproducts.exception.ExternalServiceException;
import ru.beeline.fdmproducts.jackson.ConnectionOperationIdDeserializer;
import ru.beeline.fdmproducts.service.UseCaseDiscoveredInterfaceService;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UseCaseOperationsControllerTest {

    private static final String URL = "/api/v1/use-case/operations?use-case-id=42";

    @Mock
    private UseCaseDiscoveredInterfaceService service;

    @InjectMocks
    private UseCaseOperationsController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new CustomExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("Тела нет — в сервис уходит null, сервис отдаёт «Отсутствует тело запроса»")
    void missingBodyGoesToServiceAsNull() throws Exception {
        when(service.syncUseCaseOperations(anyString(), isNull()))
                .thenThrow(new IllegalArgumentException("Отсутствует тело запроса"));

        mockMvc.perform(put(URL).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorMessage").value("Отсутствует тело запроса"));

        verify(service).syncUseCaseOperations("42", null);
    }

    @ParameterizedTest(name = "connectionOperationId={0} -> 400, поле должно быть целым")
    @MethodSource("nonIntegerConnectionOperationIds")
    @DisplayName("Нецелое connectionOperationId — 400 с текстом контракта, сервис не вызывается")
    void nonIntegerConnectionOperationId(String rawValue) throws Exception {
        String body = "[{\"interfaceCode\":\"X\",\"productPlanned\":\"PLAN\",\"operations\":["
                + "{\"name\":\"opA\",\"type\":\"GET\",\"connectionOperationId\":" + rawValue + "}]}]";

        mockMvc.perform(put(URL).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorMessage")
                        .value(ConnectionOperationIdDeserializer.NOT_INTEGER_MESSAGE));

        verify(service, never()).syncUseCaseOperations(any(), any());
    }

    @Test
    @DisplayName("Целый connectionOperationId доходит до сервиса как Integer")
    void integerConnectionOperationIdIsPassedThrough() throws Exception {
        when(service.syncUseCaseOperations(anyString(), any())).thenReturn(List.of());
        String body = "[{\"interfaceCode\":\"X\",\"productPlanned\":\"PLAN\",\"operations\":["
                + "{\"name\":\"opA\",\"type\":\"GET\",\"connectionOperationId\":42}]}]";

        mockMvc.perform(put(URL).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<UseCaseDiscoveredInterfaceDTO>> captor = ArgumentCaptor.forClass(List.class);
        verify(service).syncUseCaseOperations(eq("42"), captor.capture());
        assertThat(captor.getValue().get(0).getOperations().get(0).getConnectionOperationId()).isEqualTo(42);
    }

    private static Stream<String> nonIntegerConnectionOperationIds() {
        return Stream.of("\"abc\"", "true", "{}", "[]", "2147483648", "1.5", "1.0");
    }

    @Test
    @DisplayName("Ошибка сервиса project отдаётся со своим статусом и текстом")
    void externalServiceErrorKeepsStatusAndMessage() throws Exception {
        when(service.syncUseCaseOperations(anyString(), any()))
                .thenThrow(new ExternalServiceException(HttpStatus.BAD_GATEWAY, "Ошибка сервиса project"));

        mockMvc.perform(put(URL).contentType(MediaType.APPLICATION_JSON).content("[]"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.errorMessage").value("Ошибка сервиса project"));
    }
}
