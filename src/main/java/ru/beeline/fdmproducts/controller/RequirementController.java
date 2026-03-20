/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.beeline.fdmproducts.dto.NfrItemDTO;
import ru.beeline.fdmproducts.service.PatternRequirementService;

import java.util.List;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RestController
@RequestMapping("/api/v1")
@Api(value = "Requirement API", tags = "requirement")
public class RequirementController {

    @Autowired
    private PatternRequirementService patternRequirementService;

    @GetMapping("/requirement/pattern/{id}")
    @ApiOperation(value = "Получить все требования NFR, связанные с паттерном")
    public ResponseEntity<List<NfrItemDTO>> getNfrByPatternId(@PathVariable Integer id) {
        return ResponseEntity.ok(patternRequirementService.getNfrByPatternId(id));
    }
}
