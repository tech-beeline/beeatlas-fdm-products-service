/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.beeline.fdmproducts.service.PatternProductService;

@RestController
@RequestMapping("/api/v1/pattern")
public class PatternProductController {

    @Autowired
    private PatternProductService patternProductService;

    @GetMapping("/product")
    public ResponseEntity<?> getPatternIdsByProduct(
            @RequestParam(required = false) Integer id,
            @RequestParam(required = false) String alias,
            @RequestParam(name = "api-key", required = false) String apiKey
    ) {
        return patternProductService.getPatternIdsByProduct(id, alias, apiKey);
    }
}

