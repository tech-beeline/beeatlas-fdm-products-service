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
import ru.beeline.fdmproducts.dto.ProductDTO;
import ru.beeline.fdmproducts.dto.ProductTechRelationDTO;
import ru.beeline.fdmproducts.service.ProductTechRelationService;

import java.util.List;


@RestController
@RequestMapping("/api/v1/product-tech-relation")
@Tag(name = "product-tech-relation", description = "Явные связи «продукт — технология»: список, создание и удаление.")
public class ProductTechRelationController {
    @Autowired
    private ProductTechRelationService productTechRelationService;

    @GetMapping
    @Operation(summary = "Все продукты с привязанными технологиями",
            description = "Плоский список продуктов, у которых есть связи с технологиями в сервисе.")
    public ResponseEntity<List<ProductDTO>> getProductsWithTech() {
        return ResponseEntity.status(HttpStatus.OK).body(productTechRelationService.getProductsWithTech());
    }

    @PostMapping("/{techId}")
    @Operation(summary = "Добавить связь технологии с продуктом")
    public ResponseEntity<Void> getProducts(@Parameter(description = "Id технологии") @PathVariable Integer techId,
                                            @RequestBody ProductTechRelationDTO productTechRelationDTO) {
        productTechRelationService.addRelation(techId, productTechRelationDTO);
        return new ResponseEntity(HttpStatus.OK);
    }

    @DeleteMapping("/{techId}/{productId}")
    @Operation(summary = "Удалить связь технологии и продукта")
    public ResponseEntity<Void> deleteRelation(@Parameter(description = "Id технологии") @PathVariable Integer techId,
                                             @Parameter(description = "Id продукта") @PathVariable Integer productId) {
        productTechRelationService.deleteRelation(techId, productId);
        return new ResponseEntity(HttpStatus.OK);
    }
}
