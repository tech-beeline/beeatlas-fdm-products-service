/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@ToString(exclude = {"infra"})
@Table(
        name = "infra_product",
        schema = "product",
        uniqueConstraints = @UniqueConstraint(columnNames = {"product_id", "infra_id"})
)
public class InfraProduct implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_infra_product_id")
    @SequenceGenerator(name = "seq_infra_product_id", sequenceName = "product.seq_infra_product_id", allocationSize = 1)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    @JsonIgnore
    private Product product;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "infra_id", nullable = false)
    @JsonIgnore
    private Infra infra;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Column(name = "deleted_date")
    private LocalDateTime deletedDate;

    @Column(name = "last_modified_date")
    private LocalDateTime lastModifiedDate;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InfraProduct that = (InfraProduct) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}


