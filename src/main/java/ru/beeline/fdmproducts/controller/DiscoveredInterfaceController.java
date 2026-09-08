/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.beeline.fdmproducts.annotation.ApiErrorCodes;
import ru.beeline.fdmproducts.dto.DiscoveredInterfaceDTO;
import ru.beeline.fdmproducts.dto.DiscoveredInterfaceOperationDTO;
import ru.beeline.fdmproducts.dto.discovered.ProjectDiscoveredInterfaceDTO;
import ru.beeline.fdmproducts.dto.discovered.ProjectDiscoveredInterfaceResultDTO;
import ru.beeline.fdmproducts.service.DiscoveredInterfaceService;
import ru.beeline.fdmproducts.service.ProjectDiscoveredInterfaceService;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "discovered-interface", description = "Интерфейсы и операции, обнаруженные интеграциями (не модель Structurizr): загрузка и чтение.")
public class DiscoveredInterfaceController {

    @Autowired
    private DiscoveredInterfaceService discoveredInterfaceService;

    @Autowired
    private ProjectDiscoveredInterfaceService projectDiscoveredInterfaceService;

    @ApiErrorCodes({400, 404, 500})
    @PutMapping("/discovered-interface/project/{projectId}")
    @Operation(summary = "Создать или обновить обнаруженные интерфейсы и операции проекта",
            description = "Принимает массив интерфейсов, сгруппированных по паре (interfaceCode, product), "
                    + "и возвращает по каждой операции её discovered_operation.id — вызывающий сервис "
                    + "связывает их со своими сущностями по ключу (interfaceCode, product, name, type). "
                    + "Идентичность интерфейса — (source, продукт, interfaceCode), операции — "
                    + "(интерфейс, name, type): повторный вызов обновляет атрибуты, ничего не удаляя.")
    public ResponseEntity<List<ProjectDiscoveredInterfaceResultDTO>> putProjectDiscoveredInterfaces(
            @Parameter(description = "Идентификатор проекта") @PathVariable("projectId") Integer projectId,
            @Parameter(description = "Происхождение данных, по умолчанию ProjectTask")
            @RequestParam(name = "source", required = false) String source,
            @RequestBody(required = false) List<ProjectDiscoveredInterfaceDTO> interfaces) {
        return ResponseEntity.ok(
                projectDiscoveredInterfaceService.upsertProjectInterfaces(projectId, source, interfaces));
    }

    @ApiErrorCodes({400, 500})
    @PutMapping("/discovered-interfaces")
    @Operation(summary = "Создать или обновить набор обнаруженных интерфейсов",
            description = "Массовая upsert по списку DiscoveredInterfaceDTO для одного или нескольких продуктов в теле.")
    public ResponseEntity<Void> putProductDiscoveredInterfaces(@RequestBody List<DiscoveredInterfaceDTO> DInterfacesDTOS) {
        discoveredInterfaceService.createOrUpdateDiscoveredInterfaces(DInterfacesDTOS);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @ApiErrorCodes({400, 500})
    @PutMapping("/discovered-interface/{id}/operations")
    @Operation(summary = "Создать или обновить операции интерфейса",
            description = "Path id — идентификатор сохранённого обнаруженного интерфейса.")
    public ResponseEntity<Void> updateInterfaceOperations(@Parameter(description = "Id интерфейса в БД") @PathVariable("id") Integer interfaceId,
                                                            @RequestBody List<DiscoveredInterfaceOperationDTO> operations) {
        discoveredInterfaceService.createOrUpdateOperations(interfaceId, operations);
        return ResponseEntity.ok().build();
    }

    @ApiErrorCodes({400, 500})
    @GetMapping("/discovered-interface")
    @Operation(summary = "Получить обнаруженный интерфейс с операциями",
            description = "Ровно один из параметров id, external-id или api-id должен быть задан для поиска.")
    public ResponseEntity<DiscoveredInterfaceDTO> getInterfaceOperations(
            @Parameter(description = "Внутренний id интерфейса") @RequestParam(name = "id", required = false) Integer interfaceId,
            @Parameter(description = "Внешний идентификатор из источника") @RequestParam(name = "external-id", required = false) Integer externalId,
            @Parameter(description = "Идентификатор API в Mapic") @RequestParam(name = "api-id", required = false) Integer apiId) {
        return ResponseEntity.status(HttpStatus.OK).body(discoveredInterfaceService.getOperationsByInterfaceId(interfaceId, externalId, apiId));
    }

    @ApiErrorCodes({500})
    @GetMapping("/discovered-interface/sources")
    @Operation(summary = "Получить все источники обнаруженных интерфейсов",
            description = "Уникальные значения source из discovered_interface (без учёта регистра), в алфавитном порядке.")
    public ResponseEntity<List<String>> getDiscoveredInterfaceSources() {
        return ResponseEntity.status(HttpStatus.OK).body(discoveredInterfaceService.getDistinctSources());
    }
}
