package ru.beeline.fdmproducts.controller;


import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.beeline.fdmproducts.dto.PublishedApiDTO;
import ru.beeline.fdmproducts.dto.SpecDTO;
import ru.beeline.fdmproducts.service.MapicService;

import java.util.List;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RestController
@RequestMapping("/api/v1/mapic")
@Api(value = "Mapic API", tags = "mapic")
public class MapicController {

    @Autowired
    MapicService mapicService;

    @GetMapping("/product/{cmdb}/published-api")
    @ApiOperation("Эмуляция запросов к Mapic")
    public ResponseEntity<List<PublishedApiDTO>> requestToMapic(@PathVariable String cmdb) {
        return ResponseEntity.status(HttpStatus.OK).body(mapicService.requestToMapic(cmdb));
    }

    @GetMapping("/spec/{api-id}")
    @ApiOperation("Найти запись в таблице mapic.api  по id")
    public ResponseEntity<String> getMapicApi(@PathVariable("api-id") Integer apiId) {
        return ResponseEntity.status(HttpStatus.OK).body(mapicService.getMapicApi(apiId));
    }
}
