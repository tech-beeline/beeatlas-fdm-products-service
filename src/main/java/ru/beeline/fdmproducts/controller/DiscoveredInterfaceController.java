/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.beeline.fdmproducts.dto.DiscoveredInterfaceDTO;
import ru.beeline.fdmproducts.dto.DiscoveredInterfaceOperationDTO;
import ru.beeline.fdmproducts.service.DiscoveredInterfaceService;

import java.util.List;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RestController
@RequestMapping("/api/v1")
@Api(value = "Discovered Interface API", tags = "discoveredInterface")
public class DiscoveredInterfaceController {

    @Autowired
    private DiscoveredInterfaceService discoveredInterfaceService;

    @PutMapping("/discovered-interfaces")
    @ApiOperation(value = "Создание и обновление интерфейсов продукта")
    public ResponseEntity putProductDiscoveredInterfaces(@RequestBody List<DiscoveredInterfaceDTO> DInterfacesDTOS) {
        discoveredInterfaceService.createOrUpdateDiscoveredInterfaces(DInterfacesDTOS);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PutMapping("/discovered-interface/{id}/operations")
    @ApiOperation(value = "Создание и обновление операций интерфейса по id")
    public ResponseEntity<?> updateInterfaceOperations(@PathVariable("id") Integer interfaceId,
                                                       @RequestBody List<DiscoveredInterfaceOperationDTO> operations) {
        discoveredInterfaceService.createOrUpdateOperations(interfaceId, operations);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/discovered-interface")
    @ApiOperation(value = "Получение интерфейса")
    public ResponseEntity<DiscoveredInterfaceDTO> getInterfaceOperations(@RequestParam(name = "id", required = false) Integer interfaceId,
                                                                         @RequestParam(name = "external-id", required = false) Integer externalId,
                                                                         @RequestParam(name = "api-id", required = false) Integer apiId) {
        return ResponseEntity.ok().body(discoveredInterfaceService.getOperationsByInterfaceId(interfaceId, externalId, apiId));
    }
}
