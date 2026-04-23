/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.beeline.fdmproducts.dto.ConnectionRequestDTO;
import ru.beeline.fdmproducts.service.InterfaceService;

@RestController
@RequestMapping("/api/v1/connection/interface")
@Tag(description = "Interface API", name = "interface")
public class InterfaceController {
    @Autowired
    private InterfaceService interfaceService;


    @PostMapping
    @Operation(summary = "Метод ручного сопостовления интерфейсов мапика и архитектуры")
    public ResponseEntity<Void> syncInfrastructure(@RequestBody ConnectionRequestDTO request) {

        interfaceService.handConnection(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
