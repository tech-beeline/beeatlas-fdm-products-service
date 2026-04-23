/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.beeline.fdmproducts.client.TechradarClient;
import ru.beeline.fdmproducts.domain.NonFunctionalRequirementEnum;
import ru.beeline.fdmproducts.domain.NonFunctionalRequirementEnumCore;
import ru.beeline.fdmproducts.domain.PatternRequirement;
import ru.beeline.fdmproducts.dto.ProductAvailableDTO;
import ru.beeline.fdmproducts.dto.nfr.NfrItemDTO;
import ru.beeline.fdmproducts.exception.EntityNotFoundException;
import ru.beeline.fdmproducts.repository.NonFunctionalRequirementEnumRepository;
import ru.beeline.fdmproducts.repository.PatternRequirementRepository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Transactional
@Service
@Slf4j
public class PatternRequirementService {

    @Autowired
    private PatternRequirementRepository patternRequirementRepository;
    @Autowired
    private NonFunctionalRequirementEnumRepository nonFunctionalRequirementEnumRepository;
    @Autowired
    private TechradarClient techradarClient;

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
                .id(nfr.getId())
                .code(core != null ? core.getCode() : null)
                .version(nfr.getVersion())
                .name(nfr.getName())
                .description(nfr.getDescription())
                .rule(nfr.getRule())
                .source(core != null ? core.getSource() : null)
                .build();
    }

    public void linkPatternWithNfr(Integer patternId, List<Integer> nfrIds, boolean refreshRelation) {
        if (!refreshRelation) {
            if (nfrIds == null || nfrIds.isEmpty()) {
                return;
            }
        }
        validatePatternExistsInTechradar(patternId);
        if (refreshRelation) {
            List<PatternRequirement> existing = patternRequirementRepository.findByPatternId(patternId);
            if (!existing.isEmpty()) {
                patternRequirementRepository.deleteAll(existing);
            }
            if (nfrIds != null && !nfrIds.isEmpty()) {
                createPatternRequirements(patternId, nfrIds);
            }
        } else {
            if (!nfrIds.isEmpty()) {
                createPatternRequirements(patternId, nfrIds);
            }
        }
    }

    private void createPatternRequirements(Integer patternId, List<Integer> nfrIds) {
        List<Integer> distinctIds = nfrIds.stream().distinct().filter(Objects::nonNull).toList();
        List<NonFunctionalRequirementEnum> nfrList = validateNfrIdsExist(distinctIds);
        Map<Integer, NonFunctionalRequirementEnum> nfrMap = nfrList.stream()
                .collect(Collectors.toMap(NonFunctionalRequirementEnum::getId, Function.identity()));
        for (Integer nfrId : distinctIds) {
            boolean exists = patternRequirementRepository.findByPatternIdAndNfrId(patternId, nfrId).isPresent();
            if (exists) {
                continue;
            }
            PatternRequirement toCreate = PatternRequirement.builder()
                    .patternId(patternId)
                    .nfr(nfrMap.get(nfrId))
                    .build();
            patternRequirementRepository.save(toCreate);
        }
    }

    private void validatePatternExistsInTechradar(Integer patternId) {
        ProductAvailableDTO response = techradarClient.checkPatternsAvailability(List.of(patternId));
        if (response == null || Boolean.FALSE.equals(response.getAvailability())) {
            throw new IllegalArgumentException("Идентификатор не соответсвует существующему паттерну");
        }
    }

    private List<NonFunctionalRequirementEnum> validateNfrIdsExist(List<Integer> nfrIdsDistinct) {
        List<NonFunctionalRequirementEnum> enums = nonFunctionalRequirementEnumRepository.findAllById(nfrIdsDistinct);
        if (enums.size() != nfrIdsDistinct.size()) {
            throw new IllegalArgumentException("Не все переданные идентификаторы соответствуют существующим требованиям");
        }
        return enums;
    }
}
