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
import ru.beeline.fdmproducts.dto.InfraRequestDTO;
import ru.beeline.fdmproducts.service.InfraService;

@RestController
@RequestMapping("/api/v1/infra")
@Tag(name = "infra", description = "Синхронизация элементов инфраструктуры продукта из внешних источников в CMDB-слой сервиса.")
public class InfraController {
    @Autowired
    private InfraService infraService;


    @PostMapping
    @Operation(summary = "Запустить синхронизацию инфраструктуры продукта",
            description = "Query product — идентификатор/alias продукта; тело InfraRequestDTO — данные для загрузки.")
    public ResponseEntity<Void> syncInfrastructure(@Parameter(description = "Ключ продукта для синхронизации") @RequestParam String product,
                                                   @RequestBody InfraRequestDTO request) {
        infraService.syncInfrastructure(product, request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
