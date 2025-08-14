package ru.beeline.fdmproducts.domain.mapic;

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
@Table(name = "api", schema = "mapic")
public class ApiMapic {

    @Id
    @Column(name = "id", nullable = false)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "capability_id")
    private CapabilityMapic capabilityId;

    @Column(name = "status")
    private String status;

    @Column(name = "context")
    private String context;

    @Column(name = "spec")
    private String spec;
}
