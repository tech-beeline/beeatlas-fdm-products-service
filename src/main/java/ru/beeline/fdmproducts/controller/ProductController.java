package ru.beeline.fdmproducts.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.beeline.fdmlib.dto.product.ProductPutDto;
import ru.beeline.fdmproducts.domain.Product;
import ru.beeline.fdmproducts.service.ProductService;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

import static ru.beeline.fdmproducts.utils.Constant.USER_ID_HEADER;

import ru.beeline.fdmlib.dto.product.ProductPutDto;
import ru.beeline.fdmproducts.service.ProductService;
import ru.beeline.fdmproducts.domain.Product;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RestController
@RequestMapping("/api/v1")
@Api(value = "Product API", tags = "product")
public class ProductController {
    @Autowired
    private ProductService productService;

    @GetMapping("/user/product")
    @ApiOperation(value = "Получить все продукты пользователя", response = List.class)
    public ResponseEntity<List<Product>> getProducts(HttpServletRequest request) {
        Integer userId = Integer.valueOf(request.getHeader(USER_ID_HEADER));
        return ResponseEntity.status(HttpStatus.OK).body(productService.getProductsByUser(userId));
    }

    @PutMapping("/user/product")
    @ApiOperation(value = "Получить все продукты пользователя", response = List.class)
    public ResponseEntity putProducts(ProductPutDto productPutDto) {
        return ResponseEntity.status(HttpStatus.OK).body("");
    }

    @PatchMapping("product/{code}/workspace")
    @ApiOperation(value = "Добавление атрибутов к продукту")
    public ResponseEntity patchProducts(@PathVariable String code,
                                        @RequestBody ProductPutDto productPutDto) {
        productService.patchProduct(productPutDto, code);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
