/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

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
@Table(name = "product_interaction")
public class ProductInteraction {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "product_interaction_id_generator")
    @SequenceGenerator(name = "product_interaction_id_generator",
            sequenceName = "product_interaction_id_seq",
            allocationSize = 1)
    @Column(name = "id")
    private Integer id;

    @Column(name = "provider_alias", nullable = false)
    private String providerAlias;

    @Column(name = "consumer_alias", nullable = false)
    private String consumerAlias;

    @Column(name = "source", nullable = false)
    private String source;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
