package ru.beeline.fdmproducts.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.beeline.fdmlib.dto.product.GetProductTechDto;
import ru.beeline.fdmlib.dto.product.GetProductsByIdsDTO;
import ru.beeline.fdmlib.dto.product.ProductPutDto;
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

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RestController
@RequestMapping("/api/v1")
@Api(value = "Product API", tags = "product")
public class ProductController {

    @Autowired
    private ProductService productService;

    @Autowired
    private InfraService infraService;

    @GetMapping("/user/product")
    @ApiOperation(value = "Получить все продукты пользователя", response = List.class)
    public ResponseEntity<List<Product>> getProducts(HttpServletRequest request) {
        Integer userId = Integer.valueOf(request.getHeader(USER_ID_HEADER));
        return ResponseEntity.status(HttpStatus.OK).body(productService.getProductsByUser(userId));
    }

    @GetMapping("/api/v1/product/infra")
    @ApiOperation(value = "Получить элементы инфраструктуры cmdb по имени", response = List.class)
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Параметр 'name' отсутствует или пустой"),
            @ApiResponse(code = 404, message = "Элементы инфраструктуры с заданным именем не найдены"),
            @ApiResponse(code = 200, message = "Успешный ответ с элементами инфраструктуры")
    })
    public ResponseEntity<ProductInfraDto> getProductInfra(@RequestParam String name){
        return ResponseEntity.status(HttpStatus.OK).body(infraService.getProductInfraByName(name));
    }

    @GetMapping("/user/product/admin")
    @ApiOperation(value = "Получить все продукты пользователя", response = List.class)
    public ResponseEntity<List<Product>> getProductsAdmin(HttpServletRequest request) {
        Integer userId = Integer.valueOf(request.getHeader(USER_ID_HEADER));
        return ResponseEntity.status(HttpStatus.OK)
                .body(productService.getProductsByUserAdmin(userId, request.getHeader(USER_ROLES_HEADER)));
    }

    @GetMapping("/product/{code}")
    @ApiOperation(value = "Получить продукт по alias", response = ProductFullDTO.class)
    public ProductFullDTO getProductsByCode(@PathVariable String code) {
        return productService.getProductDTOByCode(code);
    }

    @GetMapping("/product/{cmdb}/influence")
    @ApiOperation(value = "Получить массив связанных систем для продукта по cmdb мнемонике", response = SystemRelationDto.class)
    public SystemRelationDto getInfluencesByCmdb(@PathVariable String cmdb) {
        return productService.getInfluencesByCmdb(cmdb);
    }

    @GetMapping("/product/{id}/tc-implementation")
    @ApiOperation(value = "Получить идентификаторы реализованных TC по продукту", response = List.class)
    public List<Integer> getTCIdsByProductId(@PathVariable Integer id) {
        return productService.getTCIdsByProductId(id);
    }

    @GetMapping("/product/by-ids")
    @ApiOperation(value = "Получить продукты по списку идентификаторов", response = List.class)
    public List<GetProductsByIdsDTO> getProductsByIds(@RequestParam List<Integer> ids) {
        return productService.getProductByIds(ids);
    }

    @GetMapping("/product/{code}/info")
    @ApiOperation(value = "Получить инфо продукта по alias", response = ProductInfoDTO.class)
    public ProductInfoDTO getProductsInfoByCode(@PathVariable String code) {
        return productService.getProductInfoByCode(code);
    }

    @GetMapping("/product/info")
    @ApiOperation(value = "Информация по продуктам без данных по ключам structurizr", response = List.class)
    public List<ProductInfoShortDTO> getProductsInfo() {
        return productService.getProductInfo();
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

    @GetMapping("/product/parent")
    @ApiOperation(value = "Получение мнемоники продукта для дочерних сущностей")
    public ProductInfoShortV2DTO getParent(@RequestParam Integer id, @RequestParam String type) {
        return productService.getParent(id, type);
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
    public List<ProductMapicInterfaceDTO> getProductsFromMapic(@PathVariable String cmdb,
                                                               @RequestParam(value = "show-hidden", required = false,
                                                                       defaultValue = "false") Boolean showHidden) {
        return productService.getProductsFromMapic(cmdb, showHidden);
    }

    @GetMapping("/product/{id}/structurizr-key")
    @ApiOperation(value = "Получение ключей structurizr членом команды")
    public ApiKeyDTO getKey(@PathVariable Integer id) {
        return productService.getKey(id);
    }

    @GetMapping("/product/{cmdb}/container")
    @ApiOperation(value = "Просмотр контейнеров, их интерфейсов и методов в structurizr ")
    public List<ContainerInterfacesDTO> getContainersFromStructurizr(@PathVariable String cmdb,
                                                                     @RequestParam(value = "show-hidden", required = false,
                                                                             defaultValue = "false") Boolean showHidden) {
        return productService.getContainersFromStructurizr(cmdb, showHidden);
    }

    @GetMapping("/product/{cmdb}/e2e")
    @ApiOperation(value = "Просмотр списка e2e процессов")
    public List<ResultDTO> getE2eProcessByCmdb(@PathVariable String cmdb) {
        return productService.getE2eProcessByCmdb(cmdb);
    }

    @PostMapping("/user/{id}/products")
    @ApiOperation(value = "Создание связи пользователя и продукта")
    public ResponseEntity postUserProducts(@PathVariable Integer id, @RequestBody List<String> aliasLIst) {
        productService.postUserProduct(aliasLIst, id);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("/product/{alias}/fitness-function/{source_type}")
    @ResponseStatus(HttpStatus.CREATED)
    @ApiOperation(value = "Публикация результатов фитнесс-функций")
    public ResponseEntity<Void> postFitnessFunctions(@PathVariable String alias,
                                                     @PathVariable("source_type") String sourceType,
                                                     @RequestBody List<FitnessFunctionDTO> requests,
                                                     @RequestParam(value = "source_id", required = false) Integer sourceId) {

        productService.postFitnessFunctions(alias, sourceType, requests, sourceId);
        return new ResponseEntity<>(HttpStatus.CREATED);

    }

    @PostMapping("/product/{alias}/patterns/{source-type}")
    @ResponseStatus(HttpStatus.CREATED)
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
    public ResponseEntity putProductRelations(@PathVariable String code,
                                              @RequestBody List<ContainerDTO> containerDTO,
                                              @RequestParam(name = "source", required = false) String source) {
        ValidationErrorResponse errorEntity = productService.createOrUpdateProductRelations(containerDTO, code, source);
        if (errorEntity.hasErrors()) {
            return ResponseEntity.status(207).body(Map.of("errorEntity", errorEntity));
        }
        return ResponseEntity.ok(Map.of("message", "Сущности успешно сохранены"));
    }

    @PatchMapping("product/{code}/workspace")
    @ApiOperation(value = "Добавление атрибутов к продукту")
    public ResponseEntity patchProducts(@PathVariable String code, @RequestBody ProductPutDto productPutDto) {
        productService.patchProduct(productPutDto, code);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PatchMapping("product/{cmdb}/source")
    @ApiOperation(value = "Проставление источника обновления продукта и время подгрузки данных.")
    public ResponseEntity patchProductsSource(@PathVariable String cmdb,
                                              @RequestParam(name = "source-name", required = false) String sourceName) {
        productService.patchProductSource(cmdb, sourceName);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
