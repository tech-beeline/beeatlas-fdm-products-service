/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "product_patterns", schema = "product")
public class ProductPatterns {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "product_patterns_seq")
    @SequenceGenerator(name = "product_patterns_seq", sequenceName = "product_patterns_id_seq",
            schema = "product", allocationSize = 1)
    private Integer id;

    @Column(name = "pattern_code", nullable = false)
    private String patternCode;

    @Column(name = "is_check", nullable = false)
    private Boolean isCheck;

    @Column(name = "result_details")
    private String resultDetails;

    @Column(name = "product_alias", nullable = false)
    private String productAlias;

    @Column(name = "product_branch", nullable = false)
    private String productBranch;

    @Column(name = "source_type_id", nullable = false)
    private Integer sourceTypeId;

    @Column(name = "source_id")
    private Integer sourceId;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Column(name = "is_actual", nullable = false)
    private Boolean isActual;
}
