/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.beeline.fdmproducts.dto.nfr.NfrDetailsDTO;
import ru.beeline.fdmproducts.dto.nfr.NfrItemProductDTO;
import ru.beeline.fdmproducts.dto.nfr.NfrItemPublicDTO;
import ru.beeline.fdmproducts.service.NonFunctionalRequirementEnumService;
import ru.beeline.fdmproducts.service.NonFunctionalRequirementService;
import ru.beeline.fdmproducts.service.PatternRequirementService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/nfr")
@Tag(name = "nfr", description = "Нефункциональные требования (NFR): справочник, связи с продуктами и паттернами, детали с фитнес-функциями и главами.")
public class NfrController {

    @Autowired
    private NonFunctionalRequirementService nonFunctionalRequirementService;

    @Autowired
    private NonFunctionalRequirementEnumService nonFunctionalRequirementEnumService;

    @Autowired
    PatternRequirementService patternRequirementService;

    @GetMapping
    @Operation(summary = "Получить все актуальные версии требований NFR (без дублей по core_id)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK",
                    content = @Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = NfrItemPublicDTO.class)),
                            examples = @ExampleObject(
                                    name = "Успешный ответ",
                                    value = """
                                            [
                                              {
                                                "id": 1,
                                                "code": "NFR-001",
                                                "version": 1,
                                                "name": "string",
                                                "description": "string",
                                                "rule": "string",
                                                "source": "string"
                                              }
                                            ]
                                            """
                            )
                    ))
    })
    public ResponseEntity<List<NfrItemPublicDTO>> getAllNfr() {
        return ResponseEntity.ok(nonFunctionalRequirementEnumService.getAllActualNfr());
    }

    @GetMapping("/product")
    @Operation(summary = "Получить все актуальные версии требований NFR, связанные с продуктом")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Список NFR продукта",
                    content = @Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = NfrItemProductDTO.class)),
                            examples = @ExampleObject(
                                    name = "Успешный ответ",
                                    value = """
                                            [
                                              {
                                                "id": 1,
                                                "code": "NFR-001",
                                                "version": 1,
                                                "name": "Пример NFR",
                                                "description": "Описание требования",
                                                "source": "Источник",
                                                "sourcePurpose": "Цель источника",
                                                "createdDate": "2026-04-07T07:37:29.203",
                                                "fitnessFunctions": [
                                                  {
                                                    "id": 101,
                                                    "code": "FF-001",
                                                    "description": "Функция пригодности",
                                                    "docLink": "https://example.com/ff-001"
                                                  }
                                                ],
                                                "chapters": [
                                                  {
                                                    "id": 201,
                                                    "name": "Глава 1",
                                                    "description": "Описание главы",
                                                    "docLink": "https://example.com/chapter-1",
                                                    "code": "CH-001"
                                                  }
                                                ],
                                                "patterns": [
                                                  {
                                                    "id": 301,
                                                    "code": "PAT-001",
                                                    "name": "Шаблон мониторинга",
                                                    "description": "Описание шаблона",
                                                    "rule": "Правило",
                                                    "dsl": "DSL выражение",
                                                    "isAntiPattern": false,
                                                    "createDate": "2026-04-07T07:37:29.203Z",
                                                    "updateDate": "2026-04-07T07:37:29.203Z",
                                                    "deleteDate": null
                                                  }
                                                ]
                                              }
                                            ]
                                            """
                            )
                    )),
            @ApiResponse(responseCode = "400", description = "Не передан или передано несколько идентификаторов",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "400 BAD REQUEST",
                                    value = "{\"error\": \"Не передан или передано несколько идентификаторов\"}"))),
            @ApiResponse(responseCode = "404", description = "Продукт не найден",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "404 NOT FOUND",
                                    value = "{\"error\": \"Продукт не найден\"}")))})
    public ResponseEntity<List<NfrItemProductDTO>> getProductNfr(@RequestParam(value = "id", required = false) Integer id,
                                                                 @RequestParam(value = "alias", required = false) String alias,
                                                                 @RequestParam(value = "api-key", required = false) String apiKey) {
        Integer productId = nonFunctionalRequirementService.resolveProductId(id, alias, apiKey);
        List<NfrItemProductDTO> nfrList = nonFunctionalRequirementService.getProductNfr(productId);
        return ResponseEntity.ok(nfrList);
    }

    @DeleteMapping("/product/relations")
    @Operation(summary = "Удалить связи NFR с продуктом (только source='Beeatlas') по списку id связей")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "400", description = "Ошибки валидации/несоответствия source или продукта",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(name = "Не передан идентификатор приложения",
                                            value = "{\"errorMessage\":\"Не передан один из идентификаторов приложения: id/alias/api-key\"}"),
                                    @ExampleObject(name = "Передано несколько идентификаторов приложения",
                                            value = "{\"errorMessage\":\"Передано несколько идентификаторов приложения\"}"),
                                    @ExampleObject(name = "Не переданы идентификаторы связей",
                                            value = "{\"errorMessage\":\"Не передан ни один идентификатор связи\"}"),
                                    @ExampleObject(name = "Связь не принадлежит продукту",
                                            value = "{\"errorMessage\":\"Связь 10 не принадлежит указанному продукту\"}"),
                                    @ExampleObject(name = "Связь с source != 'Beeatlas'",
                                            value = "{\"errorMessage\":\"Связь 10 имеет source отличный от 'Beeatlas'\"}"),
                                    @ExampleObject(name = "Часть связей не найдена",
                                            value = "{\"errorMessage\":\"Не найдены связи: [10, 11]\"}")
                            })),
            @ApiResponse(responseCode = "404", description = "Продукт не найден",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "404 NOT FOUND",
                                    value = "{\"errorMessage\":\"Продукт с указанным идентификатором не найден\"}"
                            )))
    })
    public ResponseEntity<Void> deleteBeeatlasProductNfrRelations(@RequestParam(value = "id", required = false) Integer id,
                                                                  @RequestParam(value = "alias", required = false) String alias,
                                                                  @RequestParam(value = "api-key", required = false) String apiKey,
                                                                  @RequestBody(required = false) List<Integer> relationIds) {
        nonFunctionalRequirementService.deleteBeeatlasProductNfrRelations(id, alias, apiKey, relationIds);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{req-id}/product")
    @Operation(summary = "Удалить связь требования NFR с продуктом")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "400", description = "Ошибки валидации входных данных",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(name = "Не передан идентификатор приложения",
                                            value = "{\"errorMessage\":\"Не передан один из идентификаторов приложения: id/alias/api-key\"}"),
                                    @ExampleObject(name = "Передано несколько идентификаторов приложения",
                                            value = "{\"errorMessage\":\"Передано несколько идентификаторов приложения\"}"),
                                    @ExampleObject(name = "Автоматически назначенное требование",
                                            value = "{\"errorMessage\":\"Требование назначенное автоматически, нельзя удалить вручную\"}")
                            })),
            @ApiResponse(responseCode = "404", description = "Продукт не найден",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "404 NOT FOUND",
                                    value = "{\"errorMessage\":\"Продукт с указанным идентификатором не найден\"}"
                            )))
    })
    public ResponseEntity<Void> deleteProductNfr(@PathVariable("req-id") Integer reqId,
                                                 @RequestParam(value = "id", required = false) Integer id,
                                                 @RequestParam(value = "alias", required = false) String alias,
                                                 @RequestParam(value = "api-key", required = false) String apiKey) {
        nonFunctionalRequirementService.deleteProductNfr(reqId, id, alias, apiKey);
        return ResponseEntity.ok().build();
    }

    @GetMapping(path = "/{id}", produces = "application/json")
    @Operation(summary = "Получить требование NFR по id с обогащением ФФ/главами/паттернами",
            description = "Возвращает карточку NFR по числовому id из справочника вместе со связанными фитнес-функциями, главами (жизненными ситуациями) и паттернами.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = NfrDetailsDTO.class),
                            examples = @ExampleObject(
                                    name = "Успешный ответ",
                                    value = """
                                            {
                                              "id": 1,
                                              "code": "NFR-001",
                                              "version": 1,
                                              "name": "string",
                                              "description": "string",
                                              "fitnessFunctions": [],
                                              "chapters": [],
                                              "patterns": []
                                            }
                                            """
                            ))),
            @ApiResponse(responseCode = "404", description = "Требование не найдено",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "404 NOT FOUND",
                                    value = "{\"errorMessage\": \"Требование не найдено\"}")))})
    public ResponseEntity<NfrDetailsDTO> getNfrById(
            @Parameter(description = "Идентификатор требования NFR в каталоге") @PathVariable("id") Integer id) {
        return ResponseEntity.ok(nonFunctionalRequirementService.getNfrDetails(id));
    }

    @PostMapping("/product")
    @Operation(summary = "Связать требования NFR с продуктом")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации идентификаторов/требований",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "400 BAD REQUEST",
                                    value = "{\"error\": \"Ошибка валидации идентификаторов/требований\"}"))),
            @ApiResponse(responseCode = "404", description = "Продукт/пользователь не найден",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "404 NOT FOUND",
                                    value = "{\"error\": \"Продукт/пользователь не найден\"}"))),
            @ApiResponse(responseCode = "500", description = "Auth недоступен",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "500 SERVER ERROR",
                                    value = "{\"error\": \"Auth недоступен\"}")))})
    public ResponseEntity<Void> addProductNfr(@RequestParam(value = "id", required = false) Integer id,
                                              @RequestParam(value = "alias", required = false) String alias,
                                              @RequestParam(value = "api-key", required = false) String apiKey,
                                              @RequestHeader(value = "USER-ID", required = false) String userIdHeader,
                                              @RequestBody(required = false) List<Integer> nfrIds) {
        nonFunctionalRequirementService.addProductNfr(id, alias, apiKey, userIdHeader, nfrIds);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("/pattern/{id}")
    @Operation(summary = "Связать паттерн с требованиями NFR",
            description = "Привязывает к паттерну (Techradar) список требований NFR по их id. Query-параметр refresh-relation переопределяет связи при значении true.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "400", description = "400 BAD REQUEST",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(name = "Не переданы идентификаторы требований",
                                            value = "{\"errorMessage\": \"Не переданы идентификаторы требований\"}"),
                                    @ExampleObject(name = "Паттерн не найден",
                                            value = "{\"errorMessage\": \"Идентификатор не соответсвует существующему паттерну\"}"),
                                    @ExampleObject(name = "Некорректные идентификаторы требований",
                                            value = "{\"errorMessage\": \"Не все переданные идентификаторы соответствуют существующим требованиям\"}")}))})
    public ResponseEntity<Void> linkPatternWithNfr(
            @Parameter(description = "Идентификатор паттерна в системе") @PathVariable("id") Integer patternId,
            @Parameter(description = "Если true — пересобрать связи паттерна с переданным списком NFR") @RequestParam(name = "refresh-relation", required = false) Boolean refreshRelation,
            @RequestBody(description = "Идентификаторы требований NFR для связи с паттерном. Пример: [101, 102, 105]",
                    required = false,
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = Integer.class))))
            @org.springframework.web.bind.annotation.RequestBody(required = false) List<Integer> nfrIds) {
        patternRequirementService.linkPatternWithNfr(patternId, nfrIds, Boolean.TRUE.equals(refreshRelation));
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
