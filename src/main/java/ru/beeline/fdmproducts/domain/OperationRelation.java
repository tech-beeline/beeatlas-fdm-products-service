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

    @Column(name = "e2e_id", nullable = false)
    private Integer e2eId;

    @Builder.Default
    @Column(name = "entity_type_operation", nullable = false)
    private String entityTypeOperation = "operation";

    @Builder.Default
    @Column(name = "entity_type_operation_relation", nullable = false)
    private String entityTypeOperationRelation = "operation";
}
