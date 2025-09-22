package ru.beeline.fdmproducts.domain;

import com.fasterxml.jackson.annotation.JsonBackReference;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "discovered_parameter")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiscoveredParameter {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "discovered_parameter_generator")
    @SequenceGenerator(name = "discovered_parameter_generator", sequenceName = "discovered_parameter_id_seq", allocationSize = 1)
    private Integer id;

    @JsonBackReference
   @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operation_id")
    private DiscoveredOperation discoveredOperation;

    @Column(name = "parameter_name")
    private String parameterName;

    @Column(name = "parameter_type")
    private String parameterType;

    @Column(name = "created_date")
    private LocalDateTime createdDate;

    @Column(name = "deleted_date")
    private LocalDateTime deletedDate;
}