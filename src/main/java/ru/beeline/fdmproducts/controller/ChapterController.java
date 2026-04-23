/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.beeline.fdmproducts.dto.chapter.ChapterCreateDTO;
import ru.beeline.fdmproducts.dto.chapter.ChapterCreateRequestDTO;
import ru.beeline.fdmproducts.dto.chapter.ChapterPatchRequestDTO;
import ru.beeline.fdmproducts.dto.chapter.ChapterWithNfrDTO;
import ru.beeline.fdmproducts.service.ChapterService;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "chapter", description = "Chapter API")
public class ChapterController {

    @Autowired
    private ChapterService chapterService;

    @GetMapping("/chapter")
    @Operation(summary = "Получить список всех жизненных ситуаций и требований к ним")
    public ResponseEntity<List<ChapterWithNfrDTO>> getChaptersWithNfr() {
        return ResponseEntity.status(HttpStatus.OK).body(chapterService.getChaptersWithNfr());
    }

    @GetMapping("/chapter/{id}/patterns")
    @Operation(summary = "Получить список pattern_id для chapter")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "404", description = "Chapter с таким id не существует",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(
                            name = "404 NOT FOUND", value = "{\"error\": \"Chapter с таким id не существует\"}"))),
            @ApiResponse(responseCode = "200", description = "Список pattern_id",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(
                            value = "[101, 102, 103, 105]")))
    })
    public ResponseEntity<List<Integer>> getChapterPatterns(@PathVariable("id") Integer id) {
        return ResponseEntity.ok(chapterService.getPatternIdsByChapterId(id));
    }

    @PostMapping("/chapter")
    @Operation(summary = "Создать жизненную ситуацию (chapter)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "400", description = "Ошибки валидации",
                    content = @Content(mediaType = "application/json",
                            examples = {
                                    @ExampleObject(name = "отсутствуют-параметры",
                                            summary = "Не переданы обязательные параметры",
                                            value = "{\"error\": \"Не переданы обязательные параметры\"}"),
                                    @ExampleObject(name = "некорректные-nfr",
                                            summary = "Несуществующие NFR",
                                            value = "{\"error\": \"В массиве nfr переданы идентификаторы несуществующих требований\"}"),
                                    @ExampleObject(name = "некорректные-паттерны",
                                            summary = "Несуществующие паттерны",
                                            value = "{\"error\": \"В массиве patterns переданы идентификаторы несуществующих паттернов\"}")
                            })),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "доступ-запрещен",
                                    summary = "Пользователь не администратор",
                                    value = "{\"error\": \"Пользователь не является администратором\"}")))})
    public ResponseEntity<ChapterCreateDTO> createChapter(@RequestBody(required = false) ChapterCreateRequestDTO body,
                                                          @RequestHeader(value = "user-roles", required = false) String userRoles) {
        return ResponseEntity.ok(chapterService.createChapter(body, userRoles));
    }

    @PatchMapping("/chapter")
    @Operation(summary = "Изменить жизненную ситуацию (chapter) по id или code")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "400", description = "Ошибки валидации",
                    content = @Content(mediaType = "application/json",
                            examples = {
                                    @ExampleObject(name = "отсутствуют-параметры",
                                            summary = "Не переданы обязательные параметры",
                                            value = "{\"error\": \"Не переданы обязательные параметры\"}"),
                                    @ExampleObject(name = "отсутствует-идентификатор",
                                            summary = "Не передан идентификатор главы (id или code)",
                                            value = "{\"error\": \"Не передан идентификатор главы (id или code)\"}"),
                                    @ExampleObject(name = "несколько-идентификаторов",
                                            summary = "Переданы несколько идентификаторов",
                                            value = "{\"error\": \"Переданы несколько идентификаторов\"}"),
                                    @ExampleObject(name = "некорректные-паттерны",
                                            summary = "В массиве patterns переданы идентификаторы несуществующих паттернов",
                                            value = "{\"error\": \"В массиве patterns переданы идентификаторы несуществующих паттернов\"}")
                            })),
            @ApiResponse(responseCode = "404", description = "Жизненная ситуация не найдена",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "глава-не-найдена",
                                    summary = "Жизненная ситуация не найдена",
                                    value = "{\"error\": \"Жизненная ситуация не найдена\"}"))),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "доступ-запрещен",
                                    summary = "Пользователь не администратор",
                                    value = "{\"error\": \"Пользователь не является администратором\"}")))})
    public ResponseEntity<Void> patchChapter(@RequestParam(required = false) Integer id,
                                             @RequestParam(required = false) String code,
                                             @RequestHeader(value = "user-roles", required = false) String userRoles,
                                             @RequestBody(required = false) ChapterPatchRequestDTO body) {
        chapterService.patchChapter(id, code, userRoles, body);
        return ResponseEntity.ok().build();
    }
}
