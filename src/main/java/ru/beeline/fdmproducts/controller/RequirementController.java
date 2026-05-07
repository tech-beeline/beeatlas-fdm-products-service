/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.beeline.fdmproducts.dto.nfr.NfrItemDTO;
import ru.beeline.fdmproducts.dto.CreateRequirementRequestDTO;
import ru.beeline.fdmproducts.dto.CreateRequirementResponseDTO;
import ru.beeline.fdmproducts.dto.CreateRequirementVersionResponseDTO;
import ru.beeline.fdmproducts.dto.ErrorMessageDTO;
import ru.beeline.fdmproducts.service.PatternRequirementService;
import ru.beeline.fdmproducts.service.RequirementCreateService;
import ru.beeline.fdmproducts.service.RequirementVersionService;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "requirement",
        description = "Создание и версионирование требований NFR в каталоге; связь требований с паттернами.")
public class RequirementController {

    @Autowired
    private PatternRequirementService patternRequirementService;

    @Autowired
    private RequirementCreateService requirementCreateService;

    @Autowired
    private RequirementVersionService requirementVersionService;

    @GetMapping("/requirement/pattern/{id}")
    @Operation(summary = "Список NFR, привязанных к паттерну",
            description = "Все актуальные элементы каталога NFR, связанные с паттерном Techradar по его id.")
    public ResponseEntity<List<NfrItemDTO>> getNfrByPatternId(
            @Parameter(description = "Идентификатор паттерна") @PathVariable Integer id) {
        return ResponseEntity.ok(patternRequirementService.getNfrByPatternId(id));
    }

    @PostMapping("/requirement")
    @Operation(summary = "Создать требование NFR (первая версия)",
            description = "Тело CreateRequirementRequestDTO; права администратора и доступность Auth см. коды ответа.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Требование создано",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = CreateRequirementResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Ошибки валидации входных данных",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorMessageDTO.class),
                            examples = {
                                    @ExampleObject(name = "Обязательные параметры не переданы",
                                            value = "{\"errorMessage\":\"Не переданы обязательные параметры\"}"),
                                    @ExampleObject(name = "Некорректные chapters",
                                            value = "{\"errorMessage\":\"В массиве chapters переданы идентификаторы несуществующих жизненных ситуаций\"}"),
                                    @ExampleObject(name = "Некорректные patterns",
                                            value = "{\"errorMessage\":\"В массиве patterns переданы идентификаторы несуществующих паттернов\"}")
                            })),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorMessageDTO.class),
                            examples = {
                                    @ExampleObject(name = "Пользователь не администратор",
                                            value = "{\"errorMessage\":\"Пользователь не является администратором\"}")
                            })),
            @ApiResponse(responseCode = "404", description = "Пользователь не найден",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorMessageDTO.class),
                            examples = {
                                    @ExampleObject(name = "Инициатор не найден",
                                            value = "{\"errorMessage\":\"Пользователь, являющийся инициатором добавления требования к продукту, не найден\"}")
                            })),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка / недоступность Auth",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorMessageDTO.class),
                            examples = {
                                    @ExampleObject(name = "Auth недоступен",
                                            value = "{\"errorMessage\":\"Сервис Auth недоступен\"}")
                            }))
    })
    public ResponseEntity<CreateRequirementResponseDTO> createRequirement(
            @RequestBody(required = false) CreateRequirementRequestDTO request) {
        return ResponseEntity.ok(requirementCreateService.createRequirement(request));
    }

    @PostMapping("/requirement/version")
    @Operation(summary = "Создать новую версию существующего NFR",
            description = "Укажите ровно один из query-параметров id или code для базового требования, плюс тело с изменениями.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Версия требования создана",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = CreateRequirementVersionResponseDTO.class))),

            @ApiResponse(responseCode = "400", description = "Ошибки валидации входных данных",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorMessageDTO.class),
                            examples = {
                                    @ExampleObject(name = "Переданы несколько идентификаторов",
                                            value = "{\"errorMessage\":\"Переданы несколько идентификаторов\"}"),
                                    @ExampleObject(name = "Не передан id/code",
                                            value = "{\"errorMessage\":\"Не передан идентификатор требования (id или code)\"}"),
                                    @ExampleObject(name = "Обязательные параметры не переданы",
                                            value = "{\"errorMessage\":\"Не переданы обязательные параметры\"}"),
                                    @ExampleObject(name = "Некорректные chapters",
                                            value = "{\"errorMessage\":\"В массиве chapters переданы идентификаторы несуществующих жизненных ситуаций\"}"),
                                    @ExampleObject(name = "Некорректные patterns",
                                            value = "{\"errorMessage\":\"В массиве patterns переданы идентификаторы несуществующих паттернов\"}")
                            })),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorMessageDTO.class),
                            examples = {
                                    @ExampleObject(name = "Пользователь не администратор",
                                            value = "{\"errorMessage\":\"Пользователь не является администратором\"}")
                            })),
            @ApiResponse(responseCode = "404", description = "Требование не найдено",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorMessageDTO.class),
                            examples = {
                                    @ExampleObject(name = "Требование не найдено",
                                            value = "{\"errorMessage\":\"Требование не найдено\"}")
                            }))
    })
    public ResponseEntity<CreateRequirementVersionResponseDTO> createRequirementVersion(
            @Parameter(description = "Числовой id требования-основы") @RequestParam(value = "id", required = false) Integer id,
            @Parameter(description = "Код требования-основы (альтернатива id)") @RequestParam(value = "code", required = false) String code,
            @RequestBody(required = false) CreateRequirementRequestDTO request) {
        return ResponseEntity.ok(requirementVersionService.createVersion(id, code, request));
    }
}
