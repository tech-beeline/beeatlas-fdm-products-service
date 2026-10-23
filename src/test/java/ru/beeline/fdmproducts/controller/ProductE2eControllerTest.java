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
import ru.beeline.fdmproducts.dto.e2e.E2eGroupedByE2eDTO;
import ru.beeline.fdmproducts.exception.EntityNotFoundException;
import ru.beeline.fdmproducts.exception.ExternalServiceException;
import ru.beeline.fdmproducts.service.E2eGroupedByE2eService;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ProductE2eControllerTest {

    private static final String URL = "/api/v1/product/e2e/grouped-by-e2e";

    @Mock
    private E2eGroupedByE2eService service;

    @InjectMocks
    private ProductE2eController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new CustomExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("200 — успешный ответ")
    void success() throws Exception {
        when(service.findGroupedByE2e(eq("1"), isNull(), isNull()))
                .thenReturn(List.of(E2eGroupedByE2eDTO.builder()
                        .id(1)
                        .code("checkout-flow")
                        .name("Оформление заказа")
                        .source("SPARX")
                        .operations(List.of())
                        .build()));

        mockMvc.perform(get(URL).param("id", "1").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].source").value("SPARX"));

        verify(service).findGroupedByE2e("1", null, null);
    }

    @Test
    @DisplayName("400 — оба параметра")
    void bothParams() throws Exception {
        when(service.findGroupedByE2e(eq("1"), eq("alias"), isNull()))
                .thenThrow(new IllegalArgumentException("Нужно передать либо идентификатор, либо alias продукта"));

        mockMvc.perform(get(URL).param("id", "1").param("alias", "alias"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorMessage")
                        .value("Нужно передать либо идентификатор, либо alias продукта"));
    }

    @Test
    @DisplayName("404 — продукт не найден")
    void notFound() throws Exception {
        when(service.findGroupedByE2e(isNull(), eq("unknown"), isNull()))
                .thenThrow(new EntityNotFoundException("Продукт не найден."));

        mockMvc.perform(get(URL).param("alias", "unknown"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorMessage").value("Продукт не найден."));
    }

    @Test
    @DisplayName("502 — capability недоступен")
    void capabilityUnavailable() throws Exception {
        when(service.findGroupedByE2e(eq("1"), isNull(), isNull()))
                .thenThrow(new ExternalServiceException(HttpStatus.BAD_GATEWAY, "Сервис capability недоступен"));

        mockMvc.perform(get(URL).param("id", "1"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.errorMessage").value("Сервис capability недоступен"));
    }
}
