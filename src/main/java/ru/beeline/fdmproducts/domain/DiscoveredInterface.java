package ru.beeline.fdmproducts.domain;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "discovered_interface")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiscoveredInterface {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "discovered_interface_generator")
    @SequenceGenerator(name = "discovered_interface_generator", sequenceName = "discovered_interface_id_seq", allocationSize = 1)
    private Integer id;

    private String name;

    @Column(name = "external_id")
    private Integer externalId;

    @Column(name = "api_id")
    private Integer apiId;

    @Column(name = "connection_interface_id")
    private Integer connectionInterfaceId;

    @Column(name = "api_link")
    private String apiLink;

    private String version;

    private String description;

    private String status;

    private String context;

    @JsonBackReference
    @ToString.Exclude
    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(name = "created_date")
    private LocalDateTime createdDate;

    @Column(name = "deleted_date")
    private LocalDateTime deletedDate;

    @Column(name = "updated_date")
    private LocalDateTime updatedDate;

    @JsonManagedReference
    @ToString.Exclude
    @OneToMany(mappedBy = "discoveredInterface")
    private List<DiscoveredOperation> operations;
}
