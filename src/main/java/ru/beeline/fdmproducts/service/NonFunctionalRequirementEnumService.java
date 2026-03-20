/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.beeline.fdmproducts.domain.NonFunctionalRequirementEnum;
import ru.beeline.fdmproducts.domain.NonFunctionalRequirementEnumCore;
import ru.beeline.fdmproducts.exception.EntityNotFoundException;
import ru.beeline.fdmproducts.repository.NonFunctionalRequirementEnumCoreRepository;
import ru.beeline.fdmproducts.repository.NonFunctionalRequirementEnumRepository;

import java.util.List;

@Transactional
@Service
@Slf4j
public class NonFunctionalRequirementEnumService {

    @Autowired
    private NonFunctionalRequirementEnumRepository nonFunctionalRequirementEnumRepository;
    @Autowired
    private NonFunctionalRequirementEnumCoreRepository nonFunctionalRequirementEnumCoreRepository;

    public List<NonFunctionalRequirementEnum> findAll() {
        return nonFunctionalRequirementEnumRepository.findAll();
    }

    public List<NonFunctionalRequirementEnum> findByCoreId(Integer coreId) {
        return nonFunctionalRequirementEnumRepository.findByCoreId(coreId);
    }

    public NonFunctionalRequirementEnum findById(Integer id) {
        return nonFunctionalRequirementEnumRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("NFR enum не найден"));
    }

    public NonFunctionalRequirementEnum save(NonFunctionalRequirementEnum nfrEnum) {
        return nonFunctionalRequirementEnumRepository.save(nfrEnum);
    }

    public NonFunctionalRequirementEnumCore findCoreByCode(String code) {
        return nonFunctionalRequirementEnumCoreRepository.findByCode(code)
                .orElseThrow(() -> new EntityNotFoundException("NFR enum core не найден по коду: " + code));
    }
}
