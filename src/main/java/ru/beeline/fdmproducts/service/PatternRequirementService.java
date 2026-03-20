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
import ru.beeline.fdmproducts.domain.PatternRequirement;
import ru.beeline.fdmproducts.dto.NfrItemDTO;
import ru.beeline.fdmproducts.exception.EntityNotFoundException;
import ru.beeline.fdmproducts.repository.NonFunctionalRequirementEnumRepository;
import ru.beeline.fdmproducts.repository.PatternRequirementRepository;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Transactional
@Service
@Slf4j
public class PatternRequirementService {

    @Autowired
    private PatternRequirementRepository patternRequirementRepository;
    @Autowired
    private NonFunctionalRequirementEnumRepository nonFunctionalRequirementEnumRepository;

    public PatternRequirement addRequirement(Integer patternId, Integer nfrId) {
        NonFunctionalRequirementEnum nfr = nonFunctionalRequirementEnumRepository.findById(nfrId)
                .orElseThrow(() -> new EntityNotFoundException("NFR enum не найден"));

        PatternRequirement requirement = PatternRequirement.builder()
                .patternId(patternId)
                .nfr(nfr)
                .build();
        return patternRequirementRepository.save(requirement);
    }

    public List<PatternRequirement> findByPatternId(Integer patternId) {
        return patternRequirementRepository.findByPatternId(patternId);
    }

    public List<PatternRequirement> findByNfrId(Integer nfrId) {
        return patternRequirementRepository.findByNfrId(nfrId);
    }

    public void deleteById(Integer id) {
        patternRequirementRepository.deleteById(id);
    }

    /**
     * Получить все требования NFR, связанные с паттерном.
     * Дубли по core_id убираются, остаётся только запись с максимальным version.
     */
    public List<NfrItemDTO> getNfrByPatternId(Integer patternId) {
        List<PatternRequirement> requirements = patternRequirementRepository
                .findByPatternIdWithNfrAndCore(patternId);

        if (requirements.isEmpty()) {
            return List.of();
        }

        return requirements.stream()
                .map(PatternRequirement::getNfr)
                .filter(nfr -> nfr != null && nfr.getCore() != null)
                .collect(Collectors.groupingBy(nfr -> nfr.getCore().getId()))
                .values().stream()
                .map(nfrGroup -> nfrGroup.stream()
                        .max(Comparator.comparing(NonFunctionalRequirementEnum::getVersion,
                                Comparator.nullsFirst(Comparator.naturalOrder())))
                        .orElse(null))
                .filter(nfr -> nfr != null)
                .map(this::toNfrItemDTO)
                .collect(Collectors.toList());
    }

    private NfrItemDTO toNfrItemDTO(NonFunctionalRequirementEnum nfr) {
        NonFunctionalRequirementEnumCore core = nfr.getCore();
        return NfrItemDTO.builder()
                .id(String.valueOf(nfr.getId()))
                .code(core != null ? core.getCode() : null)
                .version(nfr.getVersion())
                .name(nfr.getName())
                .description(nfr.getDescription())
                .rule(nfr.getRule())
                .source(core != null ? core.getSource() : null)
                .build();
    }
}
