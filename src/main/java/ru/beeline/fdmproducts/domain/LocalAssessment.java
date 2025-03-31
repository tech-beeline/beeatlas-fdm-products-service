package ru.beeline.fdmproducts.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "local_assessment")
public class LocalAssessment {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "assessment_id_generator")
    @SequenceGenerator(name = "assessment_id_generator", sequenceName = "seq_assessment_id", allocationSize = 1)
    @Column(name = "id")
    private Integer id;

    @Column(name = "source_id", nullable = false)
    private Integer sourceId;

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "created_time")
    private LocalDateTime createdTime;
}
