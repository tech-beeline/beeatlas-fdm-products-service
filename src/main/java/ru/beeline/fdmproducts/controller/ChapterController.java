/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.beeline.fdmproducts.dto.ChapterWithNfrDTO;
import ru.beeline.fdmproducts.service.ChapterService;

import java.util.List;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RestController
@RequestMapping("/api/v1")
@Api(value = "Chapter API", tags = "chapter")
public class ChapterController {

    @Autowired
    private ChapterService chapterService;

    @GetMapping("/chapter")
    @ApiOperation(value = "Получить список всех жизненных ситуаций и требований к ним")
    public ResponseEntity<List<ChapterWithNfrDTO>> getChaptersWithNfr() {
        return ResponseEntity.status(HttpStatus.OK).body(chapterService.getChaptersWithNfr());
    }
}
