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
@Table(name = "local_ac_object_detail")
public class LocalAcObjectDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "local_ac_object_detail_id_generator")
    @SequenceGenerator(name = "local_ac_object_detail_id_generator", sequenceName = "seq_local_ac_object_detail_id", allocationSize = 1)
    @Column(name = "id")
    private Integer id;

    @Column(name = "laco_id")
    private Integer lacoId;

    @Column(name = "key")
    private String key;

    @Column(name = "value")
    private String value;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "laco_id", insertable = false, updatable = false)
    private LocalAcObject localAcObject;
}
