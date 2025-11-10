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
@Table(name = "local_fitness_function")
public class LocalFitnessFunction {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "local_fitness_function_id_generator")
    @SequenceGenerator(name = "local_fitness_function_id_generator", sequenceName = "seq_local_fitness_function_id", allocationSize = 1)
    @Column(name = "id")
    private Integer id;

    @Column(unique = true, nullable = false)
    private String code;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private String status;

    @Column(name = "doc_link")
    private String docLink;
}
