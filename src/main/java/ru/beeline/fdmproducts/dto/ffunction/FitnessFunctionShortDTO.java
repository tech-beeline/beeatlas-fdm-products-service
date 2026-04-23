package ru.beeline.fdmproducts.dto.ffunction;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class FitnessFunctionShortDTO {
    private Integer id;
    private Boolean isCheck;
    private Integer countAll;
    private Integer countSuccess;
}