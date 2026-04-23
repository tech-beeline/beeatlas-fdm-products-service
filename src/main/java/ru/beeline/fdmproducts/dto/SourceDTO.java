/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.dto;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class SourceDTO {

    @JsonProperty("source_type")
    private String sourceType;

    @JsonProperty("source_id")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer sourceId;
}
