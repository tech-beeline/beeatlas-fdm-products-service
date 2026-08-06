/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.beeline.fdmproducts.annotation.ApiErrorCodes;
import ru.beeline.fdmproducts.dto.chapter.ChapterCreateDTO;
import ru.beeline.fdmproducts.dto.chapter.ChapterCreateRequestDTO;
import ru.beeline.fdmproducts.dto.chapter.ChapterPatchRequestDTO;
import ru.beeline.fdmproducts.dto.chapter.ChapterWithNfrDTO;
import ru.beeline.fdmproducts.service.ChapterService;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "chapter", description = "Жизненные ситуации (chapter): каталог, паттерны, создание и изменение с привязкой к NFR.")
public class ChapterController {

    @Autowired
    private ChapterService chapterService;

    @ApiErrorCodes({404, 500})
    @GetMapping("/chapter")
    @Operation(summary = "Получить список всех жизненных ситуаций и требований к ним")
    public ResponseEntity<List<ChapterWithNfrDTO>> getChaptersWithNfr() {
        return ResponseEntity.status(HttpStatus.OK).body(chapterService.getChaptersWithNfr());
    }

    @ApiErrorCodes({400, 404, 500})
    @GetMapping("/chapter/{id}/patterns")
    @Operation(summary = "Идентификаторы паттернов для главы (chapter)",
            description = "Список pattern_id из Techradar, связанных с жизненной ситуацией.")
    public ResponseEntity<List<Integer>> getChapterPatterns(@Parameter(description = "Id главы (chapter)") @PathVariable("id") Integer id) {
        return ResponseEntity.ok(chapterService.getPatternIdsByChapterId(id));
    }

    @ApiErrorCodes({400, 403, 404, 500})
    @PostMapping("/chapter")
    @Operation(summary = "Создать жизненную ситуацию (chapter)",
            description = "Тело ChapterCreateRequestDTO; заголовок user-roles для проверки прав администратора.")
    public ResponseEntity<ChapterCreateDTO> createChapter(@RequestBody(required = false) ChapterCreateRequestDTO body) {
        return ResponseEntity.ok(chapterService.createChapter(body));
    }

    @ApiErrorCodes({400, 403, 404, 500})
    @PatchMapping("/chapter")
    @Operation(summary = "Изменить главу по id или code",
            description = "Ровно один из query-параметров id или code должен задавать изменяемую запись.")
    public ResponseEntity<Void> patchChapter(@Parameter(description = "Id главы") @RequestParam(required = false) Integer id,
                                             @Parameter(description = "Код главы") @RequestParam(required = false) String code,
                                             @RequestBody(required = false) ChapterPatchRequestDTO body) {
        chapterService.patchChapter(id, code, body);
        return ResponseEntity.ok().build();
    }
}
