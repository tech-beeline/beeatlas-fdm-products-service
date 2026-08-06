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
@Table(name = "non_functional_requirement_enum_core", schema = "product")
public class NonFunctionalRequirementEnumCore {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "nfr_enum_core_seq")
    @SequenceGenerator(name = "nfr_enum_core_seq", sequenceName = "product.non_functional_requirement_enum_core_id_seq", allocationSize = 1)
    private Integer id;

    @Column(name = "code")
    private String code;

    @Column(name = "source")
    private String source;
}
