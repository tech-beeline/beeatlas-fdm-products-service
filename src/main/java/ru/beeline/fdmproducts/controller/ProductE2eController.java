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
import ru.beeline.fdmproducts.dto.e2e.E2eGroupedByE2eDTO;
import ru.beeline.fdmproducts.service.E2eGroupedByE2eService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/product/e2e")
@RequiredArgsConstructor
@Tag(name = "product-e2e",
        description = "E2e продукта: группировка обнаруженных методов по карточкам e2e.")
public class ProductE2eController {

    private final E2eGroupedByE2eService e2eGroupedByE2eService;

    @ApiErrorCodes({400, 404, 502, 500})
    @GetMapping("/grouped-by-e2e")
    @Operation(summary = "E2e с discovered-методами продукта",
            description = "По id или alias продукта возвращает карточки e2e, в шагах которых участвуют "
                    + "discovered-методы интерфейсов этого продукта. Источники берутся из карточек e2e "
                    + "(уникальные source без учёта регистра). Участвующие методы обогащаются archOperation "
                    + "и techCapability. Сервис project не вызывается.")
    @ApiResponse(responseCode = "200", description = "Массив карточек e2e с operations; может быть пустым",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    array = @ArraySchema(schema = @Schema(implementation = E2eGroupedByE2eDTO.class))))
    public ResponseEntity<List<E2eGroupedByE2eDTO>> getGroupedByE2e(
            @Parameter(description = "Id продукта (ровно один из id/alias)")
            @RequestParam(required = false) String id,
            @Parameter(description = "Alias продукта без учёта регистра (ровно один из id/alias)")
            @RequestParam(required = false) String alias) {
        return ResponseEntity.ok(e2eGroupedByE2eService.findGroupedByE2e(id, alias));
    }
}
