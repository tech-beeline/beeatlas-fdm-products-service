package ru.beeline.fdmproducts.dto.dashboard;


import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class SystemDTO {

    private String name;
    private String code;
    private String href;
}
