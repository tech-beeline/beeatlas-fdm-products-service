package ru.beeline.fdmproducts.domain.mapic;

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
@Table(name = "products", schema = "mapic")
public class ProductMapic {

    @Id
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column(name = "cmdb")
    private String cmdb;

    @Column(name = "load_date")
    private LocalDateTime loadDate;
}
