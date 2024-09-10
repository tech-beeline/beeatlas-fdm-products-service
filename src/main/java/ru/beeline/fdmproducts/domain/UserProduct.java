package ru.beeline.fdmproducts.domain;

import lombok.*;

import javax.persistence.*;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "user_product")
public class UserProduct {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_product_id_generator")
    @SequenceGenerator(name = "user_product_id_generator", sequenceName = "seq_user_product_id", allocationSize = 1)
    @Column(name = "id")
    private Integer id;

    @Column(name = "user_id")
    private Integer userId;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;
}
