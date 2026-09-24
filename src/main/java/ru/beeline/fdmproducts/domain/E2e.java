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
@Table(name = "e2e")
public class E2e {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "e2e_id_generator")
    @SequenceGenerator(name = "e2e_id_generator", sequenceName = "e2e_id_seq", allocationSize = 1)
    @Column(name = "id")
    private Integer id;

    @Column(name = "code", nullable = false, unique = true)
    private String code;

    @Column(name = "name")
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "bi_step_code")
    private String biStepCode;

    @Column(name = "source")
    private String source;
}
