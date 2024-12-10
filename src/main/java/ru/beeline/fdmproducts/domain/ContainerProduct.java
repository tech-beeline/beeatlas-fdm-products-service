package ru.beeline.fdmproducts.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.Date;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "containers_product")
public class ContainerProduct {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "containers_product_id_generator")
    @SequenceGenerator(name = "containers_product_generator", sequenceName = "seq_containers_product_id", allocationSize = 1)
    @Column(name = "id")
    private Integer id;

    @Column(name = "name")
    private String name;

    @Column(name = "product_id")
    private Integer productId;

    @Column(name = "code")
    private String code;

    @Column(name = "version")
    private String version;

    @Column(name = "created_date")
    private Date createdDate;

    @Column(name = "updated_date ")
    private Date updatedDate;

    @Column(name = "deleted_date")
    private Date deletedDate;
}
