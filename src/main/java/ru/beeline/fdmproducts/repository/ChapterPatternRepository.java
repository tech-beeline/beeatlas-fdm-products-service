/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.beeline.fdmproducts.domain.ChapterPattern;

import java.util.List;

@Repository
public interface ChapterPatternRepository extends JpaRepository<ChapterPattern, Integer> {

    List<ChapterPattern> findAllByChapterId(Integer chapterId);
}

