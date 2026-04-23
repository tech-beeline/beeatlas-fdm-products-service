/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.domain;

import lombok.*;

import javax.persistence.*;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "chapter_nfr", schema = "product")
public class ChapterNfr {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "chapter_nfr_seq")
    @SequenceGenerator(name = "chapter_nfr_seq", sequenceName = "product.chapter_nfr_id_seq", allocationSize = 1)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chapter_id")
    private Chapter chapter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nfr_id")
    private NonFunctionalRequirementEnum nfr;
}
