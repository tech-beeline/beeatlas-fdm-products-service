package ru.beeline.fdmproducts.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.beeline.fdmproducts.dto.ProductDTO;
import ru.beeline.fdmproducts.dto.ProductTechRelationDTO;
import ru.beeline.fdmproducts.service.ProductTechRelationService;

import java.util.List;


@CrossOrigin(origins = "*", allowedHeaders = "*")
@RestController
@RequestMapping("/api/v1/product-tech-relation")
@Api(value = "Product tech relation API", tags = "product-tech-relation")
public class ProductTechRelationController {
    @Autowired
    private ProductTechRelationService productTechRelationService;

    @GetMapping
    @ApiOperation(value = "Получение связей продукта и технологии", response = List.class)
    public ResponseEntity<List<ProductDTO>> getProductsWithTech() {
        return ResponseEntity.status(HttpStatus.OK).body(productTechRelationService.getProductsWithTech());
    }

    @PostMapping("/{techId}")
    @ApiOperation(value = "Создание связи технологии и продукта", response = ResponseEntity.class)
    public ResponseEntity getProducts(@PathVariable Integer techId,
                                      @RequestBody ProductTechRelationDTO productTechRelationDTO) {
        productTechRelationService.addRelation(techId, productTechRelationDTO);
        return new ResponseEntity(HttpStatus.OK);
    }

    @DeleteMapping("/{techId}/{productId}")
    @ApiOperation(value = "Удаление связи технологии и продукта", response = ResponseEntity.class)
    public ResponseEntity deleteRelation(@PathVariable Integer techId,
                                         @PathVariable Integer productId) {
        productTechRelationService.deleteRelation(techId, productId);
        return new ResponseEntity(HttpStatus.OK);
    }

}
