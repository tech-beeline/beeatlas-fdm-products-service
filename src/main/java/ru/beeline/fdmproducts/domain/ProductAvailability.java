/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.domain;

import lombok.*;

import javax.persistence.*;
import java.time.LocalDateTime;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "product_availability", schema = "product")
@ToString
public class ProductAvailability {

    @Id
    @Column(name = "id")
    private Integer id;

    @Column(name = "product_id", insertable = false, updatable = false)
    private Integer productId;

    @Column(name = "availability")
    private Boolean availability;

    @Column(name = "created_date")
    private LocalDateTime createdDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    @ToString.Exclude
    private Product product;
}
