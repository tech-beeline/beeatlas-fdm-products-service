package ru.beeline.fdmproducts.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class GetUserProfileDTO {

    private Integer id;
    private String fullName;
    private String email;
    private String login;
}
