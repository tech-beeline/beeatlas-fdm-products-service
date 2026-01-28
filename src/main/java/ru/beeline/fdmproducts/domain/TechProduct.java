/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.domain;
import com.fasterxml.jackson.annotation.JsonBackReference;
import lombok.*;

import javax.persistence.*;
import java.time.LocalDateTime;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "tech_product")
public class TechProduct {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "tech_product_id_generator")
    @SequenceGenerator(name = "tech_product_id_generator", sequenceName = "seq_tech_product_id", allocationSize = 1)
    @Column(name = "id")
    private Integer id;

    @Column(name = "tech_id")
    private Integer techId;

    @JsonBackReference
    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(name = "source")
    private String source;

    @Column(name = "created_date")
    private LocalDateTime createdDate;

    @Column(name = "deleted_date")
    private LocalDateTime deletedDate;

    @Column(name = "last_modified_date")
    private LocalDateTime lastModifiedDate;
}
