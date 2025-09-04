package ru.beeline.fdmproducts.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.beeline.fdmlib.dto.product.GetProductTechDto;
import ru.beeline.fdmlib.dto.product.ProductPutDto;
import ru.beeline.fdmproducts.domain.Product;
import ru.beeline.fdmproducts.dto.*;
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
        return ResponseEntity.status(HttpStatus.OK)
                .body(productService.getProductsByUserAdmin(userId, request.getHeader(USER_ROLES_HEADER)));
    }

    @GetMapping("/product/{code}")
    @ApiOperation(value = "Получить продукт по alias", response = Product.class)
    public Product getProductsByCode(@PathVariable String code) {
        return productService.getProductByCode(code);
    }

    @GetMapping("/product/{code}/info")
    @ApiOperation(value = "Получить инфо продукта по alias", response = ProductInfoDTO.class)
    public ProductInfoDTO getProductsInfoByCode(@PathVariable String code) {
        return productService.getProductInfoByCode(code);
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

    @GetMapping("/products/relations/tech")
    @ApiOperation(value = "Получение всех продуктов и связей с технологиями")
    public List<GetProductTechDto> getAllProductsAndTechRelations() {
        return productService.getAllProductsAndTechRelations();
    }

    @GetMapping("/product/{alias}/fitness-function")
    @ApiOperation(value = "Получение результатов фитнесс-функций")
    public ResponseEntity<AssessmentResponseDTO> getFitnessFunctions(@PathVariable String alias,
                                                                     @RequestParam(name = "source_id", required = false) Integer sourceId,
                                                                     @RequestParam(name = "source_type", required = false) String sourceType) {

        AssessmentResponseDTO response = productService.getFitnessFunctions(alias, sourceId, sourceType);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/product/{alias}/patterns")
    @ApiOperation(value = "Получение паттернов реализованных в продукте")
    public List<PatternDTO> getProductPatterns(@PathVariable(value = "alias", required = false) String alias,
                                               @RequestParam(value = "source-id", required = false) Integer sourceId,
                                               @RequestParam(value = "source-type", required = false) String sourceType) {
        return productService.getProductPatterns(alias, sourceId, sourceType);
    }

    @GetMapping("/products/mnemonic")
    @ApiOperation(value = "Получение всех продуктов и связей с технологиями")
    public List<String> getAllMnemonics() {
        return productService.getMnemonics();
    }

    @GetMapping("/product/{cmdb}/interface/arch")
    @ApiOperation(value = "Интерфейсы продукта полученные из архитектуры")
    public List<ProductInterfaceDTO> getProductsFromStructurizr(@PathVariable String cmdb) {
        return productService.getProductsFromStructurizr(cmdb);
    }

    @GetMapping("/product/{cmdb}/interface/mapic")
    @ApiOperation(value = "Интерфейсы продукта полученные из мапик")
    public List<ProductMapicInterfaceDTO> getProductsFromMapic(@PathVariable String cmdb) {
        return productService.getProductsFromMapic(cmdb);
    }

    @GetMapping("/product/{cmdb}/container")
    @ApiOperation(value = "Просмотр контейнеров, их интерфейсов и методов в structurizr ")
    public List<ContainerInterfacesDTO> getContainersFromStructurizr(@PathVariable String cmdb) {
        return productService.getContainersFromStructurizr(cmdb);
    }

    @PostMapping("/user/{id}/products")
    @ApiOperation(value = "Создание связи пользователя и продукта")
    public ResponseEntity postUserProducts(@PathVariable String id, @RequestBody List<String> aliasLIst) {
        productService.postUserProduct(aliasLIst, id);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("/product/{alias}/fitness-function/{source_type}")
    @ApiOperation(value = "Публикация результатов фитнесс-функций")
    public ResponseEntity postFitnessFunctions(@PathVariable String alias,
                                               @PathVariable("source_type") String sourceType,
                                               @RequestBody List<FitnessFunctionDTO> requests,
                                               @RequestParam(value = "source_id", required = false) Integer sourceId) {

        productService.postFitnessFunctions(alias, sourceType, requests, sourceId);
        return new ResponseEntity<>(HttpStatus.CREATED);

    }

    @PostMapping("/product/{alias}/patterns/{source-type}")
    @ApiOperation(value = "Создание связи паттерна из технорадра с продуктами в которых они реализованны")
    public ResponseEntity postPatternProduct(@PathVariable String alias,
                                             @PathVariable(value = "source-type", required = false) String sourceType,
                                             @RequestBody List<PostPatternProductDTO> postPatternProductDTOS,
                                             @RequestParam(name = "source-id", required = false) Integer sourceId) {
        productService.postPatternProduct(alias, sourceType, postPatternProductDTOS, sourceId);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @PutMapping("/product/{code}")
    @ApiOperation(value = "Редактирование продукта")
    public ResponseEntity putProducts(@PathVariable String code, @RequestBody ProductPutDto productPutDto) {
        productService.createOrUpdate(productPutDto, code);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PutMapping("/product/{code}/relations")
    @ApiOperation(value = "Создание и обновление связей продукта")
    public ResponseEntity putProductRelations(@PathVariable String code, @RequestBody List<ContainerDTO> containerDTO) {
        productService.createOrUpdateProductRelations(containerDTO, code);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PatchMapping("product/{code}/workspace")
    @ApiOperation(value = "Добавление атрибутов к продукту")
    public ResponseEntity patchProducts(@PathVariable String code, @RequestBody ProductPutDto productPutDto) {
        productService.patchProduct(productPutDto, code);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
