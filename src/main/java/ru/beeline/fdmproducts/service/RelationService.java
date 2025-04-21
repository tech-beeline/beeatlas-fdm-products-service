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
        Infra parent = infraRepository.findByCmdbId(parentCmdbId)
                .orElseThrow(() -> new EntityNotFoundException("Parent infra not found"));

        Infra child = infraRepository.findByCmdbId(childCmdbId)
                .orElseThrow(() -> new EntityNotFoundException("Child infra not found"));

        Relation relation = Relation.builder()
                .parent(parent)
                .child(child)
                .createdDate(LocalDateTime.now())
                .build();

        return relationRepository.save(relation);
    }
}
