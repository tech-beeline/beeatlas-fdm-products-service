package ru.beeline.fdmproducts.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.beeline.fdmproducts.annotation.ApiErrorCodes;
import ru.beeline.fdmproducts.dto.SourceMetricDto;
import ru.beeline.fdmproducts.dto.dashboard.SourceMetricRequestDto;
import ru.beeline.fdmproducts.service.SourceMetricService;

@Slf4j
@RestController
@RequestMapping("/api/v1")
@Tag(name = "source-metric", description = "Метрики источников данных дашборда: чтение и обновление конфигурации метрик.")
public class SourceMetricController {

    private final SourceMetricService sourceMetricService;

    @Autowired
    public SourceMetricController(SourceMetricService sourceMetricService) {
        this.sourceMetricService = sourceMetricService;
    }

    @ApiErrorCodes({500})
    @GetMapping("/source-metric")
    @Operation(summary = "Получить метрики источников",
            description = "Возвращает агрегированный объект метрик (поля DTO), а не массив отдельных записей.")
    @ApiResponse(responseCode = "200", description = "Успешный ответ",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = SourceMetricDto.class)))
    public ResponseEntity<SourceMetricDto> getSourceMetrics() {
        log.info("GET /api/v1/source-metric — запрос списка source_metric");
        SourceMetricDto response = sourceMetricService.getAllMetrics();
        int products = response.getProducts() != null ? response.getProducts().size() : 0;
        int containers = response.getContainers() != null ? response.getContainers().size() : 0;
        int interfaces = response.getInterfaces() != null ? response.getInterfaces().size() : 0;
        log.info("GET /api/v1/source-metric — ответ: products={}, containers={}, interfaces={}",
                products, containers, interfaces);
        return ResponseEntity.ok(response);
    }

    @ApiErrorCodes({400, 404, 500})
    @PutMapping("/source-metric")
    @Operation(summary = "Обновить метрику источника",
            description = "Частичное обновление: entity/id задают целевую сущность; тело содержит новые значения метрик.")
    @ApiResponse(responseCode = "200", description = "Обновление выполнено")
    public ResponseEntity<Void> updateSourceMetric(
            @Parameter(description = "Тип сущности (используется сервисом для адресации записи)") @RequestParam(name = "entity", required = false) String entity,
            @Parameter(description = "Идентификатор сущности") @RequestParam(name = "id", required = false) Long id,
            @RequestBody SourceMetricRequestDto body) {

        sourceMetricService.updateSourceMetric(entity, id, body.getSourceMetric());
        return ResponseEntity.ok().build();
    }
}