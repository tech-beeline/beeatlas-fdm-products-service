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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.beeline.fdmproducts.annotation.ApiErrorCodes;
import ru.beeline.fdmproducts.dto.domain.DomainProductsResponseDTO;
import ru.beeline.fdmproducts.service.ProductDomainService;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "domain", description = "Домены продуктов и вложенные приложения.")
public class ProductDomainController {

    @Autowired
    private ProductDomainService productDomainService;

    @ApiErrorCodes({405, 500})
    @GetMapping("/domain/products")
    @Operation(summary = "Список доменов и вложенных продуктов",
            description = "Все домены из product_domain с продуктами; продукты без домена — в элементе «Блок не указан».")
    public ResponseEntity<DomainProductsResponseDTO> getDomainsWithProducts() {
        return ResponseEntity.status(HttpStatus.OK).body(productDomainService.getDomainsWithProducts());
    }
}
