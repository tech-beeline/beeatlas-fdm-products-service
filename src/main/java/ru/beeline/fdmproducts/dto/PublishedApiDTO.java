package ru.beeline.fdmproducts.dto;

import lombok.*;
import ru.beeline.fdmproducts.domain.mapic.ApiMapic;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class PublishedApiDTO {

    private Integer id;
    private String apiContext;
    private String statusName;
    private ApiMapic apiId;
}
