package ru.beeline.fdmproducts.mapper;

import org.springframework.stereotype.Component;
import ru.beeline.fdmlib.dto.product.GetProductsDTO;
import ru.beeline.fdmproducts.domain.Product;
import ru.beeline.fdmproducts.dto.ProductDTO;
import ru.beeline.fdmproducts.dto.ProductInfoDTO;
import ru.beeline.fdmproducts.dto.TechDTO;
import ru.beeline.fdmproducts.dto.TechInfoDTO;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ProductTechMapper {
    public List<ProductDTO> mapToDto(List<Product> products) {
        if (products != null && !products.isEmpty()) {
            return products.stream()
                    .filter(product -> product.getTechProducts() != null && !product.getTechProducts().isEmpty())
                    .map(product -> ProductDTO.builder()
                            .productId(product.getId())
                            .alias(product.getAlias())
                            .tech(product.getTechProducts()
                                          .stream()
                                          .map(techProduct -> TechDTO.builder()
                                                  .id(techProduct.getTechId())
                                                  .label("")
                                                  .build())
                                          .collect(Collectors.toList()))
                            .build())
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    public GetProductsDTO mapToGetProductsDTO(Product product) {
        if (product == null) {
            return null;
        }
        return GetProductsDTO.builder()
                .id(product.getId())
                .name(product.getName())
                .alias(product.getAlias())
                .description(product.getDescription())
                .build();
    }

    public ProductInfoDTO mapToProductInfoDTO(Product product) {
        return ProductInfoDTO.builder()
                .alias(product.getAlias())
                .description(product.getDescription())
                .gitUrl(product.getGitUrl())
                .id(product.getId().toString())
                .name(product.getName())
                .structurizrApiUrl(product.getStructurizrApiUrl())
                .structurizrWorkspaceName(product.getStructurizrWorkspaceName())
                .techProducts(product.getTechProducts()
                                      .stream()
                                      .map(techProduct -> TechInfoDTO.builder()
                                              .id(techProduct.getId())
                                              .techId(techProduct.getTechId())
                                              .createdDate(techProduct.getCreatedDate())
                                              .lastModifiedDate(techProduct.getLastModifiedDate())
                                              .deletedDate(techProduct.getDeletedDate())
                                              .source(techProduct.getSource())
                                              .build())
                                      .collect(Collectors.toList()))
                .build();
    }
}