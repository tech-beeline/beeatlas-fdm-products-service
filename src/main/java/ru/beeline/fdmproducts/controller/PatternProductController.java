/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.beeline.fdmproducts.dto.ErrorMessageDTO;
import ru.beeline.fdmproducts.service.PatternProductService;

@RestController
@RequestMapping("/api/v1/pattern")
@Tag(name = "pattern-product",
        description = "Связь продуктов с паттернами Techradar: вычисление списка pattern id по NFR продукта.")
public class PatternProductController {

    @Autowired
    private PatternProductService patternProductService;

    @GetMapping("/product")
    @Operation(summary = "Идентификаторы паттернов по продукту",
            operationId = "patternProduct_listPatternIdsByProduct",
            description = """
                    Ровно один из параметров id, alias или api-key обязателен. По продукту находятся связанные \
                    требования NFR; возвращаются id паттернов, в которых покрыты все эти требования. \
                    Пустой массив — если у продукта нет подходящих NFR или паттернов.""")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Список идентификаторов паттернов",
                    content = @Content(mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = Integer.class)))),
            @ApiResponse(responseCode = "400", description = "Не передан или передано несколько идентификаторов продукта",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorMessageDTO.class))),
            @ApiResponse(responseCode = "404", description = "Продукт не найден",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorMessageDTO.class)))
    })
    public ResponseEntity<?> getPatternIdsByProduct(
            @Parameter(description = "Числовой id продукта") @RequestParam(required = false) Integer id,
            @Parameter(description = "Alias (код) продукта") @RequestParam(required = false) String alias,
            @Parameter(description = "API-ключ продукта для идентификации") @RequestParam(name = "api-key", required = false) String apiKey
    ) {
        return patternProductService.getPatternIdsByProduct(id, alias, apiKey);
    }
}

