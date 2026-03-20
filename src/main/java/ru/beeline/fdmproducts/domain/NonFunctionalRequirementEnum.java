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
@Table(name = "non_functional_requirement_enum", schema = "product")
public class NonFunctionalRequirementEnum {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "nfr_enum_seq")
    @SequenceGenerator(name = "nfr_enum_seq", sequenceName = "product.non_functional_requirement_enum_id_seq", allocationSize = 1)
    private Integer id;

    @Column(name = "name")
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "rule")
    private String rule;

    @Column(name = "version")
    private Integer version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "core_id")
    private NonFunctionalRequirementEnumCore core;
}
