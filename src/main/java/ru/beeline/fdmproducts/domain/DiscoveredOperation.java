package ru.beeline.fdmproducts.domain;

import com.fasterxml.jackson.annotation.JsonBackReference;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "discovered_operation")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiscoveredOperation {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "discovered_operation_generator")
    @SequenceGenerator(name = "discovered_operation_generator", sequenceName = "discovered_operation_id_seq", allocationSize = 1)
    private Integer id;

    @JsonBackReference
    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interface_id" )
    private DiscoveredInterface discoveredInterface;

    @Column(name = "interface_id", insertable = false, updatable = false)
    private Integer interfaceId;

    @Column(name = "connection_operation_id")
    private Integer connectionOperationId;

    private String name;

    private String context;

    private String description;

    private String type;

    @Column(name = "return_type")
    private String returnType;

    @Column(name = "created_date")
    private LocalDateTime createdDate;

    @Column(name = "updated_date")
    private LocalDateTime updatedDate;

    @Column(name = "deleted_date")
    private LocalDateTime deletedDate;

    @OneToMany(mappedBy = "discoveredOperation")
    private List<DiscoveredParameter> parameters;
}
