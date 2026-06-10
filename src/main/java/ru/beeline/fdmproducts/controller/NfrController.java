/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.controller;

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
import ru.beeline.fdmproducts.annotation.ApiErrorCodes;
import ru.beeline.fdmproducts.dto.nfr.NfrDetailsDTO;
import ru.beeline.fdmproducts.dto.nfr.NfrDetailsV2DTO;
import ru.beeline.fdmproducts.dto.nfr.NfrItemProductDTO;
import ru.beeline.fdmproducts.dto.nfr.NfrItemProductV2DTO;
import ru.beeline.fdmproducts.dto.nfr.NfrItemPublicDTO;
import ru.beeline.fdmproducts.service.NonFunctionalRequirementEnumService;
import ru.beeline.fdmproducts.service.NonFunctionalRequirementService;
import ru.beeline.fdmproducts.service.PatternRequirementService;

import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "nfr", description = "Product API")
public class NfrController {

    @Autowired
    private NonFunctionalRequirementService nonFunctionalRequirementService;

    @Autowired
    private NonFunctionalRequirementEnumService nonFunctionalRequirementEnumService;

    @Autowired
    PatternRequirementService patternRequirementService;

    @ApiErrorCodes({500})
    @GetMapping("/v1/nfr")
    @Operation(summary = "Получить все актуальные версии требований NFR (без дублей по core_id)")
    public ResponseEntity<List<NfrItemPublicDTO>> getAllNfr() {
        return ResponseEntity.ok(nonFunctionalRequirementEnumService.getAllActualNfr());
    }

    @ApiErrorCodes({400, 404, 500})
    @GetMapping("/v1/nfr/product")
    @Operation(summary = "Получить все актуальные версии требований NFR, связанные с продуктом")
    public ResponseEntity<List<NfrItemProductDTO>> getProductNfr(@RequestParam(value = "id", required = false) Integer id,
                                                                 @RequestParam(value = "alias", required = false) String alias,
                                                                 @RequestParam(value = "api-key", required = false) String apiKey) {
        Integer productId = nonFunctionalRequirementService.resolveProductId(id, alias, apiKey);
        List<NfrItemProductDTO> nfrList = nonFunctionalRequirementService.getProductNfr(productId);
        return ResponseEntity.ok(nfrList);
    }

    @ApiErrorCodes({400, 404, 500})
    @GetMapping("/v2/nfr/product")
    @Operation(summary = "Получить NFR продукта (v2): справочник FF из FF Manager")
    public ResponseEntity<List<NfrItemProductV2DTO>> getProductNfrV2(@RequestParam(value = "id", required = false) Integer id,
                                                                     @RequestParam(value = "alias", required = false) String alias,
                                                                     @RequestParam(value = "api-key", required = false) String apiKey) {
        Integer productId = nonFunctionalRequirementService.resolveProductId(id, alias, apiKey);
        return ResponseEntity.ok(nonFunctionalRequirementService.getProductNfrV2(productId));
    }

    @ApiErrorCodes({400, 404, 500})
    @DeleteMapping("/v1/nfr/product/relations")
    @Operation(summary = "Удалить связи NFR с продуктом (только source='Beeatlas') по списку id связей")
    public ResponseEntity<Void> deleteBeeatlasProductNfrRelations(@RequestParam(value = "id", required = false) Integer id,
                                                                  @RequestParam(value = "alias", required = false) String alias,
                                                                  @RequestParam(value = "api-key", required = false) String apiKey,
                                                                  @RequestBody(required = false) List<Integer> relationIds) {
        nonFunctionalRequirementService.deleteBeeatlasProductNfrRelations(id, alias, apiKey, relationIds);
        return ResponseEntity.ok().build();
    }

    @ApiErrorCodes({400, 404, 500})
    @DeleteMapping("/v1/nfr/{req-id}/product")
    @Operation(summary = "Удалить связь требования NFR с продуктом")
    public ResponseEntity<Void> deleteProductNfr(@PathVariable("req-id") Integer reqId,
                                                 @RequestParam(value = "id", required = false) Integer id,
                                                 @RequestParam(value = "alias", required = false) String alias,
                                                 @RequestParam(value = "api-key", required = false) String apiKey) {
        nonFunctionalRequirementService.deleteProductNfr(reqId, id, alias, apiKey);
        return ResponseEntity.ok().build();
    }

    @ApiErrorCodes({400, 404, 500})
    @GetMapping(path = "/v1/nfr/{id}", produces = "application/json")
    @Operation(summary = "Получить требование NFR по id с обогащением ФФ/главами/паттернами")
    public ResponseEntity<NfrDetailsDTO> getNfrById(@PathVariable("id") Integer id) {
        return ResponseEntity.ok(nonFunctionalRequirementService.getNfrDetails(id));
    }

    @ApiErrorCodes({400, 404, 500})
    @GetMapping(path = "/v2/nfr/{id}", produces = "application/json")
    @Operation(summary = "Получить требование NFR по id (v2): справочник FF из FF Manager")
    public ResponseEntity<NfrDetailsV2DTO> getNfrByIdV2(@PathVariable("id") Integer id) {
        return ResponseEntity.ok(nonFunctionalRequirementService.getNfrDetailsV2(id));
    }

    @ApiErrorCodes({400, 404, 500})
    @PostMapping("/v1/nfr/product")
    @Operation(summary = "Связать требования NFR с продуктом")
    public ResponseEntity<Void> addProductNfr(@RequestParam(value = "id", required = false) Integer id,
                                              @RequestParam(value = "alias", required = false) String alias,
                                              @RequestParam(value = "api-key", required = false) String apiKey,
                                              @RequestHeader(value = "USER-ID", required = false) String userIdHeader,
                                              @RequestBody(required = false) List<Integer> nfrIds) {
        nonFunctionalRequirementService.addProductNfr(id, alias, apiKey, userIdHeader, nfrIds);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("/v1/nfr/pattern/{id}")
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
