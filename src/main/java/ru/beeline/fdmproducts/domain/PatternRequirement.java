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
@Table(name = "pattern_requirement", schema = "product", uniqueConstraints = @UniqueConstraint(columnNames = {"pattern_id", "nfr_id"}))
public class PatternRequirement {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "pattern_requirement_seq")
    @SequenceGenerator(name = "pattern_requirement_seq", sequenceName = "product.pattern_requirement_id_seq", allocationSize = 1)
    private Integer id;

    @Column(name = "pattern_id", nullable = false)
    private Integer patternId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nfr_id", nullable = false)
    private NonFunctionalRequirementEnum nfr;
}
