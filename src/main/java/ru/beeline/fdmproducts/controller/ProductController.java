/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.controller;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.beeline.fdmproducts.domain.Product;
import ru.beeline.fdmproducts.dto.*;
import ru.beeline.fdmproducts.dto.dashboard.ResultDTO;
import ru.beeline.fdmproducts.dto.ffunction.FitnessFunctionDTO;
import ru.beeline.fdmproducts.service.InfraService;
import ru.beeline.fdmproducts.service.ProductService;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

import static ru.beeline.fdmproducts.utils.Constant.USER_ID_HEADER;
import static ru.beeline.fdmproducts.utils.Constant.USER_ROLES_HEADER;

@RestController
@RequestMapping("/api")
public class ProductController {

    @Autowired
    private ProductService productService;

    @Autowired
    private InfraService infraService;


    @GetMapping("/v1/user/product")
    @Operation(summary = "Получить все продукты пользователя")
    public ResponseEntity<List<Product>> getProducts(HttpServletRequest request) {
        Integer userId = Integer.valueOf(request.getHeader(USER_ID_HEADER));
        return ResponseEntity.status(HttpStatus.OK).body(productService.getProductsByUser(userId));
    }

    @GetMapping("/v1/product/infra")
    @Operation(summary = "Получить элементы инфраструктуры cmdb по имени")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешный ответ с элементами инфраструктуры"),
            @ApiResponse(responseCode = "400", description = "Параметр 'name' отсутствует или пустой"),
            @ApiResponse(responseCode = "404", description = "Элементы инфраструктуры с заданным именем не найдены")
    })
    public ResponseEntity<ProductInfraDto> getProductInfra(@RequestParam String name) {
        return ResponseEntity.status(HttpStatus.OK).body(infraService.getProductInfraByName(name));
    }

    @GetMapping("/v1/product/infra/contains")
    @Operation(summary = "Получить элементы инфраструктуры содержащие имя")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешный ответ с элементами инфраструктуры"),
            @ApiResponse(responseCode = "400", description = "Параметр 'name' отсутствует или пустой"),
            @ApiResponse(responseCode = "404", description = "Элементы инфраструктуры с заданным именем не найдены")
    })
    public ResponseEntity<List<ProductInfraDtoDb>> getProductInfraContainsName(@RequestParam String name) {
        return ResponseEntity.status(HttpStatus.OK).body(infraService.getProductInfraContainsName(name));
    }

    @GetMapping("/v1/product/infra/search")
    @Operation(summary = "Поиск по параметру и значению в properties")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешный ответ с данными"),
            @ApiResponse(responseCode = "400", description = "Параметры отсутствуют или пусты")
    })
    public ResponseEntity<List<ProductInfraSearchDto>> searchInfra(@Parameter(description = "Имя параметра", required = true) @RequestParam String parameter,
                                                                   @Parameter(description = "Значение параметра", required = true) @RequestParam String value) {
        List<ProductInfraSearchDto> result = infraService.searchByParameterValue(parameter, value);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/v1/user/product/admin")
    @Operation(summary = "Получить все продукты пользователя")
    public ResponseEntity<List<Product>> getProductsAdmin(HttpServletRequest request) {
        Integer userId = Integer.valueOf(request.getHeader(USER_ID_HEADER));
        return ResponseEntity.status(HttpStatus.OK)
                .body(productService.getProductsByUserAdmin(userId, request.getHeader(USER_ROLES_HEADER)));
    }

    @GetMapping("/v1/product/{code}")
    @Operation(summary = "Получить продукт по alias")
    public ProductFullDTO getProductsByCode(@PathVariable String code) {
        return productService.getProductDTOByCode(code);
    }

    @GetMapping("/v1/product/{id}/availability")
    @Operation(summary = "Получить доступность продукта")
    public ProductAvailableDTO getAvailableProductsByCode(@PathVariable String id) {
        return productService.getAvailableProductsByCode(id);
    }

    @GetMapping("/v1/product/{cmdb}/influence")
    @Operation(summary = "Получить массив связанных систем для продукта по cmdb мнемонике")
    public SystemRelationDto getInfluencesByCmdb(@PathVariable String cmdb) {
        return productService.getInfluencesByCmdb(cmdb);
    }

    @GetMapping("/v1/product/{id}/tc-implementation")
    @Operation(summary = "Получить идентификаторы реализованных TC по продукту")
    public List<Integer> getTCIdsByProductId(@PathVariable Integer id) {
        return productService.getTCIdsByProductId(id);
    }

    @GetMapping("/v1/product/by-ids")
    @Operation(summary = "Получить продукты по списку идентификаторов")
    public List<GetProductsByIdsDTO> getProductsByIds(@RequestParam List<Integer> ids) {
        return productService.getProductByIds(ids);
    }

    @GetMapping("/v1/product/{code}/info")
    @Operation(summary = "Получить инфо продукта по alias")
    public ProductInfoDTO getProductsInfoByCode(@PathVariable String code) {
        return productService.getProductInfoByCode(code);
    }

    @GetMapping("/v2/product/{code}/info")
    @Operation(summary = "Получить инфо продукта по alias")
    public ProductInfoV2DTO getProductsInfoByCodeV2(@PathVariable String code) {
        return productService.getProductInfoByCodeV2(code);
    }

    @GetMapping("/v1/product/info")
    @Operation(summary = "Информация по продуктам без данных по ключам structurizr")
    public List<ProductInfoShortDTO> getProductsInfo() {
        return productService.getProductInfo();
    }

    @GetMapping("/v1/product/api-secret/{api-key}")
    @Operation(summary = "Получение api secret из таблицы product")
    public ApiSecretDTO getProductSecretByApiKey(@PathVariable("api-key") String apiKey) {
        return productService.getProductByApiKey(apiKey);
    }

    @GetMapping("/v1/service/api-secret/{api-key}")
    @Operation(summary = "Получение api secret из таблицы service")
    public ApiSecretDTO getServiceSecretByApiKey(@PathVariable("api-key") String apiKey) {
        return productService.getServiceSecretByApiKey(apiKey);
    }

    @GetMapping("/v1/products/relations/tech")
    @Operation(summary = "Получение всех продуктов и связей с технологиями")
    public List<GetProductTechDto> getAllProductsAndTechRelations() {
        return productService.getAllProductsAndTechRelations();
    }

    @GetMapping("/v1/product/{alias}/fitness-function")
    @Operation(summary = "Получение результатов фитнесс-функций")
    public ResponseEntity<AssessmentResponseDTO> getFitnessFunctions(@PathVariable String alias,
                                                                     @RequestParam(name = "source_id", required = false) Integer sourceId,
                                                                     @RequestParam(name = "source_type", required = false) String sourceType) {

        AssessmentResponseDTO response = productService.getFitnessFunctions(alias, sourceId, sourceType);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/v1/product/{alias}/patterns")
    @Operation(summary = "Получение паттернов реализованных в продукте")
    public List<PatternDTO> getProductPatterns(@PathVariable(value = "alias", required = false) String alias,
                                               @RequestParam(value = "source-id", required = false) Integer sourceId,
                                               @RequestParam(value = "source-type", required = false) String sourceType) {
        return productService.getProductPatterns(alias, sourceId, sourceType);
    }

    @GetMapping("/v1/product/parent")
    @Operation(summary = "Получение мнемоники продукта для дочерних сущностей")
    public ProductInfoShortV2DTO getParent(@RequestParam Integer id, @RequestParam String type) {
        return productService.getParent(id, type);
    }

    @GetMapping("/v1/products/mnemonic")
    @Operation(summary = "Получение всех продуктов и связей с технологиями")
    public List<String> getAllMnemonics() {
        return productService.getMnemonics();
    }

    @GetMapping("/v1/product/{cmdb}/interface/arch")
    @Operation(summary = "Интерфейсы продукта полученные из архитектуры")
    public List<ProductInterfaceDTO> getProductsFromStructurizr(@PathVariable String cmdb) {
        return productService.getProductsFromStructurizr(cmdb);
    }

    @GetMapping("/v1/product/{cmdb}/interface/mapic")
    @Operation(summary = "Интерфейсы продукта полученные из мапик")
    public List<ProductMapicInterfaceDTO> getProductsFromMapic(@PathVariable String cmdb,
                                                               @RequestParam(value = "show-hidden", required = false, defaultValue = "false") Boolean showHidden) {
        return productService.getProductsFromMapic(cmdb, showHidden);
    }

    @GetMapping("/v1/product/{id}/structurizr-key")
    @Operation(summary = "Получение ключей structurizr членом команды")
    public ApiKeyDTO getKey(@PathVariable Integer id) {
        return productService.getKey(id);
    }

    @GetMapping("/v1/product/{cmdb}/container")
    @Operation(summary = "Просмотр контейнеров, их интерфейсов и методов в structurizr ")
    public List<ContainerInterfacesDTO> getContainersFromStructurizr(@PathVariable String cmdb,
                                                                     @RequestParam(value = "show-hidden", required = false, defaultValue = "false") Boolean showHidden) {
        return productService.getContainersFromStructurizr(cmdb, showHidden);
    }

    @GetMapping("/v1/product/{cmdb}/e2e")
    @Operation(summary = "Просмотр списка e2e процессов")
    public List<ResultDTO> getE2eProcessByCmdb(@PathVariable String cmdb) {
        return productService.getE2eProcessByCmdb(cmdb);
    }

    @GetMapping("/v1/product/{alias}/free")
    @Operation(summary = "Проверка доступности alias приложения")
    public IsUniqAliasDTO getFreeAlias(@PathVariable String alias) {
        return productService.getFreeAlias(alias);
    }

    @GetMapping("/v1/product/{alias}/employee")
    @Operation(summary = "Информацию о сотрудниках из команды продукта")
    public List<GetUserProfileDTO> getEmployeeByAlias(@PathVariable String alias) {
        return productService.getEmployeeByAlias(alias);
    }

    @GetMapping("/v1/product/implemented/container/tech-capability")
    @Operation(summary = "Получение ТС реализованных в контейнерах продукта")
    public List<TcDTO> getTcByContainerProduct(@RequestParam(value = "alias", required = false) String alias,
                                               @RequestParam(value = "containers", required = false) List<String> containers) {
        return productService.getTcByContainerProduct(alias, containers);
    }

    @PostMapping("/v1/user/{id}/products")
    @Operation(summary = "Создание связи пользователя и продукта")
    public ResponseEntity postUserProducts(@PathVariable Integer id, @RequestBody List<String> aliasLIst) {
        productService.postUserProduct(aliasLIst, id);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("/v1/product/{alias}/fitness-function/{source_type}")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Публикация результатов фитнесс-функций")
    public ResponseEntity<Void> postFitnessFunctions(@PathVariable String alias,
                                                     @PathVariable("source_type") String sourceType,
                                                     @RequestBody List<FitnessFunctionDTO> requests,
                                                     @RequestParam(value = "source_id", required = false) Integer sourceId) {

        productService.postFitnessFunctions(alias, sourceType, requests, sourceId);
        return new ResponseEntity<>(HttpStatus.CREATED);

    }

    @PostMapping("/v1/product/{alias}/patterns/{source-type}")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Создание связи паттерна из технорадра с продуктами в которых они реализованны")
    public ResponseEntity postPatternProduct(@PathVariable String alias,
                                             @PathVariable(value = "source-type", required = false) String sourceType,
                                             @RequestBody List<PostPatternProductDTO> postPatternProductDTOS,
                                             @RequestParam(name = "source-id", required = false) Integer sourceId) {
        productService.postPatternProduct(alias, sourceType, postPatternProductDTOS, sourceId);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @PutMapping("/v1/product/{code}")
    @Operation(summary = "Редактирование продукта")
    public ResponseEntity putProducts(@PathVariable String code, @RequestBody ProductPutDto productPutDto) {
        productService.createOrUpdate(productPutDto, code);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PutMapping("/v1/product/{code}/relations")
    @Operation(summary = "Создание и обновление связей продукта")
    public ResponseEntity putProductRelations(@PathVariable String code,
                                              @RequestBody List<ContainerDTO> containerDTO,
                                              @RequestParam(name = "source", required = false) String source) {
        ValidationErrorResponse errorEntity = productService.createOrUpdateProductRelations(containerDTO, code, source);
        if (errorEntity.hasErrors()) {
            return ResponseEntity.status(207).body(Map.of("errorEntity", errorEntity));
        }
        return ResponseEntity.ok(Map.of("message", "Сущности успешно сохранены"));
    }

    @PutMapping("/v1/product")
    @Operation(summary = "Создавать/обновлять приложения")
    public ResponseEntity updateProduct(@RequestBody PutUpdateProductDTO putUpdateProductDTO,
                                        HttpServletRequest request) {
        productService.updateProduct(putUpdateProductDTO, request.getHeader(USER_ROLES_HEADER));
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PatchMapping("/v1/product/{code}/workspace")
    @Operation(summary = "Добавление атрибутов к продукту")
    public ResponseEntity patchProducts(@PathVariable String code, @RequestBody ProductPutDto productPutDto) {
        productService.patchProduct(productPutDto, code);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PatchMapping("/v1/product/{cmdb}/source")
    @Operation(summary = "Проставление источника обновления продукта и время подгрузки данных.")
    public ResponseEntity patchProductsSource(@PathVariable String cmdb,
                                              @RequestParam(name = "source-name", required = false) String sourceName) {
        productService.patchProductSource(cmdb, sourceName);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
