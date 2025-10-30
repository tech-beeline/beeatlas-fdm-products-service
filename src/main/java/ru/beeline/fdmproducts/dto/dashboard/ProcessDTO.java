package ru.beeline.fdmproducts.dto.dashboard;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class ProcessDTO {

    private String name;
    private String uid;
    private String href;
}
