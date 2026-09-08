/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

/** Карточка sequence в ветке продукта (product.sequence), SFDM-4080. */
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "sequence")
public class ProductSequence {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequence_id_generator")
    @SequenceGenerator(name = "sequence_id_generator", sequenceName = "sequence_id_seq", allocationSize = 1)
    @Column(name = "id")
    private Integer id;

    @Column(name = "product_branch_id", nullable = false)
    private Integer productBranchId;

    @Column(name = "code", nullable = false)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "tc_code")
    private String tcCode;
}
