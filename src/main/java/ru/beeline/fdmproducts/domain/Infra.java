/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.domain;

import lombok.*;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(
        name = "infra",
        schema = "product",
        indexes = {
                @Index(name = "idx_infra_type", columnList = "type"),
                @Index(name = "idx_infra_cmdb_id", columnList = "cmdb_id", unique = true)
        },
        uniqueConstraints = @UniqueConstraint(columnNames = {"cmdb_id"})
)
public class Infra implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_infra_id")
    @SequenceGenerator(name = "seq_infra_id", sequenceName = "product.seq_infra_id", allocationSize = 1)
    private Integer id;

    @Column(nullable = false)
    private String name;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Column(name = "deleted_date")
    private LocalDateTime deletedDate;

    @Column(name = "last_modified_date")
    private LocalDateTime lastModifiedDate;

    private String type;

    @Column(name = "cmdb_id", unique = true)
    private String cmdbId;

    @OneToMany(mappedBy = "infra", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<InfraProduct> infraProducts = new HashSet<>();

    @OneToMany(mappedBy = "infra", orphanRemoval = true)
    private Set<Property> properties = new HashSet<>();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Infra infra = (Infra) o;
        return Objects.equals(id, infra.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}