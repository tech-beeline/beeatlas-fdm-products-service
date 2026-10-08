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
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.beeline.fdmproducts.dto.e2e.E2eByBiStepCodeItemDTO;
import ru.beeline.fdmproducts.dto.e2e.E2eByBiStepCodesGroupDTO;
import ru.beeline.fdmproducts.service.E2eByBiStepCodesService;
import ru.beeline.fdmproducts.service.E2eService;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class E2eByBiStepCodesControllerTest {

    private static final String URL = "/api/v1/e2e/by-bi-step-codes";

    @Mock
    private E2eService e2eService;

    @Mock
    private E2eByBiStepCodesService e2eByBiStepCodesService;

    @InjectMocks
    private E2eController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new CustomExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();
    }

    @Test
    @DisplayName("Тела нет — в сервис уходит null, сервис отдаёт «Отсутствует тело запроса»")
    void missingBodyGoesToServiceAsNull() throws Exception {
        when(e2eByBiStepCodesService.getByBiStepCodes(isNull()))
                .thenThrow(new IllegalArgumentException("Отсутствует тело запроса"));

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorMessage").value("Отсутствует тело запроса"));

        verify(e2eByBiStepCodesService).getByBiStepCodes(null);
    }

    @Test
    @DisplayName("Число в массиве — 400 «Код bi-step не может быть пустым», сервис не вызывается")
    void nonStringElementIsBadRequest() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[1]"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorMessage").value("Код bi-step не может быть пустым"));

        verify(e2eByBiStepCodesService, never()).getByBiStepCodes(anyList());
    }

    @Test
    @DisplayName("Успех — 200 и группы с biStepCode")
    void successReturnsGroups() throws Exception {
        when(e2eByBiStepCodesService.getByBiStepCodes(anyList())).thenReturn(List.of(
                E2eByBiStepCodesGroupDTO.builder()
                        .biStepCode("Step.00.00.02.77")
                        .e2e(List.of(E2eByBiStepCodeItemDTO.builder()
                                .id(1)
                                .name("Оформление заказа")
                                .code("checkout-flow")
                                .source("SPARX")
                                .build()))
                        .build(),
                E2eByBiStepCodesGroupDTO.builder()
                        .biStepCode("Step.00.00.02.84")
                        .e2e(List.of())
                        .build()
        ));

        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[\"Step.00.00.02.77\",\"Step.00.00.02.84\"]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].biStepCode").value("Step.00.00.02.77"))
                .andExpect(jsonPath("$[0].e2e[0].id").value(1))
                .andExpect(jsonPath("$[0].e2e[0].source").value("SPARX"))
                .andExpect(jsonPath("$[1].biStepCode").value("Step.00.00.02.84"))
                .andExpect(jsonPath("$[1].e2e").isEmpty());
    }
}
