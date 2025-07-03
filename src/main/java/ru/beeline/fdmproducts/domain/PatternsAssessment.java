package ru.beeline.fdmproducts.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "patterns_assessment", schema = "product")
public class PatternsAssessment {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "patterns_assessment_seq")
    @SequenceGenerator(name = "patterns_assessment_seq", sequenceName = "patterns_assessment_id_seq", allocationSize = 1)
    private Integer id;

    @Column(name = "product_id")
    private Integer productId;

    @Column(name = "source_type_id")
    private Integer sourceTypeId;

    @Column(name = "source_id")
    private Integer sourceId;

    @Column(name = "create_date")
    private LocalDateTime createDate;

    @OneToMany(mappedBy = "assessment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PatternsCheck> checks;
}