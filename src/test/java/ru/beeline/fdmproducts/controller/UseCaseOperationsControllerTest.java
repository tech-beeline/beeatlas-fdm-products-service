/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.beeline.fdmproducts.exception.ExternalServiceException;
import ru.beeline.fdmproducts.service.UseCaseDiscoveredInterfaceService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
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
