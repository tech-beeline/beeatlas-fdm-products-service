package ru.beeline.fdmproducts.controller;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import ru.beeline.fdmproducts.service.TechService;
import ru.beeline.fdmproducts.domain.Product;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RestController
@RequestMapping("/api/v1/tech")
@Api(value = "Product API", tags = "tech")
public class TechController {
    @Autowired
    private TechService techService;

    @GetMapping("/{techId}/product")
    @ApiOperation(value = "Получить все продукты использующие технологию", response = List.class)
    public ResponseEntity<List<Product>> getProducts(@PathVariable Integer techId) {
        return ResponseEntity.status(HttpStatus.OK).body(techService.getProductsByTechId(techId));
    }
}
