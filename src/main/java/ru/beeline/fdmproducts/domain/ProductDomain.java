/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.domain;

import lombok.*;
import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "product_domain", schema = "product")
@ToString
public class ProductDomain {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "product_domain_id_generator")
    @SequenceGenerator(name = "product_domain_id_generator", sequenceName = "seq_product_domain_id", allocationSize = 1)
    @Column(name = "id")
    private Integer id;

    @Column(name = "name", nullable = false, length = 250)
    private String name;

    @Column(name = "alias", nullable = false, length = 255)
    private String alias;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "owner_id")
    private Integer ownerId;

    @OneToMany(mappedBy = "domain")
    @ToString.Exclude
    private List<Product> products = new ArrayList<>();
}