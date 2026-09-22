/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.dto.search;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.beeline.fdmproducts.dto.ContainerSearchDTO;
import ru.beeline.fdmproducts.dto.InterfaceSearchDTO;
import ru.beeline.fdmproducts.dto.ProductSearchDTO;

/**
 * Элемент плоского ответа search-matched. Один и тот же массив несёт две формы: найденную
 * архитектурную операцию и признак того, что продукт кандидата не найден (это не ошибка запроса,
 * поэтому статус остаётся 200). Незаполненные поля из ответа выкидываются, чтобы каждая форма
 * выглядела так, как описано в контракте, — без null-полей чужой формы.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Найденная архитектурная операция либо признак ненайденного продукта кандидата")
public class MatchedArchOperationDTO {

    @Schema(description = "Идентификатор operation")
    private Integer id;

    @Schema(description = "Имя операции")
    private String name;

    @Schema(description = "Тип операции")
    private String type;

    @Schema(description = "Код продукта, по которому выполнялся поиск — как в запросе")
    private String productCode;

    @Schema(description = "Имя метода из запроса, для которого найдено совпадение")
    private String requestedMethodName;

    @JsonProperty("interface")
    @Schema(description = "Интерфейс, которому принадлежит операция")
    private InterfaceSearchDTO interfaceObj;

    @Schema(description = "Контейнер интерфейса")
    private ContainerSearchDTO container;

    @Schema(description = "Продукт контейнера")
    private ProductSearchDTO product;

    @Schema(description = "Текст ошибки — только для кандидата с ненайденным продуктом")
    private String error;

    @Schema(description = "Признак ненайденного продукта — только для такого кандидата")
    private Boolean notFound;
}
