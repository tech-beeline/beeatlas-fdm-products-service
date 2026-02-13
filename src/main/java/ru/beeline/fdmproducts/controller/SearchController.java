package ru.beeline.fdmproducts.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.beeline.fdmproducts.dto.ArchOperationDTO;
import ru.beeline.fdmproducts.dto.OperationSearchDTO;
import ru.beeline.fdmproducts.service.SearchService;

import java.util.List;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RestController
@RequestMapping("/api/v1")
@Api(value = "Search API", tags = "search")
public class SearchController {

    @Autowired
    private SearchService searchService;

    @GetMapping("/operation")
    @ApiOperation(value = "Поиск методов, интерфейсов, контейнеров, систем в которых они реализованны")
    public OperationSearchDTO searchOperations(@RequestParam(required = false) String path,
                                               @RequestParam(required = false) String type) {
        return ResponseEntity.status(HttpStatus.OK).body(searchService.searchOperations(path, type)).getBody();
    }

    @GetMapping("/operation/tech-capability/{id}")
    @ApiOperation(value = "Список всех методов в которых реализованна ТС")
    public List<ArchOperationDTO> searchOperations(@PathVariable Integer id) {
        return ResponseEntity.status(HttpStatus.OK).body(searchService.getOperationByTc(id)).getBody();
    }
}