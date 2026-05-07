package ru.beeline.fdmproducts.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import ru.beeline.fdmproducts.dto.ArchOperationDTO;
import ru.beeline.fdmproducts.dto.OperationSearchDTO;
import ru.beeline.fdmproducts.dto.ProductInfoDTOTree;
import ru.beeline.fdmproducts.service.SearchService;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "search", description = "Поиск архитектурных операций (методов API), контейнеров и связей с технологическими возможностями (ТС).")
public class SearchController {

    @Autowired
    private SearchService searchService;

    @GetMapping("/operation")
    @Operation(summary = "Поиск операций по пути и типу",
            operationId = "search_operationsByPathAndType",
            description = "Ищет методы/интерфейсы/контейнеры/системы по строке пути и фильтру типа сущности.")
    public OperationSearchDTO searchOperations(
            @Parameter(description = "Фрагмент URL или пути операции") @RequestParam(required = false) String path,
            @Parameter(description = "Тип объекта поиска (контракт сервиса)") @RequestParam(required = false) String type) {
        return ResponseEntity.status(HttpStatus.OK).body(searchService.searchOperations(path, type)).getBody();
    }

    @GetMapping("/operation/tech-capability/{id}")
    @Operation(summary = "Операции по идентификатору технологической возможности",
            operationId = "search_operationsByTechCapabilityId",
            description = "Все архитектурные методы (operations), в которых реализована указанная ТС.")
    public List<ArchOperationDTO> getOperationsByTechCapability(
            @Parameter(description = "Идентификатор технологической возможности (ТС)") @PathVariable Integer id) {
        return ResponseEntity.status(HttpStatus.OK).body(searchService.getOperationByTc(id)).getBody();
    }

    @GetMapping("/operation/tech-capability/{id}/tree")
    @Operation(summary = "Дерево операций и родительских элементов по ТС",
            operationId = "search_operationsTreeByTechCapabilityId",
            description = "Те же операции, что и для ТС, но в виде дерева продуктов/контейнеров; id в пути — положительное целое (иначе 400).")
    public ResponseEntity<List<ProductInfoDTOTree>> searchOperationsTree(
            @Parameter(description = "Идентификатор ТС (строка с положительным целым)") @PathVariable String id) {
        try {
            Integer idInt = Integer.valueOf(id);
            if (idInt <= 0) {
                throw new IllegalArgumentException("ID должен быть положительным");
            }
            return ResponseEntity.ok(searchService.getOperationByTcTree(idInt));
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ID должен быть целым числом");
        }
    }
}
