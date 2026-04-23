/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "non_functional_requirement", schema = "product")
public class NonFunctionalRequirement {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "nfr_seq")
    @SequenceGenerator(name = "nfr_seq", sequenceName = "product.non_functional_requirement_id_seq", allocationSize = 1)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nfr_id")
    private NonFunctionalRequirementEnum nfr;

    @Column(name = "source")
    private String source;

    @Column(name = "created_date")
    private LocalDateTime createdDate;

    @Column(name = "nfr_id", insertable = false, updatable = false)
    private Integer nfrId;
}
