/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import ru.beeline.fdmproducts.domain.Product;
import ru.beeline.fdmproducts.dto.ErrorMessageDTO;
import ru.beeline.fdmproducts.repository.NonFunctionalRequirementRepository;
import ru.beeline.fdmproducts.repository.PatternRequirementRepository;

import java.util.List;
import java.util.Optional;

@Service
public class PatternProductService {

    @Autowired
    private NonFunctionalRequirementService nonFunctionalRequirementService;
    @Autowired
    private NonFunctionalRequirementRepository nonFunctionalRequirementRepository;
    @Autowired
    private PatternRequirementRepository patternRequirementRepository;

    public ResponseEntity<?> getPatternIdsByProduct(Integer id, String alias, String apiKey) {
        boolean hasId = id != null;
        boolean hasAlias = StringUtils.hasText(alias);
        boolean hasApiKey = StringUtils.hasText(apiKey);

        int passed = (hasId ? 1 : 0) + (hasAlias ? 1 : 0) + (hasApiKey ? 1 : 0);
        if (passed == 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorMessageDTO("Не передан один из идентификаторов приложения: id/alias/api-key"));
        }
        if (passed > 1) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorMessageDTO("Передано несколько идентификаторов приложения"));
        }

        Optional<Product> productOpt = nonFunctionalRequirementService.findProductByIdOrAliasOrApiKey(id, alias, apiKey);
        if (productOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorMessageDTO("Продукт с указанным идентификатором не найден"));
        }

        List<Integer> nfrIds = nonFunctionalRequirementRepository.findDistinctNfrEnumIdsByProductId(productOpt.get().getId());
        if (nfrIds.isEmpty()) {
            return ResponseEntity.ok(List.of());
        }

        List<Integer> patternIds = patternRequirementRepository.findPatternIdsWhereAllRequirementsIn(nfrIds);
        return ResponseEntity.ok(patternIds);
    }
}

