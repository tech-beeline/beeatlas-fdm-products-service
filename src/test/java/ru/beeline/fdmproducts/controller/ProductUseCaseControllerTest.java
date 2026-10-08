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
import ru.beeline.fdmproducts.dto.usecase.UseCaseGroupedByUseCaseDTO;
import ru.beeline.fdmproducts.exception.EntityNotFoundException;
import ru.beeline.fdmproducts.exception.ExternalServiceException;
import ru.beeline.fdmproducts.service.UseCaseGroupedByUseCaseService;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ProductUseCaseControllerTest {

    private static final String URL = "/api/v1/product/use-cases/grouped-by-use-case";

    @Mock
    private UseCaseGroupedByUseCaseService service;

    @InjectMocks
    private ProductUseCaseController controller;

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
        when(service.findGroupedByUseCase(eq("1"), isNull()))
                .thenReturn(List.of(UseCaseGroupedByUseCaseDTO.builder()
                        .id(7)
                        .code("UC-ORDER")
                        .name("Оформление заказа")
                        .operations(List.of())
                        .build()));

        mockMvc.perform(get(URL).param("id", "1").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(7))
                .andExpect(jsonPath("$[0].code").value("UC-ORDER"));

        verify(service).findGroupedByUseCase("1", null);
    }

    @Test
    @DisplayName("400 — оба параметра")
    void bothParams() throws Exception {
        when(service.findGroupedByUseCase(eq("1"), eq("alias")))
                .thenThrow(new IllegalArgumentException("Нужно передать либо идентификатор, либо alias продукта"));

        mockMvc.perform(get(URL).param("id", "1").param("alias", "alias"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorMessage")
                        .value("Нужно передать либо идентификатор, либо alias продукта"));
    }

    @Test
    @DisplayName("404 — продукт не найден")
    void notFound() throws Exception {
        when(service.findGroupedByUseCase(isNull(), eq("unknown")))
                .thenThrow(new EntityNotFoundException("Продукт не найден."));

        mockMvc.perform(get(URL).param("alias", "unknown"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorMessage").value("Продукт не найден."));
    }

    @Test
    @DisplayName("502 — capability недоступен")
    void capabilityUnavailable() throws Exception {
        when(service.findGroupedByUseCase(eq("1"), isNull()))
                .thenThrow(new ExternalServiceException(HttpStatus.BAD_GATEWAY, "Сервис capability недоступен"));

        mockMvc.perform(get(URL).param("id", "1"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.errorMessage").value("Сервис capability недоступен"));
    }
}
