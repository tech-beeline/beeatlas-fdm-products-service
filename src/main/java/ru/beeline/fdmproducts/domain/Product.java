package ru.beeline.fdmproducts.domain;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import lombok.*;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "product")
@ToString
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "product_id_generator")
    @SequenceGenerator(name = "product_id_generator", sequenceName = "seq_product_id", allocationSize = 1)
    @Column(name = "id")
    private Integer id;

    @Column(name = "name")
    private String name;

    @Column(name = "alias")
    private String alias;

    @Column(name = "description")
    private String description;

    @Column(name = "git_url")
    private String gitUrl;

    @Column(name = "structurizr_workspace_name")
    private String structurizrWorkspaceName;

    @Column(name = "structurizr_api_key")
    private String structurizrApiKey;

    @Column(name = "structurizr_api_secret")
    private String structurizrApiSecret;

    @Column(name = "structurizr_api_url")
    private String structurizrApiUrl;

    @Column(name = "source")
    private String source;

    @Column(name = "upload_date ")
    private LocalDateTime uploadDate ;

    @JsonManagedReference
    @OneToMany(mappedBy = "product")
    private List<TechProduct> techProducts;

    @ToString.Exclude
    @OneToMany(mappedBy = "product")
    @JsonManagedReference
    private List<DiscoveredInterface> discoveredInterfaces = new ArrayList<>();
}
