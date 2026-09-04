/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.controller;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.beeline.fdmproducts.annotation.ApiErrorCodes;
import ru.beeline.fdmproducts.domain.Product;
import ru.beeline.fdmproducts.dto.*;
import ru.beeline.fdmproducts.dto.dashboard.ResultDTO;
import ru.beeline.fdmproducts.dto.ffunction.FitnessFunctionDTO;
import ru.beeline.fdmproducts.service.InfraService;
import ru.beeline.fdmproducts.service.ProductService;

import java.util.List;
import java.util.Map;

import static ru.beeline.fdmproducts.utils.Constant.USER_ID_HEADER;

@RestController
@RequestMapping("/api")
@Tag(name = "product",
        description = "Каталог продуктов и смежные данные: пользовательские списки, CMDB/инфра, Structurizr, Mapic, "
                + "паттерны Techradar, NFR/фитнес-функции, ключи API. Эндпоинты с HttpServletRequest используют заголовки "
                + "user-id и при необходимости user-roles (прокси gateway).")
public class ProductController {

    @Autowired
    private ProductService productService;

    @Autowired
    private InfraService infraService;


    @ApiErrorCodes({400, 404, 500})
    @GetMapping("/v1/user/product")
    @Operation(summary = "Продукты текущего пользователя",
            description = "Список продуктов по числовому user-id из заголовка запроса.")
    public ResponseEntity<List<Product>> getProducts(@RequestHeader(value = USER_ID_HEADER) String userId) {
        return ResponseEntity.status(HttpStatus.OK).body(productService.getProductsByUser(Integer.valueOf(userId)));
    }

    @ApiErrorCodes({400, 404, 500})
    @GetMapping("/v1/user/product/auth")
    @Operation(summary = "Продукты пользователя для авторизации (облегчённый)",
            description = "Возвращает только id/name/alias без lazy-коллекций. Вызывается fdm-auth.")
    public ResponseEntity<List<ProductAuthDTO>> getProductsForAuth(@RequestHeader(value = USER_ID_HEADER) String userId) {
        return ResponseEntity.ok(productService.getProductsForAuth(Integer.valueOf(userId)));
    }

