/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.beeline.fdmproducts.annotation.ApiErrorCodes;
import ru.beeline.fdmproducts.dto.e2e.E2eUpsertResponseDTO;
import ru.beeline.fdmproducts.dto.e2e.E2eV2GetResponseDTO;
import ru.beeline.fdmproducts.dto.e2e.E2eV2PatchRequestDTO;
import ru.beeline.fdmproducts.dto.e2e.E2eV2UpsertRequestDTO;
import ru.beeline.fdmproducts.service.E2eV2Service;

@RestController
@RequestMapping("/api/v2/e2e")
@Tag(name = "e2e-v2", description = "Загрузка e2e-процессов из Sparx напрямую в продуктовый каталог "
        + "(discovered_interface / discovered_operation), без слоя контейнеров.")
public class E2eV2Controller {

    @Autowired
    private E2eV2Service e2eV2Service;

    @ApiErrorCodes({400, 404, 500})
    @PostMapping()
    @Operation(summary = "Создать или обновить e2e-процесс из внешней системы",
            description = "Принимает описание e2e, products → interfaces → operations "
                    + "(интерфейс привязан напрямую к продукту через parentProductCmdb) "
                    + "и последовательность вызовов operationsRelations. "
                    + "Интерфейсы и операции сохраняются в discovered_interface / discovered_operation, "
                    + "связи operationsRelations — на id discovered_operation; "
                    + "источник интерфейсов задаётся query-параметром source, по умолчанию SPARX. "
                    + "e2e.productAlias — необязательный продукт-владелец (product.alias, без учёта регистра, "
                    + "после trim): не передан или null — при создании владельца нет, при обновлении не меняется; "
                    + "пустая строка снимает владельца; строка из пробелов — 400; неизвестный alias — 404, "
                    + "продукт не создаётся.")
    public ResponseEntity<E2eUpsertResponseDTO> upsertE2e(
            @Parameter(description = "Источник discovered_interface на весь запрос (SPARX, MAPIC, Structurizr, ...). "
                    + "Необязательный: если не передан — SPARX. Пустое значение — 400.")
            @RequestParam(value = "source", required = false) String source,
            @RequestBody E2eV2UpsertRequestDTO request) {
        return ResponseEntity.ok(e2eV2Service.upsert(request, source));
    }

    @ApiErrorCodes({400, 404, 500})
    @PatchMapping("/{code}")
    @Operation(summary = "Частично обновить e2e-процесс (SFDM-4092)",
            description = "В отличие от POST, обновляет только присланные поля — остальные не трогает. "
                    + "e2e с указанным code должен уже существовать (404, если нет). "
                    + "operationsRelations заменяется целиком, только если поле явно передано в теле; "
                    + "если поле отсутствует — существующие operation_relations не меняются. "
                    + "productAlias — продукт-владелец: не передан или null — не меняется, пустая строка снимает, "
                    + "строка из пробелов — 400, неизвестный alias — 404.")
    public ResponseEntity<E2eUpsertResponseDTO> patchE2e(
            @Parameter(description = "Код e2e (product.e2e.code)") @PathVariable String code,
            @RequestBody(required = false) E2eV2PatchRequestDTO request) {
        return ResponseEntity.ok(e2eV2Service.patch(code, request));
    }

    @ApiErrorCodes({404, 405, 500})
    @GetMapping("/{code}")
    @Operation(summary = "Получить e2e-процесс по коду",
            description = "Возвращает карточку e2e с владельцем (productId, productAlias), дерево связей "
                    + "по id discovered_operation и справочник discoveredOperations; справочник operations пуст. "
                    + "У обнаруженных методов — сопоставленная операция архитектуры с SLA, интерфейсом и контейнером.")
    public ResponseEntity<E2eV2GetResponseDTO> getE2eByCode(
            @Parameter(description = "Код e2e (product.e2e.code)") @PathVariable String code) {
        return ResponseEntity.ok(e2eV2Service.getByCode(code));
    }
}
