/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.dto.dashboard;


import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class GetInfoProcessDTO {

    private String name;
    private String uid;
    private String stereotype;
    private String rps;
    private String latency;
    private String error_rate;
    private String operation_guid;
    private String diagram_uid;
    private Integer seqno;
    private String client_name;
    private String client_code;
    private String server_name;
    private String is_ret;
    private String linked_diagram_uid;
}
