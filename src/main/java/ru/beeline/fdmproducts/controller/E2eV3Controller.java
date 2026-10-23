/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.beeline.fdmproducts.annotation.ApiErrorCodes;
import ru.beeline.fdmproducts.dto.e2e.E2eV3GetResponseDTO;
import ru.beeline.fdmproducts.service.E2eV3Service;

@RestController
@RequestMapping("/api/v3/e2e")
@RequiredArgsConstructor
@Tag(name = "e2e-v3", description = "Чтение e2e: дерево вызовов по product.discovered_operation.")
public class E2eV3Controller {

    private final E2eV3Service e2eV3Service;

    @ApiErrorCodes({400, 404, 405, 500})
    @GetMapping("/{code}")
    @Operation(summary = "Получить e2e с деревом вызовов по коду (SFDM-4239)",
            description = "Возвращает карточку e2e с владельцем (productId, productAlias), дерево operationsRelations "
                    + "и справочник discoveredOperations. Ключа operations в ответе нет. "
                    + "Корень дерева — связи с пустым operation_id, ребёнок — связь с operation_id, равным "
                    + "relatedOperationId родителя; сортировка по order, затем по id связи; повтор "
                    + "discovered_operation на пути от корня обрывает ветку. Узел — id discovered_operation. "
                    + "discoveredOperations — методы всех узлов без повторов в порядке первого появления; operation "
                    + "внутри карточки — product.operation по connection_operation_id со sla, interface и container "
                    + "или null; удалённые operation, interface и container в объект входят. "
                    + "Нет e2e — 404 {\"errorMessage\": \"E2e с указанным кодом не найден\"}; нет связей или ветки "
                    + "branch — 200 с пустыми массивами.")
    public ResponseEntity<E2eV3GetResponseDTO> getE2eByCode(
            @Parameter(description = "Код e2e (product.e2e.code), поиск без учёта регистра") @PathVariable String code,
            @Parameter(description = E2eV2Controller.BRANCH_READ_DESCRIPTION)
            @RequestParam(value = "branch", required = false) String branch) {
        return ResponseEntity.ok(e2eV3Service.getByCode(code, branch));
    }
}
