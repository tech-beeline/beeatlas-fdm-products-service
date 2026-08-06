/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.beeline.fdmproducts.domain.ChapterNfr;

import java.util.List;

@Repository
public interface ChapterNfrRepository extends JpaRepository<ChapterNfr, Integer> {

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"nfr", "nfr.core"})
    @Query("SELECT cn FROM ChapterNfr cn WHERE cn.chapter.id = :chapterId")
    List<ChapterNfr> findByChapterIdWithNfrAndCore(@Param("chapterId") Integer chapterId);

    List<ChapterNfr> findByChapterId(Integer chapterId);

    List<ChapterNfr> findByNfrId(Integer nfrId);
}
