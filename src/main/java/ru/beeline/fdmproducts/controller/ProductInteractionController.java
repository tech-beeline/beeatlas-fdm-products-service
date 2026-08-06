/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.beeline.fdmproducts.annotation.ApiErrorCodes;
import ru.beeline.fdmproducts.dto.interaction.ProductInteractionItemDTO;
import ru.beeline.fdmproducts.dto.interaction.ProductInteractionLinksDTO;
import ru.beeline.fdmproducts.dto.interaction.ProductInteractionResponseDTO;
import ru.beeline.fdmproducts.service.ProductInteractionService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/product")
@Tag(name = "product-interaction", description = "Взаимосвязи продуктов (provider → consumer).")
@RequiredArgsConstructor
public class ProductInteractionController {

    private final ProductInteractionService productInteractionService;

    @ApiErrorCodes({400, 405, 500})
    @GetMapping("/{alias}/interaction")
    @Operation(summary = "Получить взаимосвязи продукта по alias",
            description = "Возвращает providers (кто поставляет сервисы продукту) "
                    + "и consumers (кто потребляет сервисы продукта) из product.product_interaction.")
    public ResponseEntity<ProductInteractionLinksDTO> getInteractionsByAlias(
            @Parameter(description = "Alias продукта") @PathVariable String alias) {
        return ResponseEntity.ok(productInteractionService.getInteractionsByAlias(alias));
    }

    @ApiErrorCodes({400, 405, 500})
    @PostMapping("/interaction")
    @Operation(summary = "Сохранить взаимосвязи продуктов",
            description = "Идемпотентно наполняет product.product_interaction. "
                    + "Элемент с уже существующей парой providerAlias + consumerAlias игнорируется.")
    public ResponseEntity<ProductInteractionResponseDTO> saveInteractions(
            @RequestBody List<ProductInteractionItemDTO> items) {
        return ResponseEntity.ok(productInteractionService.saveInteractions(items));
    }
}
