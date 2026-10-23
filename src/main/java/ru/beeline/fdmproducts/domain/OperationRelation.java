/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

/**
 * Шаг последовательности вызовов e2e (SFDM-4244): operation_id и related_operation_id — id
 * product.discovered_operation. operation_id пустой у корня. Колонок типа у связи нет. Связь принадлежит
 * ветке e2e (SFDM-4250).
 */
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "operation_relations")
public class OperationRelation {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "operation_relations_id_generator")
    @SequenceGenerator(name = "operation_relations_id_generator", sequenceName = "operation_relations_id_seq", allocationSize = 1)
    @Column(name = "id")
    private Integer id;

    @Column(name = "operation_id")
    private Integer operationId;

    @Column(name = "related_operation_id", nullable = false)
    private Integer relatedOperationId;

    @Column(name = "\"order\"", nullable = false)
    private Integer order;

    @Column(name = "stereo_type")
    private String stereoType;

    /** Ветка e2e (product.e2e_branch.id), SFDM-4250. Карточка — через e2e_branch.code. */
    @Column(name = "e2e_branch_id", nullable = false)
    private Integer e2eBranchId;
}
