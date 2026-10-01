/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.controller;

import com.fasterxml.jackson.databind.JsonNode;
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
import ru.beeline.fdmproducts.service.DiscoveredOperationByIdsService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class DiscoveredOperationControllerTest {

    private static final String URL = "/api/v1/discovered-operation/by-ids";

    @Mock
    private DiscoveredOperationByIdsService service;

    @InjectMocks
    private DiscoveredOperationController controller;

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
        when(service.getByIds(isNull()))
                .thenThrow(new IllegalArgumentException("Отсутствует тело запроса"));

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorMessage").value("Отсутствует тело запроса"));

        verify(service).getByIds(null);
    }

    @Test
    @DisplayName("Недоступность capability — 502")
    void capabilityUnavailableIsBadGateway() throws Exception {
        when(service.getByIds(any(JsonNode.class)))
                .thenThrow(new ExternalServiceException(HttpStatus.BAD_GATEWAY, "Сервис capability недоступен"));

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("[20]"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.errorMessage").value("Сервис capability недоступен"));
    }
}
