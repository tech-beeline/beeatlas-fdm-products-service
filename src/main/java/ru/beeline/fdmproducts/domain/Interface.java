package ru.beeline.fdmproducts.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "interface")
public class Interface {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "interface_id_generator")
    @SequenceGenerator(name = "interface_id_generator", sequenceName = "seq_interface_id", allocationSize = 1)
    @Column(name = "id")
    private Integer id;

    @Column(name = "name")
    private String name;

    @Column(name = "code")
    private String code;

    @Column(name = "spec_link")
    private String specLink;

    @Column(name = "version")
    private String version;

    @Column(name = "type_id")
    private Integer typeId;

    @Column(name = "description")
    private String description;

    @Column(name = "protocol")
    private String protocol;

    @Column(name = "status_id")
    private Integer statusId;

    @Column(name = "tc_id")
    private Integer tcId;

    @Column(name = "container_id")
    private Integer containerId;

    @Column(name = "created_date")
    private LocalDateTime createdDate;

    @Column(name = "deleted_date")
    private LocalDateTime deletedDate;

    @Column(name = "updated_date")
    private LocalDateTime updatedDate;

    @OneToMany(mappedBy = "connectedInterface", fetch = FetchType.LAZY)
    private List<DiscoveredInterface> discoveredInterfaces;

    @OneToMany(mappedBy = "interfaceObj", fetch = FetchType.LAZY)
    private List<Operation> operations;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "container_id", insertable = false, updatable = false)
    private ContainerProduct containerProduct;
}
