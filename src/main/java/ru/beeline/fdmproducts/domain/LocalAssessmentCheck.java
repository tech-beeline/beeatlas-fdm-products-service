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
@Table(name = "local_assessment_check")
public class LocalAssessmentCheck {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "assessment_check_id_generator")
    @SequenceGenerator(name = "assessment_check_id_generator", sequenceName = "seq_assessment_check_id", allocationSize = 1)
    @Column(name = "id")
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "lff_id", nullable = false)
    private LocalFitnessFunction fitnessFunction;

    @ManyToOne
    @JoinColumn(name = "assessment_id", nullable = false)
    private LocalAssessment assessment;

    @Column(name = "is_check", nullable = false)
    private Boolean isCheck;

    @Column(name = "result_details")
    private String resultDetails;
}
