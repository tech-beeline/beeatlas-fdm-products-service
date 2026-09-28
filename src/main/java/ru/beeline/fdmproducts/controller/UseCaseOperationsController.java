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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
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
            description = "Принимает массив интерфейсов с полным составом методов (один элемент — один "
                    + "интерфейс). Интерфейсы и методы создаются или обновляются в discovered_interface / "
                    + "discovered_operation с источником UseCase; product_planned и is_planned пишутся "
                    + "соответственно. Продукт ищется в каталоге по alias без учёта регистра: если продукта "
                    + "нет, интерфейс сохраняется с пустым product_id (не ошибка). Живые методы интерфейса, "
                    + "которых нет в теле, — кандидаты на удаление: их id отправляются в сервис project "
                    + "(POST /api/v1/project/use-cases/by-operations), и метод получает deleted_date только "
                    + "если он участвует ровно в одном use case, совпадающем с use-case-id. Ошибка вызова "
                    + "сервиса project откатывает транзакцию. В ответе — интерфейсы и методы из тела с "
                    + "discovered_operation.id для сшивки на стороне вызывающего сервиса.")
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
