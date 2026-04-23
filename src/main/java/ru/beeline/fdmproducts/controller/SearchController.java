package ru.beeline.fdmproducts.controller;

import io.swagger.v3.oas.annotations.Operation;
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
@Tag(description = "Search API", name = "search")
public class SearchController {

    @Autowired
    private SearchService searchService;

    @GetMapping("/operation")
    @Operation(summary = "Поиск методов, интерфейсов, контейнеров, систем в которых они реализованны")
    public OperationSearchDTO searchOperations(@RequestParam(required = false) String path,
                                               @RequestParam(required = false) String type) {
        return ResponseEntity.status(HttpStatus.OK).body(searchService.searchOperations(path, type)).getBody();
    }

    @GetMapping("/operation/tech-capability/{id}")
    @Operation(summary = "Список всех методов в которых реализованна ТС")
    public List<ArchOperationDTO> searchOperations(@PathVariable Integer id) {
        return ResponseEntity.status(HttpStatus.OK).body(searchService.getOperationByTc(id)).getBody();
    }

    @GetMapping("/operation/tech-capability/{id}/tree")
    @Operation(summary = "Список всех методы и их родительские элементы, реализовывающие ТС в виде дерева")
    public ResponseEntity<List<ProductInfoDTOTree>> searchOperationsTree(@PathVariable String id) {
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
