package ru.beeline.fdmproducts.dto;

import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class LacCountsDTO {
    private Integer lacId;
    private Integer countAll;
    private Integer countSuccess;

    public static LacCountsDTO empty() {
        return new LacCountsDTO(null, 0, 0);
    }}