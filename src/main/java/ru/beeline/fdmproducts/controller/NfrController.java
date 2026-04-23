/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.controller;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Tag(name = "nfr", description = "Product API")
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
    @Operation(summary = "Получить требование NFR по id с обогащением ФФ/главами/паттернами")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "404", description = "Требование не найдено",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "404 NOT FOUND",
                                    value = "{\"errorMessage\": \"Требование не найдено\"}")))})
    @ApiResponse(responseCode = "200", description = "OK",
            content = @Content(
                    mediaType = "application/json",
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
                    ),
                    schema = @Schema(implementation = NfrDetailsDTO.class)
            )
    )
    public ResponseEntity<NfrDetailsDTO> getNfrById(@PathVariable("id") Integer id) {
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
    @Operation(summary = "Связать паттерн с требованиями NFR")
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
    public ResponseEntity<Void> linkPatternWithNfr(@PathVariable("id") Integer patternId,
                                                   @RequestParam(name = "refresh-relation", required = false) Boolean refreshRelation,
                                                   @Schema(description = "Массив ID",
                                                           example = "[101, 102, 105]",
                                                           type = "array")
                                                   @RequestBody(required = false) List<Integer> nfrIds) {
        patternRequirementService.linkPatternWithNfr(patternId, nfrIds, Boolean.TRUE.equals(refreshRelation));
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
