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
@Table(name = "chapter", schema = "product")
public class Chapter {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "chapter_seq")
    @SequenceGenerator(name = "chapter_seq", sequenceName = "product.chapter_id_seq", allocationSize = 1)
    private Integer id;

    @Column(name = "name")
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "doc_link")
    private String docLink;

    @Column(name = "code")
    private String code;
}
