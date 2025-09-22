package ru.beeline.fdmproducts.dto;


import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class ConnectionRequestDTO {
    private Integer mapicInterfaceId;
    private Integer archInterfaceId;
}
