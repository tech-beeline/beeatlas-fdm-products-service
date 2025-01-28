package ru.beeline.fdmproducts.domain;
import com.fasterxml.jackson.annotation.JsonBackReference;
import lombok.*;

import javax.persistence.*;

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
}
