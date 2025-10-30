package ru.beeline.fdmproducts.dto.dashboard;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class E2eProcessInfoDTO {

    private ProcessDTO process;
    private BiDTO bi;
    private MessageDTO message;
    private SystemDTO system;
}
