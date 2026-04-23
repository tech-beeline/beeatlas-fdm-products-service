/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.List;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "local_ac_object")
public class LocalAcObject {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "local_ac_object_id_generator")
    @SequenceGenerator(name = "local_ac_object_id_generator", sequenceName = "seq_local_ac_object_id", allocationSize = 1)
    @Column(name = "id")
    private Integer id;

    @Column(name = "lac_id")
    private Integer lacId;

    @Column(name = "is_check")
    private Boolean isCheck;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lac_id", insertable = false, updatable = false)
    private LocalAssessmentCheck localAssessmentCheck;

    @OneToMany(mappedBy = "localAcObject", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<LocalAcObjectDetail> details;
}