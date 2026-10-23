/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.dto.e2e;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class E2eUpsertResponseDTO {

    private Integer id;
    private String code;
    /** product.e2e_branch.id ветки, в которую шла запись (SFDM-4250). */
    private Integer e2eBranchId;
    /** branch_name ветки, как записан. */
    private String branch;
}
