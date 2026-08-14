/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.beeline.fdmproducts.annotation.ApiErrorCodes;
import ru.beeline.fdmproducts.dto.e2e.E2eUpsertResponseDTO;
import ru.beeline.fdmproducts.dto.e2e.E2eV2UpsertRequestDTO;
import ru.beeline.fdmproducts.service.E2eV2Service;

@RestController
@RequestMapping("/api/v2/e2e")
@Tag(name = "e2e-v2", description = "Загрузка e2e-процессов из Sparx напрямую в продуктовый каталог "
        + "(discovered_interface / discovered_operation), без слоя контейнеров.")
public class E2eV2Controller {

    @Autowired
    private E2eV2Service e2eV2Service;

    @ApiErrorCodes({400, 500})
    @PostMapping()
    @Operation(summary = "Создать или обновить e2e-процесс из Sparx",
            description = "Принимает описание e2e, products → interfaces → operations "
                    + "(интерфейс привязан напрямую к продукту через parentProductCmdb) "
                    + "и последовательность вызовов operationsRelations. "
                    + "Интерфейсы и операции сохраняются в discovered_interface / discovered_operation с source = SPARX.")
    public ResponseEntity<E2eUpsertResponseDTO> upsertE2e(@RequestBody E2eV2UpsertRequestDTO request) {
        return ResponseEntity.ok(e2eV2Service.upsert(request));
    }
}
