/*
 * Copyright (c) 2026 PJSC VimpelCom
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
import org.springframework.web.bind.annotation.RestController;
import ru.beeline.fdmproducts.annotation.ApiErrorCodes;
import ru.beeline.fdmproducts.dto.PatternUsageDTO;
import ru.beeline.fdmproducts.service.PatternUsageService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/pattern")
@Tag(name = "pattern-usage", description = "Где используется паттерн Techradar: жизненные ситуации и актуальные версии НФТ.")
public class PatternUsageController {

    private final PatternUsageService patternUsageService;

    @ApiErrorCodes({400, 500})
    @GetMapping("/{id}/usage")
    @Operation(summary = "Использование паттерна в ЖС и НФТ",
            description = """
                    ЖС, к которым привязан паттерн (по возрастанию code), и актуальные версии НФТ со связью с ним. \
                    Связь только у старой версии НФТ не учитывается. Существование паттерна не проверяется: \
                    связей нет — пустые массивы. Вызывается techradar перед удалением паттерна.""")
    public ResponseEntity<PatternUsageDTO> getPatternUsage(
            @Parameter(description = "Идентификатор паттерна, целое положительное") @PathVariable String id) {
        return ResponseEntity.ok(patternUsageService.getUsage(id));
    }
}
