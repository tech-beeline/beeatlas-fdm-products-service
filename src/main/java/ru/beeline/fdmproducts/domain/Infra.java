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
        name = "infra",
        schema = "product",
        indexes = {
                @Index(name = "idx_infra_product_id", columnList = "product_id"),
                @Index(name = "idx_infra_type", columnList = "type"),
                @Index(name = "idx_infra_cmdb_id", columnList = "cmdb_id", unique = true)
        }
)
public class Infra {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_infra_id")
    @SequenceGenerator(name = "seq_infra_id", sequenceName = "product.seq_infra_id", allocationSize = 1)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

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
}