package ru.beeline.fdmproducts.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.beeline.fdmproducts.dto.ProductTechRelationDTO;
import ru.beeline.fdmproducts.service.ProductTechRelationService;


@CrossOrigin(origins = "*", allowedHeaders = "*")
@RestController
@RequestMapping("/api/v1/product-tech-relation")
@Api(value = "Product tech relation API", tags = "product-tech-relation\"")
public class ProductTechRelationController {
    @Autowired
    private ProductTechRelationService productTechRelationService;

    @PostMapping("/{techId}")
    @ApiOperation(value = "Создание связи технологии и продукта", response = ResponseEntity.class)
    public ResponseEntity getProducts(@PathVariable Integer techId,
                                      @RequestBody ProductTechRelationDTO productTechRelationDTO) {
        productTechRelationService.addRelation(techId, productTechRelationDTO);
        return new ResponseEntity(HttpStatus.OK);
    }
}
