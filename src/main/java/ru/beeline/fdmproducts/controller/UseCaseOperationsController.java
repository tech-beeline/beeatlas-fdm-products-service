/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.beeline.fdmproducts.dto.discovered.UseCaseDiscoveredInterfaceDTO;
import ru.beeline.fdmproducts.dto.discovered.UseCaseDiscoveredInterfaceResultDTO;
import ru.beeline.fdmproducts.service.UseCaseDiscoveredInterfaceService;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "use-case", description = "Публикация состава интерфейсов и методов use case.")
public class UseCaseOperationsController {

    private final UseCaseDiscoveredInterfaceService useCaseDiscoveredInterfaceService;

    @PutMapping("/use-case/operations")
    @Operation(summary = "Синхронизировать состав интерфейсов и методов use case",
            description = "Создаёт или обновляет интерфейсы и методы use case. "
                    + "У интерфейса указывают product или productPlanned. "
                    + "У метода можно передать connectionOperationId — связь с операцией архитектуры. "
                    + "В ответе — discoveredOperationId.")
    @ApiResponse(responseCode = "200", description = "Состав сохранён",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = UseCaseDiscoveredInterfaceResultDTO.class)))
    public ResponseEntity<List<UseCaseDiscoveredInterfaceResultDTO>> putUseCaseOperations(
            @Parameter(description = "Идентификатор use case в сервисе project (обязательное, целое положительное)")
            @RequestParam(name = "use-case-id", required = false) String useCaseId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
                    description = "Массив интерфейсов с методами")
            @RequestBody(required = false) List<UseCaseDiscoveredInterfaceDTO> interfaces) {
        return ResponseEntity.ok(useCaseDiscoveredInterfaceService.syncUseCaseOperations(useCaseId, interfaces));
    }
}
