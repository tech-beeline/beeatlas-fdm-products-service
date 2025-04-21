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
        name = "relations",
        schema = "product",
        uniqueConstraints = @UniqueConstraint(columnNames = {"parent_id", "child_id"})
)
public class Relation {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_relations_id")
    @SequenceGenerator(name = "seq_relations_id", sequenceName = "product.seq_relations_id", allocationSize = 1)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", referencedColumnName = "cmdb_id", nullable = false)
    private Infra parent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "child_id", referencedColumnName = "cmdb_id", nullable = false)
    private Infra child;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Column(name = "deleted_date")
    private LocalDateTime deletedDate;

    @Column(name = "last_modified_date")
    private LocalDateTime lastModifiedDate;
}