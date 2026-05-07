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
import ru.beeline.fdmproducts.dto.GetProductDTO;
import ru.beeline.fdmproducts.service.TechService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tech")
@Tag(description = "Product API", name = "tech")
public class TechController {
    @Autowired
    private TechService techService;

    @GetMapping("/{techId}/product")
    @Operation(summary = "Получить все продукты использующие технологию")
    public ResponseEntity<List<GetProductDTO>> getProducts(@PathVariable Integer techId) {
        return ResponseEntity.status(HttpStatus.OK).body(techService.getProductsByTechId(techId));
    }
}
