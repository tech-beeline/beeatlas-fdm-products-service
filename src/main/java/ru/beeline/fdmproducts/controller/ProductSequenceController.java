/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.beeline.fdmproducts.annotation.ApiErrorCodes;
import ru.beeline.fdmproducts.dto.sequence.SequenceUpsertRequestDTO;
import ru.beeline.fdmproducts.dto.sequence.SequenceUpsertResponseDTO;
import ru.beeline.fdmproducts.service.ProductSequenceService;

@RestController
@RequestMapping("/api/v1/product")
@Tag(name = "product-sequence",
        description = "Публикация product sequence (карточка + шаги вызовов методов) в ветку архитектуры продукта.")
public class ProductSequenceController {

    @Autowired
    private ProductSequenceService productSequenceService;

    @ApiErrorCodes({400, 404, 500})
    @PutMapping("/{alias}/sequence")
    @Operation(summary = "Создать или обновить product sequence",
            description = "Принимает карточку sequence, шаги sequenceCall и справочник методов operations. "
                    + "Идентичность sequence — пара (ветка, sequence.uid): повторный вызов обновляет карточку и "
                    + "полностью пересоздаёт состав шагов. Сущности C4 (контейнеры / интерфейсы / операции) метод "
                    + "не создаёт и не обновляет: атрибуты метода сохраняются снимком в sequence_step_operation, "
                    + "а ссылка на product.operation заполняется поиском по каталогу ветки — при неоднозначном "
                    + "или пустом результате остаётся не заполненной, это не ошибка.")
    public ResponseEntity<SequenceUpsertResponseDTO> upsertSequence(
            @Parameter(description = "Alias (cmdb) продукта, без учёта регистра") @PathVariable String alias,
            @Parameter(description = "Ветка архитектуры продукта, по умолчанию main")
            @RequestParam(required = false) String branch,
            @RequestBody(required = false) SequenceUpsertRequestDTO request) {
        return ResponseEntity.ok(productSequenceService.upsert(alias, branch, request));
    }
}
