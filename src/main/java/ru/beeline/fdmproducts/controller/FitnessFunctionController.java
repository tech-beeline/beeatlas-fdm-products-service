/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.beeline.fdmproducts.dto.*;
import ru.beeline.fdmproducts.service.FitnessFunctionService;
import ru.beeline.fdmproducts.service.InfraService;
import ru.beeline.fdmproducts.service.ProductService;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RestController
@RequestMapping("/api/v1")
@Api(value = "Product API", tags = "product")
public class FitnessFunctionController {

    @Autowired
    private FitnessFunctionService fitnessFunctionService;

    @GetMapping("/dashboard/fitness-function")
    @ApiOperation(value = "Получение агрегации результатов фитнесс-функций")
    public ResponseEntity<MainResponseDTO> getFitnessFunctionsAggregation() {
        return ResponseEntity.ok(fitnessFunctionService.getFitnessFunctionsAggregation());
    }
}
