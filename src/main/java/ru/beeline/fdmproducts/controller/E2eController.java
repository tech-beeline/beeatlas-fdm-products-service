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
    @Operation(summary = "Поиск e2e (SFDM-4240)",
            description = "Возвращает массив карточек e2e: id, code, name, description, biStepCode, source, productId, "
                    + "productAlias (без владельца — null). Дерева нет. Все параметры необязательны; переданные "
                    + "фильтры дают пересечение, без параметров — все e2e. Корневой метод e2e — вызываемый "
                    + "discovered_operation связи с пустым operation_id. method и operation-id вместе — 400. "
                    + "Пустые alias, tc-code, method или operation-id — 400. Совпадений нет — 200 и []. "
                    + "Порядок: code, затем id.")
    public ResponseEntity<List<E2eCardResponseDTO>> searchE2e(
            @Parameter(description = "Наличие bi_step_code: all | without-bi-step | with-bi-step. "
                    + "Не передан или пустой — all. Сравнение после trim, без учёта регистра")
            @RequestParam(value = "filter", required = false) String filter,
            @Parameter(description = "Мнемоника продукта-владельца e2e (product.alias), без учёта регистра")
            @RequestParam(value = "alias", required = false) String alias,
            @Parameter(description = "tc_code корневого discovered_operation, без учёта регистра")
            @RequestParam(value = "tc-code", required = false) String tcCode,
            @Parameter(description = "Сигнатура корневого метода: type, один пробел, name. Совпадает с "
                    + "discovered_operation или с сопоставленной product.operation, без учёта регистра", example = "GET getCart")
            @RequestParam(value = "method", required = false) String method,
            @Parameter(description = "id корневого discovered_operation или его connection_operation_id; "
                    + "целое положительное. Нельзя вместе с method")
            @RequestParam(value = "operation-id", required = false) String operationId) {
        return ResponseEntity.ok(e2eService.search(filter, alias, tcCode, method, operationId));
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

