/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.beeline.fdmproducts.annotation.ApiErrorCodes;
import ru.beeline.fdmproducts.annotation.CustomHeaders;
import ru.beeline.fdmproducts.domain.Product;
import ru.beeline.fdmproducts.dto.*;
import ru.beeline.fdmproducts.dto.dashboard.ResultDTO;
import ru.beeline.fdmproducts.service.InfraService;
import ru.beeline.fdmproducts.service.ProductService;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

import static ru.beeline.fdmproducts.utils.Constant.USER_ID_HEADER;
import static ru.beeline.fdmproducts.utils.Constant.USER_ROLES_HEADER;

@ConditionalOnProperty(name = "app.force-app-delete", havingValue = "true")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RestController
@RequestMapping("/api/v1")
@Api(value = "Product API", tags = "product")
public class ProductDeleteController {

    @Autowired
    private ProductService productService;

    @ApiErrorCodes({400, 401, 403, 404, 500})
    @CustomHeaders
    @DeleteMapping("product/{id}")
    @ApiOperation(value = "Удаление продукта и его связей.")
    public ResponseEntity<Void> deleteProduct(@PathVariable Integer id,HttpServletRequest request) {
        productService.deleteProduct(id, request.getHeader(USER_ROLES_HEADER));
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
