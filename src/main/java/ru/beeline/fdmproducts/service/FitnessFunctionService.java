/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.beeline.fdmproducts.domain.LocalFitnessFunction;
import ru.beeline.fdmproducts.domain.Product;
import ru.beeline.fdmproducts.dto.*;
import ru.beeline.fdmproducts.repository.LocalAcObjectRepository;
import ru.beeline.fdmproducts.repository.LocalAssessmentRepository;
import ru.beeline.fdmproducts.repository.LocalFitnessFunctionRepository;
import ru.beeline.fdmproducts.repository.ProductRepository;

import java.util.*;
import java.util.stream.Collectors;

@Transactional(readOnly = true)
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
        List<Integer> productIds = products.stream().map(Product::getId).collect(Collectors.toList());

        List<LatestAssessmentCheckDTO> allChecks = localAssessmentRepository.findLatestChecksForProducts(productIds);

        List<Integer> allLacIds = allChecks.stream()
                .map(LatestAssessmentCheckDTO::getLacId)
                .distinct()
                .collect(Collectors.toList());

        Map<Integer, LacCountsDTO> countsByLacId = allLacIds.isEmpty() ? Collections.emptyMap() : localAcObjectRepository.countsByLacIds(
                allLacIds).stream().collect(Collectors.toMap(LacCountsDTO::getLacId, dto -> dto));

        Map<Integer, List<LatestAssessmentCheckDTO>> checksByProductId = allChecks.stream()
                .collect(Collectors.groupingBy(LatestAssessmentCheckDTO::getProductId));

        Map<Integer, List<Product>> productsByDomainId = products.stream()
                .filter(p -> p.getDomain() != null)
                .collect(Collectors.groupingBy(p -> p.getDomain().getId()));

        List<DomainDTO> domains = products.stream()
                .filter(p -> p.getDomain() != null)
                .map(Product::getDomain)
                .collect(Collectors.toMap(d -> d.getId(),
                                          d -> d,
                                          (existing, duplicate) -> existing,
                                          LinkedHashMap::new))
                .values()
                .stream()
                .map(domain -> {
                    List<Product> domainProducts = productsByDomainId.getOrDefault(domain.getId(),
                                                                                   Collections.emptyList());

                    List<FitnessFunctionProductDTO> productDTOs = domainProducts.stream().map(product -> {
                        List<LatestAssessmentCheckDTO> checks = checksByProductId.getOrDefault(product.getId(),
                                                                                               Collections.emptyList());

                        List<FitnessFunctionShortDTO> ffDTOs = checks.stream().map(check -> {
                            LacCountsDTO counts = countsByLacId.getOrDefault(check.getLacId(), LacCountsDTO.empty());
                            return FitnessFunctionShortDTO.builder()
                                    .id(check.getFitnessFunctionId())
                                    .isCheck(check.getIsCheck())
                                    .countAll(counts.getCountAll())
                                    .countSuccess(counts.getCountSuccess())
                                    .build();
                        }).collect(Collectors.toList());

                        return FitnessFunctionProductDTO.builder()
                                .id(product.getId())
                                .name(product.getName())
                                .alias(product.getAlias())
                                .ownerId(product.getOwnerID())
                                .fitnessFunctions(ffDTOs)
                                .build();
                    }).collect(Collectors.toList());

                    productDTOs.sort(Comparator.comparing(FitnessFunctionProductDTO::getName,
                                                          String.CASE_INSENSITIVE_ORDER)
                                             .thenComparing(FitnessFunctionProductDTO::getId));

                    return DomainDTO.builder()
                            .id(domain.getId())
                            .name(domain.getName())
                            .alias(domain.getAlias())
                            .ownerId(domain.getOwnerId())
                            .product(productDTOs)
                            .build();
                })
                .collect(Collectors.toList());

        domains.sort(Comparator.comparing(DomainDTO::getName, String.CASE_INSENSITIVE_ORDER)
                             .thenComparing(DomainDTO::getId));

        List<FitnessFunctionEnumDTO> ffEnum = fitnessFunctions.stream()
                .map(ff -> FitnessFunctionEnumDTO.builder()
                        .id(ff.getId())
                        .code(ff.getCode())
                        .description(ff.getDescription())
                        .build())
                .collect(Collectors.toList());

        ffEnum.sort(Comparator.comparing(FitnessFunctionEnumDTO::getCode, String.CASE_INSENSITIVE_ORDER));

        return MainResponseDTO.builder().fitnessFunctionEnum(ffEnum).domain(domains).build();
    }
}