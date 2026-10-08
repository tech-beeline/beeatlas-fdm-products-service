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
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.beeline.fdmproducts.annotation.ApiErrorCodes;
import ru.beeline.fdmproducts.dto.usecase.UseCaseGroupedByUseCaseDTO;
import ru.beeline.fdmproducts.service.UseCaseGroupedByUseCaseService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/product/use-cases")
@RequiredArgsConstructor
@Tag(name = "product-use-case",
        description = "Use case продукта: группировка обнаруженных методов по карточкам use case.")
public class ProductUseCaseController {

    private final UseCaseGroupedByUseCaseService useCaseGroupedByUseCaseService;

    @ApiErrorCodes({400, 404, 502, 500})
    @GetMapping("/grouped-by-use-case")
    @Operation(summary = "Use case с discovered-методами продукта",
            description = "По id или alias продукта возвращает use case, в шагах которых участвуют "
                    + "discovered-методы интерфейсов с источником UseCase. Список use case запрашивается "
                    + "в сервисе project. Участвующие методы обогащаются archOperation и techCapability.")
    @ApiResponse(responseCode = "200", description = "Массив карточек use case с operations; может быть пустым",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    array = @ArraySchema(schema = @Schema(implementation = UseCaseGroupedByUseCaseDTO.class))))
    public ResponseEntity<List<UseCaseGroupedByUseCaseDTO>> getGroupedByUseCase(
            @Parameter(description = "Id продукта (ровно один из id/alias)")
            @RequestParam(required = false) String id,
            @Parameter(description = "Alias продукта без учёта регистра (ровно один из id/alias)")
            @RequestParam(required = false) String alias) {
        return ResponseEntity.ok(useCaseGroupedByUseCaseService.findGroupedByUseCase(id, alias));
    }
}
