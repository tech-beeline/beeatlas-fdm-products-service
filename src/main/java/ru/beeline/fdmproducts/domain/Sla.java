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
@Table(name = "sla")
public class Sla {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sla_id_generator")
    @SequenceGenerator(name = "sla_id_generator", sequenceName = "seq_sla_id", allocationSize = 1)
    @Column(name = "id")
    private Integer id;

    @Column(name = "operation_id")
    private Integer operationId;

    @Column(name = "rps")
    private Integer rps;

    @Column(name = "latency")
    private Integer latency;

    @Column(name = "error_rate")
    private Integer errorRate;
}
