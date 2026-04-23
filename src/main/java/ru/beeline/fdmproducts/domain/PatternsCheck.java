/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.domain;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.persistence.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "patterns_check", schema = "product")
public class PatternsCheck {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "patterns_check_seq")
    @SequenceGenerator(name = "patterns_check_seq", sequenceName = "patterns_check_id_seq", allocationSize = 1)
    private Integer id;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assessment_id")
    private PatternsAssessment assessment;

    @Column(name = "pattern_code")
    private String patternCode;

    @Column(name = "is_check")
    private Boolean isCheck;

    @Column(name = "result_details")
    private String resultDetails;
}
