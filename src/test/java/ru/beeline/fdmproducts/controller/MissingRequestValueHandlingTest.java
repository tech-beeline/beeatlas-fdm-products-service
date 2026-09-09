/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.beeline.fdmproducts.service.InfraService;
import ru.beeline.fdmproducts.service.ProductService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SFDM-4099: отсутствие обязательного query-параметра или заголовка отдавалось как 500
 * «Внутренняя ошибка сервера», потому что исключения привязки запроса — checked ServletException —
 * доходили до catch-all обработчика {@code Exception}.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MissingRequestValueHandlingTest {

    @Mock
    private ProductService productService;

    @Mock
    private InfraService infraService;

    @InjectMocks
    private ProductController productController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(productController)
                .setControllerAdvice(new CustomExceptionHandler())
                .build();
    }

    @ParameterizedTest(name = "{0} -> 400, отсутствует параметр {1}")
    @CsvSource({
            "/api/v1/product/by-ids,                    ids",
            "/api/v1/product/infra,                     name",
            "/api/v1/product/infra/contains,            name",
            "/api/v1/product/infra/search,              parameter",
            "/api/v1/product/infra/search?parameter=x,  value",
            "/api/v1/product/infra/search?value=x,      parameter",
            "/api/v1/product/parent?id=504,             type"
    })
    @DisplayName("Отсутствующий обязательный query-параметр -> 400 с именем параметра")
    void missingRequestParameterReturnsBadRequest(String url, String parameterName) throws Exception {
        mockMvc.perform(get(url))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorMessage")
                        .value("Не передан обязательный параметр запроса '" + parameterName + "'"));
    }

    @ParameterizedTest(name = "{0} -> 400, отсутствует заголовок user-id")
    @CsvSource({
            "/api/v1/user/product",
            "/api/v1/user/product/admin"
    })
    @DisplayName("Отсутствующий обязательный заголовок -> 400 с именем заголовка")
    void missingRequestHeaderReturnsBadRequest(String url) throws Exception {
        mockMvc.perform(get(url))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorMessage")
                        .value("Не передан обязательный заголовок запроса 'user-id'"));
    }

    @ParameterizedTest(name = "{0} -> 400, неверный тип параметра {1}")
    @CsvSource({
            "/api/v1/product/parent?id=abc&type=arch_container, id",
            "/api/v1/product/by-ids?ids=abc,                    ids"
    })
    @DisplayName("Нечисловое значение числового параметра -> 400, а не 500")
    void typeMismatchReturnsBadRequest(String url, String parameterName) throws Exception {
        mockMvc.perform(get(url))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorMessage")
                        .value("Неверное значение параметра запроса '" + parameterName + "'"));
    }
}
