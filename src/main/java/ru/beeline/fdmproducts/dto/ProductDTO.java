/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
@JsonPropertyOrder({"productId", "alias", "tech"})
public class ProductDTO {

    @JsonProperty("product_id")
    private Integer productId;
    private String alias;
    private List<TechDTO> tech;
}
