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
import ru.beeline.fdmproducts.dto.CreateRequirementRequestDTO;
import ru.beeline.fdmproducts.dto.CreateRequirementResponseDTO;
import ru.beeline.fdmproducts.dto.CreateRequirementVersionResponseDTO;
import ru.beeline.fdmproducts.dto.nfr.NfrItemDTO;
import ru.beeline.fdmproducts.service.PatternRequirementService;
import ru.beeline.fdmproducts.service.RequirementCreateService;
import ru.beeline.fdmproducts.service.RequirementVersionService;

import java.util.List;

import static ru.beeline.fdmproducts.utils.Constant.USER_ID_HEADER;

@RestController
@RequestMapping("/api")
@Tag(name = "requirement",
        description = "Создание и версионирование требований NFR в каталоге; связь требований с паттернами.")
public class RequirementController {

    @Autowired
    private PatternRequirementService patternRequirementService;

    @Autowired
    private RequirementCreateService requirementCreateService;

    @Autowired
    private RequirementVersionService requirementVersionService;

    @ApiErrorCodes({400, 404, 500})
    @GetMapping("/v1/requirement/pattern/{id}")
    @Operation(summary = "Список NFR, привязанных к паттерну",
            description = "Все актуальные элементы каталога NFR, связанные с паттерном Techradar по его id.")
    public ResponseEntity<List<NfrItemDTO>> getNfrByPatternId(
            @Parameter(description = "Идентификатор паттерна") @PathVariable Integer id) {
        return ResponseEntity.ok(patternRequirementService.getNfrByPatternId(id));
    }

    @ApiErrorCodes({400, 403, 404, 500})
    @PostMapping("/v1/requirement")
    @Operation(summary = "Создать требование NFR (версия v1)",
            description = "Тело CreateRequirementRequestDTO; права администратора и доступность Auth см. коды ответа.")
    public ResponseEntity<CreateRequirementResponseDTO> createRequirement(
            @RequestBody(required = false) CreateRequirementRequestDTO request,
            @RequestHeader(value = USER_ID_HEADER) String userId) {
        return ResponseEntity.ok(requirementCreateService.createRequirement(request, userId));
    }

    @ApiErrorCodes({400, 403, 404, 500})
    @PostMapping("/v2/requirement")
    @Operation(summary = "Создать требование NFR (версия v2)",
            description = "Тело CreateRequirementRequestDTO; коды ФФ в rule проверяются по справочнику FF Manager.")
    public ResponseEntity<CreateRequirementResponseDTO> createRequirementV2(
            @RequestBody(required = false) CreateRequirementRequestDTO request,
            @RequestHeader(value = USER_ID_HEADER) String userId) {
        return ResponseEntity.ok(requirementCreateService.createRequirementV2(request, userId));
    }

    @ApiErrorCodes({400, 403, 404, 500})
    @PostMapping("/v1/requirement/version")
    @Operation(summary = "Создать новую версию существующего NFR",
            description = "Укажите ровно один из query-параметров id или code для базового требования, плюс тело с изменениями.")
    public ResponseEntity<CreateRequirementVersionResponseDTO> createRequirementVersion(
            @Parameter(description = "Числовой id требования-основы") @RequestParam(value = "id", required = false) Integer id,
            @Parameter(description = "Код требования-основы (альтернатива id)") @RequestParam(value = "code", required = false) String code,
            @RequestBody(required = false) CreateRequirementRequestDTO request) {
        return ResponseEntity.ok(requirementVersionService.createVersion(id, code, request));
    }
}
