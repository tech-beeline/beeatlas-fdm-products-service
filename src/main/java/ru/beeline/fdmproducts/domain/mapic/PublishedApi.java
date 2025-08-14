package ru.beeline.fdmproducts.domain.mapic;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "published_api", schema = "mapic")
public class PublishedApi {

    @Id
    @Column(name = "id", nullable = false)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "api_id")
    private ApiMapic apiId;

    @Column(name = "status")
    private String status;

    @Column(name = "context")
    private String context;

    @Column(name = "spec")
    private String spec;
}
