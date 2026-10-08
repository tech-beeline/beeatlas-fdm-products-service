/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.controller;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.beeline.fdmproducts.annotation.ApiErrorCodes;
import ru.beeline.fdmproducts.dto.discovered.DiscoveredOperationByIdsDTO;
import ru.beeline.fdmproducts.service.DiscoveredOperationByIdsService;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "discovered-operation",
        description = "Обнаруженные методы (discovered_operation): чтение карточек по списку id.")
public class DiscoveredOperationController {

    private final DiscoveredOperationByIdsService discoveredOperationByIdsService;

    @ApiErrorCodes({400, 502, 500})
    @PostMapping("/discovered-operation/by-ids")
    @Operation(summary = "Получить discovered_operation по списку id",
            description = "Возвращает найденные методы со вложенным интерфейсом обнаружения и "
                    + "сопоставленной операцией архитектуры (SLA, интерфейс, контейнер, ТС).")
    @ApiResponse(responseCode = "200", description = "Найденные методы в порядке уникальных id из тела",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    array = @ArraySchema(schema = @Schema(implementation = DiscoveredOperationByIdsDTO.class))))
    public ResponseEntity<List<DiscoveredOperationByIdsDTO>> getByIds(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
                    description = "JSON-массив целых id discovered_operation",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = Integer.class,
                            example = "20"))))
            @RequestBody(required = false) JsonNode body) {
        return ResponseEntity.ok(discoveredOperationByIdsService.getByIds(body));
    }
}
