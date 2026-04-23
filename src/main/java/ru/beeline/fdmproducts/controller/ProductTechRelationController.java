/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.beeline.fdmproducts.dto.ProductDTO;
import ru.beeline.fdmproducts.dto.ProductTechRelationDTO;
import ru.beeline.fdmproducts.service.ProductTechRelationService;

import java.util.List;


@RestController
@RequestMapping("/api/v1/product-tech-relation")
@Tag(description = "Product tech relation API", name = "product-tech-relation")
public class ProductTechRelationController {
    @Autowired
    private ProductTechRelationService productTechRelationService;

    @GetMapping
    @Operation(summary = "Получение связей продукта и технологии")
    public ResponseEntity<List<ProductDTO>> getProductsWithTech() {
        return ResponseEntity.status(HttpStatus.OK).body(productTechRelationService.getProductsWithTech());
    }

    @PostMapping("/{techId}")
    @Operation(summary = "Создание связи технологии и продукта")
    public ResponseEntity getProducts(@PathVariable Integer techId,
                                      @RequestBody ProductTechRelationDTO productTechRelationDTO) {
        productTechRelationService.addRelation(techId, productTechRelationDTO);
        return new ResponseEntity(HttpStatus.OK);
    }

    @DeleteMapping("/{techId}/{productId}")
    @Operation(summary = "Удаление связи технологии и продукта")
    public ResponseEntity deleteRelation(@PathVariable Integer techId,
                                         @PathVariable Integer productId) {
        productTechRelationService.deleteRelation(techId, productId);
        return new ResponseEntity(HttpStatus.OK);
    }
}
