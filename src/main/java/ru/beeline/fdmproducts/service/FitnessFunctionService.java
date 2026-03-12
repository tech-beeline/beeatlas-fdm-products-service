/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.beeline.fdmproducts.domain.LocalAssessment;
import ru.beeline.fdmproducts.domain.LocalAssessmentCheck;
import ru.beeline.fdmproducts.domain.LocalFitnessFunction;
import ru.beeline.fdmproducts.domain.Product;
import ru.beeline.fdmproducts.dto.*;
import ru.beeline.fdmproducts.repository.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Transactional
@Service
@Slf4j
public class FitnessFunctionService {
    private final LocalFitnessFunctionRepository localFitnessFunctionRepository;
    private final ProductRepository productRepository;
    private final LocalAssessmentRepository localAssessmentRepository;
    private final LocalAcObjectRepository localAcObjectRepository;

    public FitnessFunctionService(LocalFitnessFunctionRepository localFitnessFunctionRepository,
                                  ProductRepository productRepository,
                                  LocalAssessmentRepository localAssessmentRepository,
                                  LocalAcObjectRepository localAcObjectRepository) {
        this.localFitnessFunctionRepository = localFitnessFunctionRepository;
        this.productRepository = productRepository;
        this.localAssessmentRepository = localAssessmentRepository;
        this.localAcObjectRepository = localAcObjectRepository;
    }

    public MainResponseDTO getFitnessFunctionsAggregation() {
        List<LocalFitnessFunction> fitnessFunctions = localFitnessFunctionRepository.findAll();
        List<Product> products = productRepository.findAll();
        return MainResponseDTO.builder()
                .fitnessFunctionEnum(fitnessFunctions.stream().map(ff-> FitnessFunctionEnumDTO.builder()
                        .id(ff.getId().longValue())
                        .code(ff.getCode())
                        .description(ff.getDescription())
                        .build()).collect(Collectors.toList()))
                .domain(products.stream().map(product -> DomainDTO.builder()
                        .id(product.getDomain().getId().longValue())
                        .name(product.getDomain().getName())
                        .alias(product.getDomain().getAlias())
                        .ownerId(product.getDomain().getOwnerId().longValue())
                        .product(product.getDomain().getProducts().stream().map(domainProduct -> FitnessFunctionProductDTO.builder()
                                .id(domainProduct.getId().longValue())
                                .name(domainProduct.getName())
                                .alias(domainProduct.getAlias())
                                .ownerId(domainProduct.getOwnerID().longValue())
                                .fitnessFunctions(
                                        getLocalAssessmentCheck(domainProduct).stream()
                                                .map(localAssessmentCheck -> {
                                                    return FitnessFunctionShortDTO.builder()
                                                            .id(localAssessmentCheck.getFitnessFunction().getId().longValue())
                                                            .isCheck(localAssessmentCheck.getIsCheck())
                                                            .countAll(getCountAll(localAssessmentCheck))
                                                            .countSuccess(getCountSuccess(localAssessmentCheck))
                                                            .build();
                                                })
                                                .collect(Collectors.toList())
                                )
                                .build()).collect(Collectors.toList()))
                        .build()).collect(Collectors.toList()))
                .build();
    }

    private Integer getCountAll(LocalAssessmentCheck localAssessmentCheck) {
        return  localAcObjectRepository.countByLacId(localAssessmentCheck.getId());
    }

    private Integer getCountSuccess(LocalAssessmentCheck localAssessmentCheck) {
        return localAcObjectRepository.countByLacIdAndIsCheckTrue(localAssessmentCheck.getId());
    }

    private List<LocalAssessmentCheck> getLocalAssessmentCheck(Product domainProduct) {
        LocalAssessment localAssessment =
                localAssessmentRepository.findFirstByProductIdOrderByCreatedTimeDesc(domainProduct.getId()).get();
        return localAssessment.getChecks();
    }
}