    @ApiErrorCodes({400, 404, 500})
    @GetMapping("/v1/product/infra")
    @Operation(summary = "Получить элементы инфраструктуры cmdb по имени")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешный ответ с элементами инфраструктуры"),
            @ApiResponse(responseCode = "400", description = "Параметр 'name' отсутствует или пустой"),
            @ApiResponse(responseCode = "404", description = "Элементы инфраструктуры с заданным именем не найдены")
    })
    public ResponseEntity<ProductInfraDto> getProductInfra(@Parameter(description = "Имя или код объекта в CMDB") @RequestParam String name) {
        return ResponseEntity.status(HttpStatus.OK).body(infraService.getProductInfraByName(name));
    }

    @ApiErrorCodes({400, 404, 500})
    @GetMapping("/v1/product/infra/contains")
    @Operation(summary = "Получить элементы инфраструктуры содержащие имя")
    public ResponseEntity<List<ProductInfraDtoDb>> getProductInfraContainsName(@Parameter(description = "Подстрока имени инфраструктурного элемента") @RequestParam String name) {
        return ResponseEntity.status(HttpStatus.OK).body(infraService.getProductInfraContainsName(name));
    }

    @GetMapping("/v1/product/infra/search")
    @Operation(summary = "Поиск по параметру и значению в properties")
    @ApiErrorCodes({400, 500})
    public ResponseEntity<List<ProductInfraSearchDto>> searchInfra(@Parameter(description = "Имя параметра", required = true) @RequestParam String parameter,
                                                                   @Parameter(description = "Значение параметра", required = true) @RequestParam String value) {
        List<ProductInfraSearchDto> result = infraService.searchByParameterValue(parameter, value);
        return ResponseEntity.ok(result);
    }

    @ApiErrorCodes({400, 404, 500})
    @GetMapping("/v1/user/product/admin")
    @Operation(summary = "Продукты пользователя (режим администратора)",
            description = "Использует user-id; роли получаются из fdm-auth. Расширенная выборка для админ-сценариев.")
    public ResponseEntity<List<Product>> getProductsAdmin(@RequestHeader(value = USER_ID_HEADER) String userId) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(productService.getProductsByUserAdmin(Integer.valueOf(userId)));
    }

    @ApiErrorCodes({400, 404, 500})
    @GetMapping("/v1/product/{code}")
    @Operation(summary = "Полная карточка продукта по alias",
            description = "Путь {code} — alias (код) продукта.")
    public ProductFullDTO getProductsByCode(@Parameter(description = "Alias продукта") @PathVariable String code) {
        return productService.getProductDTOByCode(code);
    }

    @ApiErrorCodes({400, 404, 500})
    @GetMapping("/v1/product/{id}/availability")
    @Operation(summary = "Доступность продукта")
    public ProductAvailableDTO getAvailableProductsByCode(@Parameter(description = "Строковый идентификатор продукта") @PathVariable String id) {
        return productService.getAvailableProductsByCode(id);
    }

    @ApiErrorCodes({400, 404, 500})
    @GetMapping("/v1/product/{cmdb}/influence")
    @Operation(summary = "Связанные системы по CMDB-мнемонике",
            description = "Граф влияний/связей между системами для продукта из CMDB.")
    public SystemRelationDto getInfluencesByCmdb(@Parameter(description = "Мнемоника CMDB продукта") @PathVariable String cmdb) {
        return productService.getInfluencesByCmdb(cmdb);
    }

    @ApiErrorCodes({400, 404, 500})
    @GetMapping("/v1/product/{id}/tc-implementation")
    @Operation(summary = "Идентификаторы технологических возможностей (ТС), реализованных в продукте")
    public List<Integer> getTCIdsByProductId(@Parameter(description = "Числовой id продукта") @PathVariable Integer id,
                                             @Parameter(description = "Ветка архитектуры продукта, по умолчанию main") @RequestParam(required = false) String branch) {
        return productService.getTCIdsByProductId(id, branch);
    }

    @ApiErrorCodes({400, 404, 500})
    @GetMapping("/v1/product/by-ids")
    @Operation(summary = "Краткие карточки продуктов по списку id")
    public List<GetProductsByIdsDTO> getProductsByIds(@Parameter(description = "Повторяющийся query-параметр ids") @RequestParam List<Integer> ids) {
        return productService.getProductByIds(ids);
    }

    @ApiErrorCodes({400, 404, 500})
    @GetMapping("/v1/product/{code}/info")
    @Operation(summary = "Информация о продукте по alias (v1)")
    public ProductInfoDTO getProductsInfoByCode(@Parameter(description = "Alias продукта") @PathVariable String code) {
        return productService.getProductInfoByCode(code);
    }

    @ApiErrorCodes({400, 404, 500})
    @GetMapping("/v2/product/{code}/info")
    @Operation(summary = "Информация о продукте по alias (v2)",
            description = "Расширенная схема ответа относительно /v1/product/{code}/info.")
    public ProductInfoV2DTO getProductsInfoByCodeV2(@Parameter(description = "Alias продукта") @PathVariable String code) {
        return productService.getProductInfoByCodeV2(code);
    }

    @ApiErrorCodes({400, 404, 500})
    @GetMapping("/v1/product/info")
    @Operation(summary = "Краткий список продуктов без ключей Structurizr",
            description = "Облегчённое представление без чувствительных данных workspace.")
    public List<ProductInfoShortDTO> getProductsInfo() {
        return productService.getProductInfo();
    }

    @ApiErrorCodes({400, 404, 500})
    @GetMapping("/v1/product/api-secret/{api-key}")
    @Operation(summary = "Секрет API продукта по ключу",
            description = "Чувствительные данные из таблицы product; ограничивать доступ.")
    public ApiSecretDTO getProductSecretByApiKey(@Parameter(description = "Публичный api-key продукта") @PathVariable("api-key") String apiKey) {
        return productService.getProductByApiKey(apiKey);
    }

    @ApiErrorCodes({400, 404, 500})
    @GetMapping("/v1/service/api-secret/{api-key}")
    @Operation(summary = "Секрет API сервиса по ключу",
            description = "Чувствительные данные из таблицы service.")
    public ApiSecretDTO getServiceSecretByApiKey(@Parameter(description = "Ключ сервиса") @PathVariable("api-key") String apiKey) {
        return productService.getServiceSecretByApiKey(apiKey);
    }

    @ApiErrorCodes({404, 500})
    @GetMapping("/v1/products/relations/tech")
    @Operation(summary = "Все продукты со связями на технологии Techradar")
    public List<GetProductTechDto> getAllProductsAndTechRelations() {
        return productService.getAllProductsAndTechRelations();
    }

    @ApiErrorCodes({400, 404, 500})
    @GetMapping("/v1/product/{alias}/fitness-function")
    @Operation(summary = "Результаты фитнес-функций по продукту",
            description = "Опционально фильтр по источнику: source_id и source_type.")
    public ResponseEntity<AssessmentResponseDTO> getFitnessFunctions(@Parameter(description = "Alias продукта") @PathVariable String alias,
                                                                     @Parameter(description = "Идентификатор источника оценки") @RequestParam(name = "source_id", required = false) Integer sourceId,
                                                                     @Parameter(description = "Тип источника оценки") @RequestParam(name = "source_type", required = false) String sourceType) {

        AssessmentResponseDTO response = productService.getFitnessFunctions(alias, sourceId, sourceType);
        return ResponseEntity.ok(response);
    }

    @ApiErrorCodes({400, 404, 500})
    @GetMapping("/v1/product/{alias}/patterns")
    @Operation(summary = "Паттерны Techradar, связанные с продуктом")
    public List<PatternDTO> getProductPatterns(@Parameter(description = "Alias продукта") @PathVariable(value = "alias", required = false) String alias,
                                               @Parameter(description = "Идентификатор источника") @RequestParam(value = "source-id", required = false) Integer sourceId,
                                               @Parameter(description = "Тип источника") @RequestParam(value = "source-type", required = false) String sourceType) {
        return productService.getProductPatterns(alias, sourceId, sourceType);
    }

    @ApiErrorCodes({400, 404, 500})
    @GetMapping("/v1/product/parent")
    @Operation(summary = "Родительский продукт для дочерней сущности",
            description = "По id и типу дочернего объекта возвращает краткую информацию о продукте-владельце.")
    public ProductInfoShortV2DTO getParent(@Parameter(description = "Id дочерней сущности") @RequestParam Integer id,
                                           @Parameter(description = "Тип сущности (контракт домена)") @RequestParam String type) {
        return productService.getParent(id, type);
    }

    @ApiErrorCodes({404, 500})
    @GetMapping("/v1/products/mnemonic")
    @Operation(summary = "Список мнемоник всех продуктов",
            description = "Только строковые идентификаторы (mnemonic/cmdb), без связей с технологиями — не путать с /v1/products/relations/tech.")
    public List<String> getAllMnemonics() {
        return productService.getMnemonics();
    }

    @ApiErrorCodes({400, 404, 500})
    @GetMapping("/v1/product/{cmdb}/interface/arch")
    @Operation(summary = "Интерфейсы из модели архитектуры (Structurizr)")
    public List<ProductInterfaceDTO> getProductsFromStructurizr(@Parameter(description = "CMDB-мнемоника продукта") @PathVariable String cmdb,
                                                                @Parameter(description = "Ветка архитектуры продукта, по умолчанию main") @RequestParam(required = false) String branch) {
        return productService.getProductsFromStructurizr(cmdb, branch);
    }

    @ApiErrorCodes({400, 404, 500})
    @GetMapping("/v1/product/{cmdb}/interface/{source-type}")
    @Operation(summary = "Интерфейсы, обнаруженные из указанного источника",
            description = "source-type сравнивается с discovered_interface.source без учёта регистра (mapic, sparx, ...). "
                    + "/interface/arch обрабатывается отдельным маршрутом и сюда не попадает.")
    public List<ProductMapicInterfaceDTO> getInterfacesBySource(
            @Parameter(description = "CMDB-мнемоника продукта") @PathVariable String cmdb,
            @Parameter(description = "Тип источника интерфейсов (mapic, sparx, ...)") @PathVariable("source-type") String sourceType,
            @Parameter(description = "Включать скрытые интерфейсы") @RequestParam(value = "show-hidden", required = false, defaultValue = "false") Boolean showHidden) {
        return productService.getInterfacesBySource(cmdb, sourceType, showHidden);
    }

    @ApiErrorCodes({400, 404, 500})
    @GetMapping("/v2/product/{cmdb}/interface/{source-type}")
    @Operation(summary = "Интерфейсы, обнаруженные из указанного источника (v2)",
            description = "То же самое, что /v1/product/{cmdb}/interface/{source-type}, но externalId в ответе — string, "
                    + "а не number: discovered_interface.external_id хранит и числовые id (MAPIC), и составные коды "
                    + "интерфейсов (Sparx, вида \"interfaceCode.containerCode\"), поэтому v1 падает 400 на Sparx-данных.")
    public List<ProductMapicInterfaceV2DTO> getInterfacesBySourceV2(
            @Parameter(description = "CMDB-мнемоника продукта") @PathVariable String cmdb,
            @Parameter(description = "Тип источника интерфейсов (mapic, sparx, ...)") @PathVariable("source-type") String sourceType,
            @Parameter(description = "Включать скрытые интерфейсы") @RequestParam(value = "show-hidden", required = false, defaultValue = "false") Boolean showHidden) {
        return productService.getInterfacesBySourceV2(cmdb, sourceType, showHidden);
    }

    @ApiErrorCodes({400, 404, 500})
    @GetMapping("/v1/product/{id}/structurizr-key")
    @Operation(summary = "Ключи доступа Structurizr для участника команды продукта",
            description = "Чувствительные ключи workspace; выдавать только авторизованным пользователям продукта.")
    public ApiKeyDTO getKey(@Parameter(description = "Числовой id продукта") @PathVariable Integer id) {
        return productService.getKey(id);
    }

    @ApiErrorCodes({400, 404, 500})
    @GetMapping("/v1/product/{cmdb}/container")
    @Operation(summary = "Контейнеры продукта с интерфейсами и методами (Structurizr)")
    public List<ContainerInterfacesDTO> getContainersFromStructurizr(@Parameter(description = "CMDB-мнемоника") @PathVariable String cmdb,
                                                                     @Parameter(description = "Ветка архитектуры продукта, по умолчанию main") @RequestParam(required = false) String branch,
                                                                     @Parameter(description = "Показывать скрытые элементы") @RequestParam(value = "show-hidden", required = false, defaultValue = "false") Boolean showHidden) {
        return productService.getContainersFromStructurizr(cmdb, branch, showHidden);
    }

    @ApiErrorCodes({400, 404, 500})
    @GetMapping("/v1/product/{cmdb}/e2e")
    @Operation(summary = "End-to-end процессы продукта")
    public List<ResultDTO> getE2eProcessByCmdb(@Parameter(description = "CMDB-мнемоника") @PathVariable String cmdb) {
        return productService.getE2eProcessByCmdb(cmdb);
    }

    @ApiErrorCodes({400, 404, 500})
    @GetMapping("/v1/product/{alias}/free")
    @Operation(summary = "Проверка уникальности alias приложения")
    public IsUniqAliasDTO getFreeAlias(@Parameter(description = "Предлагаемый alias") @PathVariable String alias) {
        return productService.getFreeAlias(alias);
    }

    @ApiErrorCodes({400, 404, 500})
    @GetMapping("/v1/product/{alias}/employee")
    @Operation(summary = "Сотрудники команды продукта")
    public List<GetUserProfileDTO> getEmployeeByAlias(@Parameter(description = "Alias продукта") @PathVariable String alias) {
        return productService.getEmployeeByAlias(alias);
    }

    @ApiErrorCodes({400, 404, 500})
    @GetMapping("/v1/product/implemented/container/tech-capability")
    @Operation(summary = "ТС в контейнерах продукта",
            description = "Фильтр по alias продукта и/или списку имён контейнеров.")
    public List<TcDTO> getTcByContainerProduct(@Parameter(description = "Alias продукта") @RequestParam(value = "alias", required = false) String alias,
                                               @Parameter(description = "Имена контейнеров (повторяющийся параметр)") @RequestParam(value = "containers", required = false) List<String> containers,
                                               @Parameter(description = "Ветка архитектуры продукта, по умолчанию main") @RequestParam(required = false) String branch) {
        return productService.getTcByContainerProduct(alias, containers, branch);
    }

    @ApiErrorCodes({400, 404, 500})
    @PostMapping("/v1/user/{id}/products")
    @Operation(summary = "Привязать продукты к пользователю",
            description = "Тело — список alias продуктов; path id — пользователь.")
    public ResponseEntity<Void> postUserProducts(@Parameter(description = "Идентификатор пользователя") @PathVariable Integer id,
                                                 @RequestBody List<String> aliasLIst) {
        productService.postUserProduct(aliasLIst, id);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @ApiErrorCodes({400, 404, 500})
    @PostMapping("/v1/user/{id}/products/replacement")
    @Operation(summary = "Полная замена продуктов пользователя",
            description = "Тело — список alias продуктов; path id — пользователь. Связи, не входящие в целевой набор, удаляются.")
    public ResponseEntity<Void> replaceUserProducts(@Parameter(description = "Идентификатор пользователя") @PathVariable Integer id,
                                                    @RequestBody List<String> aliasList) {
        productService.replaceUserProducts(aliasList, id);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @ApiErrorCodes({400, 404, 500})
    @PostMapping("/v1/product/{alias}/fitness-function/{source_type}")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Публикация результатов фитнес-функций",
            description = "Массовая загрузка результатов оценки; source_type в пути задаёт тип источника.")
    public ResponseEntity<Void> postFitnessFunctions(@Parameter(description = "Alias продукта") @PathVariable String alias,
                                                     @Parameter(description = "Тип источника оценки") @PathVariable("source_type") String sourceType,
                                                     @RequestBody List<FitnessFunctionDTO> requests,
                                                     @Parameter(description = "Идентификатор источника") @RequestParam(value = "source_id", required = false) Integer sourceId) {

        productService.postFitnessFunctions(alias, sourceType, requests, sourceId);
        return new ResponseEntity<>(HttpStatus.CREATED);

    }

    @ApiErrorCodes({400, 404, 500})
    @GetMapping("/v1/product/by-aliases")
    @Operation(summary = "Краткие карточки продуктов по списку alias",
            description = "Пакетное получение id, name, alias, struturizrURL по мнемоникам CMDB.")
    public List<GetProductsByIdsDTO> getProductsByAliases(
            @Parameter(description = "query-параметр aliases") @RequestParam(required = false) List<String> aliases) {
        return productService.getProductByAliases(aliases);
    }

    @ApiErrorCodes({400, 404, 500})
    @GetMapping("/v1/container/by-codes")
    @Operation(summary = "Краткие карточки контейнеров по списку кодов",
            description = "Пакетное получение id, name, code контейнера и alias/name владеющего продукта по "
                    + "CMDB-кодам контейнеров — глобальный поиск, продукт-владелец указывать не нужно.")
    public List<ContainerByCodeDTO> getContainersByCodes(
            @Parameter(description = "query-параметр codes") @RequestParam(required = false) List<String> codes) {
        return productService.getContainersByCodes(codes);
    }

    @ApiErrorCodes({400, 404, 500})
    @PostMapping("/v1/product/{alias}/patterns/{source-type}")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Связать паттерны Techradar с продуктом",
            description = "Создаёт или обновляет привязку паттернов из технорадара к продукту по alias.")
    public ResponseEntity<Void> postPatternProduct(@Parameter(description = "Alias продукта") @PathVariable String alias,
                                                   @Parameter(description = "Тип источника связи") @PathVariable(value = "source-type", required = false) String sourceType,
                                                   @RequestBody List<PostPatternProductDTO> postPatternProductDTOS,
                                                   @Parameter(description = "Идентификатор источника") @RequestParam(name = "source-id", required = false) Integer sourceId) {
        productService.postPatternProduct(alias, sourceType, postPatternProductDTOS, sourceId);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @ApiErrorCodes({400, 404, 500})
    @PutMapping("/v1/product/{code}")
    @Operation(summary = "Создать или обновить продукт",
            description = "{code} — alias продукта; полное тело карточки ProductPutDto.")
    public ResponseEntity<Void> putProducts(@Parameter(description = "Alias продукта") @PathVariable String code,
                                            @RequestBody ProductPutDto productPutDto) {
        productService.createOrUpdate(productPutDto, code);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @ApiErrorCodes({400, 404, 500})
    @PutMapping("/v1/product/{code}/relations")
    @Operation(summary = "Сохранить дерево контейнеров и связей продукта",
            description = "При ошибках валидации ответ может быть 207 Multi-Status с телом errorEntity.")
    public ResponseEntity<?> putProductRelations(@Parameter(description = "Alias продукта") @PathVariable String code,
                                                 @RequestBody List<ContainerDTO> containerDTO,
                                                 @Parameter(description = "Ветка архитектуры продукта, по умолчанию main; создаётся, если ещё не существует") @RequestParam(required = false) String branch,
                                                 @Parameter(description = "Происхождение данных (интеграция)") @RequestParam(name = "source", required = false) String source) {
        ValidationErrorResponse errorEntity = productService.createOrUpdateProductRelations(containerDTO, code, branch, source);
        if (errorEntity.hasErrors()) {
            return ResponseEntity.status(207).body(Map.of("errorEntity", errorEntity));
        }
        return ResponseEntity.ok(Map.of("message", "Сущности успешно сохранены"));
    }

    @ApiErrorCodes({400, 404, 500})
    @PutMapping("/v1/product")
    @Operation(summary = "Массовое создание/обновление приложений")
    public ResponseEntity<Void> updateProduct(@RequestBody PutUpdateProductDTO putUpdateProductDTO) {
        productService.updateProduct(putUpdateProductDTO);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @ApiErrorCodes({400, 404, 500})
    @PatchMapping("/v1/product/{code}/workspace")
    @Operation(summary = "Частичное обновление атрибутов продукта (workspace)")
    public ResponseEntity<Void> patchProducts(@Parameter(description = "Alias продукта") @PathVariable String code,
                                              @RequestBody ProductPutDto productPutDto) {
        productService.patchProduct(productPutDto, code);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @ApiErrorCodes({400, 404, 500})
    @PatchMapping("/v1/product/{cmdb}/source")
    @Operation(summary = "Обновить метку источника синхронизации продукта",
            description = "Фиксирует источник и время последней загрузки данных для CMDB-мнемоники.")
    public ResponseEntity<Void> patchProductsSource(@Parameter(description = "CMDB-мнемоника продукта") @PathVariable String cmdb,
                                                    @Parameter(description = "Имя источника данных") @RequestParam(name = "source-name", required = false) String sourceName) {
        productService.patchProductSource(cmdb, sourceName);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
