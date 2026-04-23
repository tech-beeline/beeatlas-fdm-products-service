/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "chapter_pattern", schema = "product")
public class ChapterPattern {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "chapter_pattern_seq")
    @SequenceGenerator(name = "chapter_pattern_seq", sequenceName = "product.chapter_pattern_id_seq", allocationSize = 1)
    private Integer id;

    @Column(name = "chapter_id", nullable = false)
    private Integer chapterId;

    @Column(name = "pattern_id", nullable = false)
    private Integer patternId;
}

