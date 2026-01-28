/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.Date;
import java.util.List;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "containers_product")
public class ContainerProduct {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "containers_product_generator")
    @SequenceGenerator(name = "containers_product_generator", sequenceName = "seq_container_id", allocationSize = 1)
    @Column(name = "id")
    private Integer id;

    @Column(name = "name")
    private String name;

    @Column(name = "product_id")
    private Integer productId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", insertable = false, updatable = false)
    private Product product;

    @Column(name = "code")
    private String code;

    @Column(name = "version")
    private String version;

    @Column(name = "created_date")
    private Date createdDate;

    @Column(name = "updated_date")
    private Date updatedDate;

    @Column(name = "deleted_date")
    private Date deletedDate;

    @OneToMany(mappedBy = "containerProduct", fetch = FetchType.LAZY)
    private List<Interface> interfaces;
}
