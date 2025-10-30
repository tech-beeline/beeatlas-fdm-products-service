package ru.beeline.fdmproducts.dto.dashboard;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class MessageDTO {

    private String name;
    private OperationE2eDTO operation;
}
