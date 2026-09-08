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
@Table(name = "seq_product_step")
public class SeqProductStep {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_product_step_id_generator")
    @SequenceGenerator(name = "seq_product_step_id_generator",
            sequenceName = "seq_product_step_id_seq", allocationSize = 1)
    @Column(name = "id")
    private Integer id;

    @Column(name = "seq_id", nullable = false)
    private Integer seqId;

    @Column(name = "\"order\"", nullable = false)
    private Integer order;

    @Column(name = "raw_description")
    private String rawDescription;

    @Column(name = "seq_step_operation_id")
    private Integer seqStepOperationId;

    @Column(name = "seq_step_related_operation_id", nullable = false)
    private Integer seqStepRelatedOperationId;
}
