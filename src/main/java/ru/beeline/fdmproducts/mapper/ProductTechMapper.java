package ru.beeline.fdmproducts.mapper;

import org.springframework.stereotype.Component;
import ru.beeline.fdmlib.dto.auth.UserProfileDTO;
import ru.beeline.fdmlib.dto.auth.UserProfileShortDTO;
import ru.beeline.fdmlib.dto.product.GetProductsByIdsDTO;
import ru.beeline.fdmlib.dto.product.GetProductsDTO;
import ru.beeline.fdmproducts.domain.Product;
import ru.beeline.fdmproducts.dto.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ProductTechMapper {

    public static List<ProductDTO> mapToDto(List<Product> products) {
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

    public static GetProductsDTO mapToGetProductsDTO(Product product) {
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

    public static ProductInfoDTO mapToProductInfoDTO(Product product) {
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
                        .filter(techProduct -> techProduct.getDeletedDate() == null)
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

    public static ProductInfoShortDTO mapToProductInfoShortDTO(Product product, String ownerName) {
        return  ProductInfoShortDTO.builder()
                        .alias(product.getAlias())
                        .description(product.getDescription())
                        .gitUrl(product.getGitUrl())
                        .id(product.getId().toString())
                        .name(product.getName())
                        .structurizrApiUrl(product.getStructurizrApiUrl())
                        .structurizrWorkspaceName(product.getStructurizrWorkspaceName())
                        .uploadSource(product.getSource())
                        .uploadDate(product.getUploadDate())
                        .critical(product.getCritical())
                        .ownerName(ownerName)
                        .build();
    }

    public static ProductInfoShortV2DTO mapToProductInfoShortV2DTO(Product product) {
        return ProductInfoShortV2DTO.builder()
                .alias(product.getAlias())
                .description(product.getDescription())
                .gitUrl(product.getGitUrl())
                .id(product.getId().toString())
                .name(product.getName())
                .structurizrApiUrl(product.getStructurizrApiUrl())
                .structurizrWorkspaceName(product.getStructurizrWorkspaceName())
                .build();
    }

    public static List<GetProductsByIdsDTO> mapToGetProductsByIdsDTO(List<Product> products) {
        return products.stream()
                .map(product -> GetProductsByIdsDTO.builder()
                        .id(product.getId())
                        .name(product.getName())
                        .alias(product.getAlias())
                        .struturizrURL(product.getStructurizrApiUrl())
                        .build())
                .collect(Collectors.toList());
    }

    public static SystemInfoDTO enrichSystemWithProduct(String system, Map<String, Product> productMap,
                                                        Map<Integer, UserProfileShortDTO> userProfilesMap) {
        Product product = productMap.get(system.toLowerCase());
        if (product == null) {
            return null;
        }
        UserProfileShortDTO userProfileShortDTO = null;
        if (product.getOwnerID() != null) {
            userProfileShortDTO = userProfilesMap.get(product.getOwnerID());
        }
        return SystemInfoDTO.builder()
                .alias(product.getAlias())
                .description(product.getDescription())
                .gitUrl(product.getGitUrl())
                .id(product.getId().toString())
                .name(product.getName())
                .structurizrApiUrl(product.getStructurizrApiUrl())
                .structurizrWorkspaceName(product.getStructurizrWorkspaceName())
                .uploadSource(product.getSource())
                .uploadDate(product.getUploadDate())
                .critical(product.getCritical())
                .ownerName(userProfileShortDTO != null ? userProfileShortDTO.getFullName() : null)
                .ownerEmail(userProfileShortDTO != null ? userProfileShortDTO.getEmail() : null)
                .build();
    }

    public static ProductFullDTO mapToProductFullDTO(Product product, UserProfileDTO user) {
        return ProductFullDTO.builder()
                .alias(product.getAlias())
                .description(product.getDescription())
                .gitUrl(product.getGitUrl())
                .id(product.getId())
                .name(product.getName())
                .structurizrApiUrl(product.getStructurizrApiUrl())
                .structurizrWorkspaceName(product.getStructurizrWorkspaceName())
                .critical(product.getCritical())
                .ownerID(product.getOwnerID())
                .ownerEmail(user.getEmail())
                .ownerName(user.getFullName())
                .structurizrApiKey(product.getStructurizrApiKey())
                .structurizrApiSecret(product.getStructurizrApiSecret())
                .source(product.getSource())
                .uploadDate(product.getUploadDate())
                .techProducts(product.getTechProducts())
                .discoveredInterfaces(product.getDiscoveredInterfaces())
                .build();
    }
}