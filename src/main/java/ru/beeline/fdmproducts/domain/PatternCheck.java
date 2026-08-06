/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.domain;

import lombok.*;

import javax.persistence.*;
import java.time.LocalDateTime;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "pattern_check", schema = "product")
public class PatternCheck {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "pattern_check_seq")
    @SequenceGenerator(name = "pattern_check_seq", sequenceName = "product.pattern_check_id_seq", allocationSize = 1)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "pattern_code", nullable = false)
    private String patternCode;

    @Column(name = "is_check", nullable = false)
    private Boolean isCheck;

    @Column(name = "result_details")
    private String resultDetails;

    @Column(name = "create_date", nullable = false)
    private LocalDateTime createDate;
}
