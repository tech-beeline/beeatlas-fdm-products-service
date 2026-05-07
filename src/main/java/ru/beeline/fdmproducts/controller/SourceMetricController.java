package ru.beeline.fdmproducts.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.beeline.fdmproducts.dto.SourceMetricDto;
import ru.beeline.fdmproducts.dto.dashboard.SourceMetricRequestDto;
import ru.beeline.fdmproducts.service.SourceMetricService;

@RestController
@RequestMapping("/api/v1")
@Tag(description = "Source Metric API", name = "source-metric")
public class SourceMetricController {

    private final SourceMetricService sourceMetricService;

    @Autowired
    public SourceMetricController(SourceMetricService sourceMetricService) {
        this.sourceMetricService = sourceMetricService;
    }

    @GetMapping("/source-metric")
    @Operation(summary = "Получить список метрик источника")
    @ApiResponse(responseCode = "200", description = "Успешный ответ",
            content = @Content(mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = SourceMetricDto.class))))
    public ResponseEntity<SourceMetricDto> getSourceMetrics() {
        return ResponseEntity.ok(sourceMetricService.getAllMetrics());
    }

    @PutMapping("/source-metric")
    public ResponseEntity<Void> updateSourceMetric(
            @RequestParam(name = "entity", required = false) String entity,
            @RequestParam(name = "id", required = false) Long id,
            @RequestBody SourceMetricRequestDto body) {

        sourceMetricService.updateSourceMetric(entity, id, body.getSourceMetric());
        return ResponseEntity.ok().build();
    }
}