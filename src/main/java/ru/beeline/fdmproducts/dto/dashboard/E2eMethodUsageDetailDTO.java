/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class E2eMethodUsageDetailDTO {

    private String bi_name;
    private String bi_uid;
    private String e2e_name;
    private String e2e_uid;
    private List<E2eMethodUsageClientDTO> clients;
}
