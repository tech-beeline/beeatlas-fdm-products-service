package ru.beeline.fdmproducts.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.beeline.fdmproducts.dto.SourceMetricDto;
import ru.beeline.fdmproducts.dto.dashboard.SourceMetricRequestDto;
import ru.beeline.fdmproducts.service.SourceMetricService;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RestController
@RequestMapping("/api/v1")
@Api(value = "Source Metric API", tags = "source-metric")
public class SourceMetricController {

    private final SourceMetricService sourceMetricService;

    @Autowired
    public SourceMetricController(SourceMetricService sourceMetricService) {
        this.sourceMetricService = sourceMetricService;
    }

    @GetMapping("/source-metric")
    @ApiOperation(value = "Получить список метрик источника", response = SourceMetricDto.class, responseContainer = "List")
    public ResponseEntity<SourceMetricDto> getSourceMetrics() {
        return ResponseEntity.ok(sourceMetricService.getAllMetrics());
    }

    @PutMapping("/source-metric")
    public ResponseEntity<Void> updateSourceMetric(
            @RequestParam(name = "entity") String entity,
            @RequestParam(name = "id") Long id,
            @RequestBody SourceMetricRequestDto body) {

        sourceMetricService.updateSourceMetric(entity, id, body.getSourceMetric());
        return ResponseEntity.noContent().build();
    }
}