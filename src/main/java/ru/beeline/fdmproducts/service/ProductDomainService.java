/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.beeline.fdmproducts.domain.Product;
import ru.beeline.fdmproducts.domain.ProductDomain;
import ru.beeline.fdmproducts.dto.domain.DomainItemDTO;
import ru.beeline.fdmproducts.dto.domain.DomainProductDTO;
import ru.beeline.fdmproducts.dto.domain.DomainProductsResponseDTO;
import ru.beeline.fdmproducts.repository.ProductDomainRepository;
import ru.beeline.fdmproducts.repository.ProductRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Transactional(readOnly = true)
@Service
public class ProductDomainService {

    private static final String UNASSIGNED_DOMAIN_NAME = "Блок не указан";

    private final ProductRepository productRepository;
    private final ProductDomainRepository productDomainRepository;

    public ProductDomainService(ProductRepository productRepository,
                                ProductDomainRepository productDomainRepository) {
        this.productRepository = productRepository;
        this.productDomainRepository = productDomainRepository;
    }

    public DomainProductsResponseDTO getDomainsWithProducts() {
        List<ProductDomain> domains = productDomainRepository.findAllByOrderByNameAsc();
        List<Product> products = productRepository.findAllOrderByNameAsc();

        Set<Integer> domainIds = domains.stream()
                .map(ProductDomain::getId)
                .collect(Collectors.toCollection(HashSet::new));

        Map<Integer, List<Product>> productsByDomainId = new HashMap<>();
        List<Product> orphanProducts = new ArrayList<>();

        for (Product product : products) {
            ProductDomain domain = product.getDomain();
            Integer domainId = domain != null ? domain.getId() : null;
            if (domainId != null && domainIds.contains(domainId)) {
                productsByDomainId.computeIfAbsent(domainId, ignored -> new ArrayList<>()).add(product);
            } else {
                orphanProducts.add(product);
            }
        }

        List<DomainItemDTO> result = new ArrayList<>();

        for (ProductDomain domain : domains) {
            List<Product> domainProducts = productsByDomainId.getOrDefault(domain.getId(), Collections.emptyList());
            result.add(toDomainItemDTO(domain, domainProducts));
        }

        if (!orphanProducts.isEmpty()) {
            result.add(DomainItemDTO.builder()
                    .id(null)
                    .name(UNASSIGNED_DOMAIN_NAME)
                    .alias(null)
                    .ownerId(null)
                    .product(orphanProducts.stream().map(this::toProductDTO).collect(Collectors.toList()))
                    .build());
            result.sort(Comparator.comparing(DomainItemDTO::getName, Comparator.nullsLast(String::compareTo))
                    .thenComparing(DomainItemDTO::getId, Comparator.nullsLast(Integer::compareTo)));
        }

        return DomainProductsResponseDTO.builder().domain(result).build();
    }

    private DomainItemDTO toDomainItemDTO(ProductDomain domain, List<Product> products) {
        return DomainItemDTO.builder()
                .id(domain.getId())
                .name(domain.getName())
                .alias(domain.getAlias())
                .ownerId(domain.getOwnerId())
                .product(products.stream().map(this::toProductDTO).collect(Collectors.toList()))
                .build();
    }

    private DomainProductDTO toProductDTO(Product product) {
        return DomainProductDTO.builder()
                .id(product.getId())
                .name(product.getName())
                .alias(product.getAlias())
                .ownerId(product.getOwnerID())
                .build();
    }
}
