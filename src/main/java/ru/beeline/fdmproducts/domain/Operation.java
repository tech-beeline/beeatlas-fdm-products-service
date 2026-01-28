/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;

import javax.persistence.*;
import java.time.LocalDateTime;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@ToString(exclude = {"interfaceObj"})
@Table(name = "operation")
public class Operation {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "operation_id_generator")
    @SequenceGenerator(name = "operation_id_generator", sequenceName = "seq_operation_id", allocationSize = 1)
    @Column(name = "id")
    private Integer id;

    @Column(name = "interface_id")
    private Integer interfaceId;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interface_id", insertable = false, updatable = false)
    private Interface interfaceObj;

    @Column(name = "name")
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "type")
    private String type;

    @Column(name = "return_type")
    private String returnType;

    @Column(name = "created_date")
    private LocalDateTime createdDate;

    @Column(name = "updated_date")
    private LocalDateTime updatedDate;

    @Column(name = "deleted_date")
    private LocalDateTime deletedDate;

    @Column(name = "tc_id")
    private Integer tcId;
}
