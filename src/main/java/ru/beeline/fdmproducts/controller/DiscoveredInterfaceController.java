package ru.beeline.fdmproducts.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.beeline.fdmproducts.dto.DiscoveredInterfaceOperationDTO;
import ru.beeline.fdmproducts.service.DiscoveredInterfaceService;

import java.util.List;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RestController
@RequestMapping("/api/v1")
@Api(value = "Discovered Interface API", tags = "discoveredInterface")
public class DiscoveredInterfaceController {

    @Autowired
    private DiscoveredInterfaceService discoveredInterfaceService;

    @PutMapping("/discovered-interface/{id}/operations")
    @ApiOperation(value = "Создание и обновление операций интерфейса по id")
    public ResponseEntity<?> updateInterfaceOperations(@PathVariable("id") Integer interfaceId,
                                                       @RequestBody List<DiscoveredInterfaceOperationDTO> operations) {

        discoveredInterfaceService.createOrUpdateOperations(interfaceId, operations);

        return ResponseEntity.ok().build();
    }


}
