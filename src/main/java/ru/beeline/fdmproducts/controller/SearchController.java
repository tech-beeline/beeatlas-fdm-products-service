package ru.beeline.fdmproducts.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.beeline.fdmlib.dto.product.OperationSearchDTO;
import ru.beeline.fdmproducts.service.SearchService;

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
}