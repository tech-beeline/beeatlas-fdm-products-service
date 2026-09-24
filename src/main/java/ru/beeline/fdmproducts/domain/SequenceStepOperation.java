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
 * Снимок атрибутов метода на шаге sequence (product.sequence_step_operation), SFDM-4080.
 * {@code operationId} — результат резолва по C4-каталогу ветки; NULL, если операция не найдена
 * или найдена неоднозначно (не ошибка, атрибуты всё равно сохраняются).
 */
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "sequence_step_operation")
public class SequenceStepOperation {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequence_step_operation_id_generator")
    @SequenceGenerator(name = "sequence_step_operation_id_generator",
            sequenceName = "sequence_step_operation_id_seq", allocationSize = 1)
    @Column(name = "id")
    private Integer id;

    @Column(name = "operation_product_alias")
    private String operationProductAlias;

    @Column(name = "operation_container_code")
    private String operationContainerCode;

    @Column(name = "operation_interface_code")
    private String operationInterfaceCode;

    @Column(name = "operation_name")
    private String operationName;

    @Column(name = "operation_type")
    private String operationType;

    @Column(name = "operation_id")
    private Integer operationId;
}
