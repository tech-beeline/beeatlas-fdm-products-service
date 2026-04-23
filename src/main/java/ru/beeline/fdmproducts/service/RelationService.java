/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.beeline.fdmproducts.domain.Infra;
import ru.beeline.fdmproducts.domain.Relation;
import ru.beeline.fdmproducts.exception.EntityNotFoundException;
import ru.beeline.fdmproducts.repository.InfraRepository;
import ru.beeline.fdmproducts.repository.RelationRepository;

import java.time.LocalDateTime;

@Transactional
@Service
@Slf4j
public class RelationService {
    @Autowired
    private InfraRepository infraRepository;
    @Autowired
    private RelationRepository relationRepository;

    public Relation createRelation(String parentCmdbId, String childCmdbId) {

        Relation relation = Relation.builder()
                .parentId(parentCmdbId)
                .childId(childCmdbId)
                .createdDate(LocalDateTime.now())
                .build();

        return relationRepository.save(relation);
    }
}
