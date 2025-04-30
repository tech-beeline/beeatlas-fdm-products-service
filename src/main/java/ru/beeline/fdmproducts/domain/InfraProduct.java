package ru.beeline.fdmproducts.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
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
    @Column(name = "product_id", nullable = false)
    @JsonIgnore
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @Column(name = "infra_id", nullable = false)
    @JsonIgnore
    private Infra infra;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Column(name = "deleted_date")
    private LocalDateTime deletedDate;

    @Column(name = "last_modified_date")
    private LocalDateTime lastModifiedDate;
}
