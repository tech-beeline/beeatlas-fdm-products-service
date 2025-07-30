package ru.beeline.fdmproducts.mapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.beeline.fdmproducts.domain.DiscoveredInterface;
import ru.beeline.fdmproducts.domain.Product;
import ru.beeline.fdmlib.dto.product.DiscoveredInterfaceDTO;
import ru.beeline.fdmproducts.repository.ProductRepository;

import java.time.LocalDateTime;

@Component
public class DiscoveredInterfaceMapper {

    @Autowired
    ProductRepository productRepository;

    public DiscoveredInterfaceDTO convertToDiscoveredInterfaceDto(DiscoveredInterface entity) {
        return DiscoveredInterfaceDTO.builder()
                .name(entity.getName())
                .externalId(entity.getExternalId())
                .apiId(entity.getApiId())
                .apiLink(entity.getApiLink())
                .version(entity.getVersion())
                .description(entity.getDescription())
                .status(entity.getStatus())
                .context(entity.getContext())
                .productId(entity.getProduct().getId())
                .build();
    }


    public DiscoveredInterface convertToEntity(DiscoveredInterfaceDTO dto) {
        Product product = productRepository.findById(dto.getProductId())
                .orElseThrow(() -> new IllegalArgumentException("Продукт с данным id не найден"));
        return DiscoveredInterface.builder()
                .name(dto.getName())
                .externalId(dto.getExternalId())
                .apiId(dto.getApiId())
                .apiLink(dto.getApiLink())
                .version(dto.getVersion())
                .description(dto.getDescription())
                .status(dto.getStatus())
                .context(dto.getContext())
                .product(product)
                .createdDate(LocalDateTime.now())
                .build();
    }

    public void updateEntityFromDto(DiscoveredInterfaceDTO dto, DiscoveredInterface entity) {
        Product product = productRepository.findById(dto.getProductId())
                .orElseThrow(() -> new IllegalArgumentException("Продукт с данным id не найден"));
        entity.setName(dto.getName());
        entity.setExternalId(dto.getExternalId());
        entity.setApiId(dto.getApiId());
        entity.setApiLink(dto.getApiLink());
        entity.setVersion(dto.getVersion());
        entity.setDescription(dto.getDescription());
        entity.setStatus(dto.getStatus());
        entity.setContext(dto.getContext());
        entity.setProduct(product);
        entity.setUpdatedDate(LocalDateTime.now());
    }
}
