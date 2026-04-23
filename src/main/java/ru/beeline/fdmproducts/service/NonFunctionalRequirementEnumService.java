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
import ru.beeline.fdmproducts.dto.nfr.NfrItemPublicDTO;
import ru.beeline.fdmproducts.exception.EntityNotFoundException;
import ru.beeline.fdmproducts.repository.NonFunctionalRequirementEnumCoreRepository;
import ru.beeline.fdmproducts.repository.NonFunctionalRequirementEnumRepository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

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

    public List<NfrItemPublicDTO> getAllActualNfr() {
        List<NonFunctionalRequirementEnum> all = nonFunctionalRequirementEnumRepository.findAll();
        if (all.isEmpty()) {
            return List.of();
        }

        Map<Integer, NonFunctionalRequirementEnum> latestByCoreId = all.stream()
                .filter(nfr -> nfr != null && nfr.getCoreId() != null)
                .collect(Collectors.toMap(
                        NonFunctionalRequirementEnum::getCoreId,
                        Function.identity(),
                        (a, b) -> Comparator
                                .comparing(NonFunctionalRequirementEnum::getVersion, Comparator.nullsFirst(Comparator.naturalOrder()))
                                .compare(a, b) >= 0 ? a : b
                ));

        List<NonFunctionalRequirementEnum> latest = latestByCoreId.values().stream()
                .filter(Objects::nonNull)
                .toList();

        List<Integer> coreIds = latest.stream()
                .map(NonFunctionalRequirementEnum::getCoreId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Integer, NonFunctionalRequirementEnumCore> coresById = nonFunctionalRequirementEnumCoreRepository
                .findAllById(coreIds)
                .stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(NonFunctionalRequirementEnumCore::getId, Function.identity()));

        return latest.stream()
                .map(nfr -> {
                    NonFunctionalRequirementEnumCore core = nfr.getCoreId() != null ? coresById.get(nfr.getCoreId()) : null;
                    return NfrItemPublicDTO.builder()
                            .id(nfr.getId())
                            .code(core != null ? core.getCode() : null)
                            .version(nfr.getVersion())
                            .name(nfr.getName())
                            .description(nfr.getDescription())
                            .rule(nfr.getRule())
                            .source(core != null ? core.getSource() : null)
                            .build();
                })
                .collect(Collectors.toList());
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
