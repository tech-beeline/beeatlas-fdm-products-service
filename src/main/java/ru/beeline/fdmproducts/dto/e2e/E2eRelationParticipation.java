/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.dto.e2e;

import ru.beeline.fdmproducts.domain.OperationRelation;

/** Связь ветки e2e и id карточки e2e, которой принадлежит ветка (SFDM-4250). */
public record E2eRelationParticipation(OperationRelation relation, Integer e2eId) {
}
