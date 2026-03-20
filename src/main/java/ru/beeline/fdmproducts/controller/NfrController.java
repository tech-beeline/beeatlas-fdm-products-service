/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.beeline.fdmproducts.dto.ErrorMessageDTO;
import ru.beeline.fdmproducts.dto.NfrItemDTO;
import ru.beeline.fdmproducts.service.InfraService;
import ru.beeline.fdmproducts.service.NonFunctionalRequirementService;
import ru.beeline.fdmproducts.service.ProductService;

import java.util.List;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RestController
@RequestMapping("/api/v1")
@Api(value = "Product API", tags = "product")
public class NfrController {

    @Autowired
    private ProductService productService;

    @Autowired
    private InfraService infraService;

    @Autowired
    private NonFunctionalRequirementService nonFunctionalRequirementService;

    @GetMapping("/nfr/product")
    @ApiOperation(value = "Получить все актуальные версии требований NFR, связанные с продуктом")
    @ApiResponses(value = {@ApiResponse(code = 400, message = "Не передан или передано несколько идентификаторов"), @ApiResponse(code = 404, message = "Продукт не найден"), @ApiResponse(code = 200, message = "Список NFR продукта")})
    public ResponseEntity<?> getProductNfr(@RequestParam(value = "id", required = false) Integer id,
                                           @RequestParam(value = "alias", required = false) String alias,
                                           @RequestParam(value = "api-key", required = false) String apiKey) {

        long providedCount = (id != null ? 1 : 0) + (alias != null && !alias.isBlank() ? 1 : 0) + (apiKey != null && !apiKey.isBlank() ? 1 : 0);

        if (providedCount == 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorMessageDTO("Не передан один из идентификаторов приложения: id/alias/api-key"));
        }
        if (providedCount > 1) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorMessageDTO("Передано несколько идентификаторов приложения"));
        }

        var productOpt = nonFunctionalRequirementService.findProductByIdOrAliasOrApiKey(id, alias, apiKey);
        if (productOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorMessageDTO("Продукт с указанным идентификатором не найден"));
        }
        List<NfrItemDTO> nfrList = nonFunctionalRequirementService.getProductNfr(productOpt.get().getId());
        return ResponseEntity.ok(nfrList);
    }

}
