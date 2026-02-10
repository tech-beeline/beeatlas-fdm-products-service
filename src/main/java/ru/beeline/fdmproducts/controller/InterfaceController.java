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
import ru.beeline.fdmproducts.dto.ConnectionRequestDTO;
import ru.beeline.fdmproducts.service.InterfaceService;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RestController
@RequestMapping("/api/v1/connection/interface")
@Api(value = "Interface API", tags = "interface")
public class InterfaceController {
    @Autowired
    private InterfaceService interfaceService;


    @PostMapping
    @ApiOperation("Метод ручного сопостовления интерфейсов мапика и архитектуры")
    public ResponseEntity<Void> syncInfrastructure(@RequestBody ConnectionRequestDTO request) {

        interfaceService.handConnection(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
