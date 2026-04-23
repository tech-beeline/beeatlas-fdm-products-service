/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.beeline.fdmproducts.service.ProductService;

import javax.servlet.http.HttpServletRequest;

import static ru.beeline.fdmproducts.utils.Constant.USER_ROLES_HEADER;

@ConditionalOnProperty(name = "app.force-app-delete", havingValue = "true")
@RestController
@RequestMapping("/api/v1")
@Tag(description = "Product API", name = "product")
public class ProductDeleteController {

    @Autowired
    private ProductService productService;

    @Operation(summary = "Удаление продукта и его связей.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Продукт успешно удалён"),
            @ApiResponse(responseCode = "400", description = "Неверные входные данные"),
            @ApiResponse(responseCode = "401", description = "Требуется аутентификация"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен"),
            @ApiResponse(responseCode = "404", description = "Ресурс не найден"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    @DeleteMapping("product/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Integer id, HttpServletRequest request) {
        productService.deleteProduct(id, request.getHeader(USER_ROLES_HEADER));
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
