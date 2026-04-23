package ru.beeline.fdmproducts.dto.ffunction;


import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class FitnessFunctionNfrDTO {

    private Integer id;
    private String code;
    private String description;
    private String docLink;
}