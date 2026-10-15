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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.beeline.fdmproducts.annotation.ApiErrorCodes;
import ru.beeline.fdmproducts.dto.e2e.E2eByBiStepCodesGroupDTO;
import ru.beeline.fdmproducts.dto.e2e.E2eCardResponseDTO;
import ru.beeline.fdmproducts.dto.e2e.E2eGetResponseDTO;
import ru.beeline.fdmproducts.jackson.BiStepCodesRequest;
import ru.beeline.fdmproducts.service.E2eByBiStepCodesService;
import ru.beeline.fdmproducts.service.E2eService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/e2e")
@Tag(name = "e2e", description = "Чтение e2e-процессов. Загрузка — POST /api/v2/e2e.")
public class E2eController {

    @Autowired
    private E2eService e2eService;

    @Autowired
    private E2eByBiStepCodesService e2eByBiStepCodesService;

    @ApiErrorCodes({400, 500})
    @PostMapping("/by-bi-step-codes")
    @Operation(summary = "Получить e2e по кодам шагов BI",
            description = "Возвращает e2e-карточки, сгруппированные по кодам шагов BI.")
    @ApiResponse(responseCode = "200", description = "Группы в порядке уникальных кодов из тела",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    array = @ArraySchema(schema = @Schema(implementation = E2eByBiStepCodesGroupDTO.class))))
    public ResponseEntity<List<E2eByBiStepCodesGroupDTO>> getByBiStepCodes(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
                    description = "JSON-массив кодов шагов BI",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = String.class,
                            example = "Step.00.00.02.77"))))
            @RequestBody(required = false) BiStepCodesRequest biStepCodes) {
        return ResponseEntity.ok(e2eByBiStepCodesService.getByBiStepCodes(biStepCodes));
    }

    @ApiErrorCodes({400, 405, 500})
    @GetMapping()
    @Operation(summary = "Получить список e2e",
            description = "Возвращает массив карточек e2e. filter: all (по умолчанию), without-bi-step, with-bi-step.")
    public ResponseEntity<List<E2eCardResponseDTO>> listE2eByFilter(
            @Parameter(description = "Фильтр: all | without-bi-step | with-bi-step")
            @RequestParam(value = "filter", required = false) String filter) {
        return ResponseEntity.ok(e2eService.listE2eByFilter(filter));
    }

    @ApiErrorCodes({404, 405, 500})
    @GetMapping("/{code}")
    @Operation(summary = "Получить e2e-процесс по коду",
            description = "Возвращает карточку e2e, дерево operationsRelations и справочник operations.")
    public ResponseEntity<E2eGetResponseDTO> getE2eByCode(
            @Parameter(description = "Код e2e (product.e2e.code)") @PathVariable String code) {
        return ResponseEntity.ok(e2eService.getByCode(code));
    }
}

