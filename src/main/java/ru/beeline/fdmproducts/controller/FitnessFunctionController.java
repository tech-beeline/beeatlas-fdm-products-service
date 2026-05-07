/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.beeline.fdmproducts.dto.MainResponseDTO;
import ru.beeline.fdmproducts.dto.ffunction.GetFitnessFunctionDTO;
import ru.beeline.fdmproducts.service.FitnessFunctionService;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@Tag(description = "Product API", name = "fitness-function")
public class FitnessFunctionController {

    @Autowired
    private FitnessFunctionService fitnessFunctionService;

    @GetMapping("/dashboard/fitness-function")
    @Operation(summary = "Получение агрегации результатов фитнесс-функций")
    public ResponseEntity<MainResponseDTO> getFitnessFunctionsAggregation() {
        return ResponseEntity.ok(fitnessFunctionService.getFitnessFunctionsAggregation());
    }


    @GetMapping("/ff")
    @Operation(summary = "Получение всех ФФ")
    @ApiResponse(responseCode = "200", description = "OK",
            content = @Content(
                    mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = GetFitnessFunctionDTO.class)),
                    examples = @ExampleObject(
                            name = "Успешный ответ",
                            value = """
                                    [
                                      {
                                        "id": 1,
                                        "code": "NFR-001",
                                        "description": "string",
                                        "status": "string",
                                        "docLink": "string"
                                      }
                                    ]
                                    """
                    )))
    public ResponseEntity<List<GetFitnessFunctionDTO>> getAllFitnessFunctions() {
        return ResponseEntity.ok(fitnessFunctionService.getAllFitnessFunctions());
    }
}
