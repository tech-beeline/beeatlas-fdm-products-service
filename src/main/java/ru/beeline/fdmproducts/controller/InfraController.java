package ru.beeline.fdmproducts.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.beeline.fdmproducts.dto.InfraRequestDTO;
import ru.beeline.fdmproducts.service.InfraService;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RestController
@RequestMapping("/api/v1/infra")
@Api(value = "Infra API", tags = "infra")
public class InfraController {
    @Autowired
    private InfraService infraService;


    @PostMapping("/{product}")
    @ApiOperation("Синхронизация инфраструктуры продукта")
    public ResponseEntity<Void> syncInfrastructure(
            @PathVariable String product,
            @RequestBody InfraRequestDTO request) {

        infraService.syncInfrastructure(product, request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
