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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.beeline.fdmproducts.dto.PublishedApiDTO;
import ru.beeline.fdmproducts.service.MapicService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/mapic")
@Tag(name = "mapic", description = "Чтение опубликованных API и спецификаций из интеграции с каталогом Mapic.")
public class MapicController {

    @Autowired
    MapicService mapicService;

    @GetMapping("/product/{cmdb}/published-api")
    @Operation(summary = "Опубликованные API продукта из Mapic",
            description = "Список PublishedApiDTO для CMDB-мнемоники; используется как совместимый ответ с форматом Mapic.")
    public ResponseEntity<List<PublishedApiDTO>> requestToMapic(@Parameter(description = "CMDB-мнемоника продукта") @PathVariable String cmdb) {
        return ResponseEntity.status(HttpStatus.OK).body(mapicService.requestToMapic(cmdb));
    }

    @GetMapping("/spec/{api-id}")
    @Operation(summary = "Спецификация API по id в таблице mapic.api",
            description = "Возвращает строковое представление спецификации (например OpenAPI в текстовом виде).")
    public ResponseEntity<String> getMapicApi(@Parameter(description = "Первичный ключ записи mapic.api") @PathVariable("api-id") Integer apiId) {
        return ResponseEntity.status(HttpStatus.OK).body(mapicService.getMapicApi(apiId));
    }
}
