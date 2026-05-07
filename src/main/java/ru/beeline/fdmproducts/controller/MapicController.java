/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.controller;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.beeline.fdmproducts.dto.PublishedApiDTO;
import ru.beeline.fdmproducts.service.MapicService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/mapic")
@Tag(description = "Mapic API", name = "mapic")
public class MapicController {

    @Autowired
    MapicService mapicService;

    @GetMapping("/product/{cmdb}/published-api")
    @Operation(summary = "Эмуляция запросов к Mapic")
    public ResponseEntity<List<PublishedApiDTO>> requestToMapic(@PathVariable String cmdb) {
        return ResponseEntity.status(HttpStatus.OK).body(mapicService.requestToMapic(cmdb));
    }

    @GetMapping("/spec/{api-id}")
    @Operation(summary = "Найти запись в таблице mapic.api  по id")
    public ResponseEntity<String> getMapicApi(@PathVariable("api-id") Integer apiId) {
        return ResponseEntity.status(HttpStatus.OK).body(mapicService.getMapicApi(apiId));
    }
}
