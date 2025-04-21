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
@Table(
        name = "properties",
        schema = "product",
        uniqueConstraints = @UniqueConstraint(columnNames = {"infra_id", "name"})
)
public class Property {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_properties_id")
    @SequenceGenerator(name = "seq_properties_id", sequenceName = "product.seq_properties_id", allocationSize = 1)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "infra_id", nullable = false)
    private Infra infra;

    @Column(nullable = false)
    private String name;

    private String value;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Column(name = "deleted_date")
    private LocalDateTime deletedDate;

    @Column(name = "last_modified_date")
    private LocalDateTime lastModifiedDate;
}