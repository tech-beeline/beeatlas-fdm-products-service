package ru.beeline.fdmproducts.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.beeline.fdmlib.dto.product.ProductPutDto;
import ru.beeline.fdmproducts.domain.Product;
import ru.beeline.fdmproducts.dto.ApiSecretDTO;
import ru.beeline.fdmproducts.dto.AssessmentResponseDTO;
import ru.beeline.fdmproducts.dto.ContainerDTO;
import ru.beeline.fdmlib.dto.product.GetProductTechDto;
import ru.beeline.fdmproducts.dto.FitnessFunctionDTO;
import ru.beeline.fdmproducts.service.ProductService;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

import static ru.beeline.fdmproducts.utils.Constant.USER_ID_HEADER;
import static ru.beeline.fdmproducts.utils.Constant.USER_ROLES_HEADER;

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

    @GetMapping("/user/product/admin")
    @ApiOperation(value = "Получить все продукты пользователя", response = List.class)
    public ResponseEntity<List<Product>> getProductsAdmin(HttpServletRequest request) {
        Integer userId = Integer.valueOf(request.getHeader(USER_ID_HEADER));
        return ResponseEntity.status(HttpStatus.OK).body(productService.getProductsByUserAdmin(userId,
                request.getHeader(USER_ROLES_HEADER)));
    }

    @GetMapping("/product/{code}")
    @ApiOperation(value = "Получить продукт по alias", response = Product.class)
    public Product getProductsByCode(@PathVariable String code) {
        return productService.getProductByCode(code);
    }

    @PutMapping("/product/{code}")
    @ApiOperation(value = "Редактирование продукта")
    public ResponseEntity putProducts(@PathVariable String code,
                                      @RequestBody ProductPutDto productPutDto) {
        productService.createOrUpdate(productPutDto, code);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PatchMapping("product/{code}/workspace")
    @ApiOperation(value = "Добавление атрибутов к продукту")
    public ResponseEntity patchProducts(@PathVariable String code,
                                        @RequestBody ProductPutDto productPutDto) {
        productService.patchProduct(productPutDto, code);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("/user/{id}/products")
    @ApiOperation(value = "Создание связи пользователя и продукта")
    public ResponseEntity postUserProducts(@PathVariable String id,
                                           @RequestBody List<String> aliasLIst) {
        productService.postUserProduct(aliasLIst, id);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/product/api-secret/{api-key}")
    @ApiOperation(value = "Получение api secret из таблицы product")
    public ApiSecretDTO getProductSecretByApiKey(@PathVariable("api-key") String apiKey) {
        return productService.getProductByApiKey(apiKey);
    }

    @GetMapping("/service/api-secret/{api-key}")
    @ApiOperation(value = "Получение api secret из таблицы service")
    public ApiSecretDTO getServiceSecretByApiKey(@PathVariable("api-key") String apiKey) {
        return productService.getServiceSecretByApiKey(apiKey);
    }

    @PutMapping("/product/{code}/relations")
    @ApiOperation(value = "Создание и обновление связей продукта")
    public ResponseEntity putProductRelations(@PathVariable String code,
                                              @RequestBody List<ContainerDTO> containerDTO) {
        productService.createOrUpdateProductRelations(containerDTO, code);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/products/relations/tech")
    @ApiOperation(value = "Получение всех продуктов и связей с технологиями")
    public List<GetProductTechDto> getAllProductsAndTechRelations() {
        return productService.getAllProductsAndTechRelations();
    }

    @PostMapping("/product/{alias}/fitness-function/{source_id}")
    @ApiOperation(value = "Публикация результатов фитнесс-функций")
    public ResponseEntity postFitnessFunctions(
            @PathVariable String alias,
            @PathVariable("source_id") Integer sourceId,
            @RequestBody List<FitnessFunctionDTO> requests) {

        productService.postFitnessFunctions(alias, sourceId, requests);
        return new ResponseEntity<>(HttpStatus.CREATED);

    }

    @GetMapping("/product/{alias}/fitness-function")
    @ApiOperation(value = "Получение результатов фитнесс-функций")
    public ResponseEntity<AssessmentResponseDTO> getFitnessFunctions(
            @PathVariable String alias,
            @RequestParam(name = "source_id", required = false) Integer sourceId) {

        AssessmentResponseDTO response = productService.getFitnessFunctions(alias, sourceId);
        return ResponseEntity.ok(response);
    }
}
