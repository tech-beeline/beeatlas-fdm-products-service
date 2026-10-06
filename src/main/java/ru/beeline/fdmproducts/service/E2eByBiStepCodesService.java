/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.beeline.fdmproducts.domain.E2e;
import ru.beeline.fdmproducts.dto.e2e.E2eByBiStepCodeItemDTO;
import ru.beeline.fdmproducts.dto.e2e.E2eByBiStepCodesGroupDTO;
import ru.beeline.fdmproducts.repository.E2eRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class E2eByBiStepCodesService {

    private static final String MISSING_BODY = "Отсутствует тело запроса";
    private static final String EMPTY_CODES = "Список кодов bi-step не может быть пустым";
    private static final String INVALID_CODE = "Код bi-step не может быть пустым";

    private final E2eRepository e2eRepository;

    @Transactional(readOnly = true)
    public List<E2eByBiStepCodesGroupDTO> getByBiStepCodes(List<String> biStepCodes) {
        List<String> uniqueCodes = uniqueCodes(parseCodes(biStepCodes));
        log.info("Чтение e2e по bi_step_code: уникальных кодов={}", uniqueCodes.size());

        List<String> lowerCodes = uniqueCodes.stream()
                .map(code -> code.toLowerCase(Locale.ROOT))
                .distinct()
                .toList();
        List<E2e> found = e2eRepository.findAllByBiStepCodeInIgnoreCase(lowerCodes);
        Map<String, List<E2e>> byLowerBiStepCode = found.stream()
                .collect(Collectors.groupingBy(e2e -> e2e.getBiStepCode().toLowerCase(Locale.ROOT)));

        List<E2eByBiStepCodesGroupDTO> result = new ArrayList<>(uniqueCodes.size());
        for (String code : uniqueCodes) {
            List<E2e> group = byLowerBiStepCode.getOrDefault(code.toLowerCase(Locale.ROOT), List.of());
            List<E2eByBiStepCodeItemDTO> items = group.stream()
                    .sorted(Comparator.comparing(E2e::getId))
                    .map(this::toItem)
                    .toList();
            result.add(E2eByBiStepCodesGroupDTO.builder()
                    .biStepCode(code)
                    .e2e(items)
                    .build());
        }
        log.info("Чтение e2e по bi_step_code завершено: групп={}, найденных e2e={}",
                result.size(), found.size());
        return result;
    }

    private List<String> parseCodes(List<String> biStepCodes) {
        if (biStepCodes == null) {
            throw new IllegalArgumentException(MISSING_BODY);
        }
        if (biStepCodes.isEmpty()) {
            throw new IllegalArgumentException(EMPTY_CODES);
        }
        List<String> codes = new ArrayList<>(biStepCodes.size());
        for (String item : biStepCodes) {
            if (item == null) {
                throw new IllegalArgumentException(INVALID_CODE);
            }
            String trimmed = item.trim();
            if (trimmed.isEmpty()) {
                throw new IllegalArgumentException(INVALID_CODE);
            }
            codes.add(trimmed);
        }
        return codes;
    }

    private List<String> uniqueCodes(List<String> codes) {
        return new ArrayList<>(new LinkedHashSet<>(codes));
    }

    private E2eByBiStepCodeItemDTO toItem(E2e e2e) {
        return E2eByBiStepCodeItemDTO.builder()
                .id(e2e.getId())
                .name(e2e.getName())
                .code(e2e.getCode())
                .source(e2e.getSource())
                .build();
    }
}
