/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.beeline.fdmproducts.annotation.ApiErrorCodes;
import ru.beeline.fdmproducts.dto.e2e.E2eCardResponseDTO;
import ru.beeline.fdmproducts.dto.e2e.E2eGetResponseDTO;
import ru.beeline.fdmproducts.dto.e2e.E2eUpsertRequestDTO;
import ru.beeline.fdmproducts.dto.e2e.E2eUpsertResponseDTO;
import ru.beeline.fdmproducts.service.E2eService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/e2e")
@Tag(name = "e2e", description = "Загрузка и чтение e2e-процессов с деревом вызовов операций.")
public class E2eController {

    @Autowired
    private E2eService e2eService;

    @ApiErrorCodes({400, 404, 500})
    @PostMapping()
    @Operation(summary = "Создать или обновить e2e-процесс",
            description = "Принимает описание e2e, дерево products → containers → interfaces → operations "
                    + "и последовательность вызовов operationsRelations.")
    public ResponseEntity<E2eUpsertResponseDTO> upsertE2e(@RequestBody E2eUpsertRequestDTO request) {
        return ResponseEntity.ok(e2eService.upsert(request));
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

