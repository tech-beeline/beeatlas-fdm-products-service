package ru.beeline.fdmproducts.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.beeline.fdmproducts.domain.*;
import ru.beeline.fdmproducts.dto.InfraDTO;
import ru.beeline.fdmproducts.dto.InfraRequestDTO;
import ru.beeline.fdmproducts.dto.PropertyDTO;
import ru.beeline.fdmproducts.dto.RelationDTO;
import ru.beeline.fdmproducts.exception.EntityNotFoundException;
import ru.beeline.fdmproducts.repository.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Transactional
@Service
@Slf4j
public class InfraService {
    @Autowired
    private InfraRepository infraRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private PropertyRepository propertyRepository;
    @Autowired
    private RelationRepository relationRepository;
    @Autowired
    private InfraProductRepository infraProductRepository;

    public void syncInfrastructure(String productAlias, InfraRequestDTO request) {
        log.info("start of the Product Infrastructure Synchronization method");
        Product product = productRepository.findByAliasCaseInsensitive(productAlias);
        log.info("product is" + product);

        if (product == null) {
            throw new EntityNotFoundException("Продукт не найден");
        }

        List<InfraProduct> existingInfraProducts = infraProductRepository.findByProductId(product.getId());
        log.info("existingInfraProducts is" + existingInfraProducts);
        List<String> processedCmdbIds = request.getInfra().stream().map(InfraDTO::getCmdbId).toList();
        existingInfraProducts.stream()
                .map(InfraProduct::getInfra)
                .filter(infra -> !processedCmdbIds.contains(infra.getCmdbId()))
                .filter(infra -> infra.getDeletedDate() == null)
                .forEach(infra -> {
                    log.info("setDeletedDate for infraId:" + infra.getId());
                    infra.setDeletedDate(LocalDateTime.now());
                    infra.getInfraProducts().forEach(infraProduct -> infraProduct.setDeletedDate(LocalDateTime.now()));
                });
        infraProductRepository.saveAll(existingInfraProducts);
        Map<String, Infra> existingInfraMap = existingInfraProducts.stream()
                .map(InfraProduct::getInfra)
                .collect(Collectors.toMap(Infra::getCmdbId, Function.identity()));
        processInfras(request.getInfra(), product, existingInfraMap);
        processRelations(request.getRelations(), existingInfraMap);
        log.info("The syncInfrastructure method is completed");
    }
    private void processInfras(List<InfraDTO> requestInfras, Product product, Map<String, Infra> existingInfraMap) {
        log.info("requestInfras size: " + requestInfras.size());
        loadMissingInfras(requestInfras, existingInfraMap);
        List<Infra> newInfras = new ArrayList<>();
        List<Infra> updatedInfras = new ArrayList<>();
        processCreateOrUpdateInfras(requestInfras, existingInfraMap, product, newInfras, updatedInfras);
        saveNewInfrasAndProducts(newInfras, product, existingInfraMap);
        saveUpdatedInfras(updatedInfras);
        processAllProperties(requestInfras, existingInfraMap);
    }

    private void loadMissingInfras(List<InfraDTO> requestInfras, Map<String, Infra> existingInfraMap) {
        Set<String> requestCmdbIds = requestInfras.stream()
                .map(InfraDTO::getCmdbId)
                .collect(Collectors.toSet());
        Set<String> missingCmdbIds = requestCmdbIds.stream()
                .filter(cmdbId -> !existingInfraMap.containsKey(cmdbId))
                .collect(Collectors.toSet());
        if (!missingCmdbIds.isEmpty()) {
            List<Infra> missingInfras = infraRepository.findByCmdbIdIn(missingCmdbIds);
            for (Infra infra : missingInfras) {
                existingInfraMap.put(infra.getCmdbId(), infra);
            }
        }
    }

    private void processCreateOrUpdateInfras(List<InfraDTO> requestInfras, Map<String, Infra> existingInfraMap,
                                             Product product, List<Infra> newInfras, List<Infra> updatedInfras) {
        for (InfraDTO infraDTO : requestInfras) {
            Infra infra = existingInfraMap.get(infraDTO.getCmdbId());
            if (infra == null) {
                Infra newInfra = Infra.builder()
                        .name(infraDTO.getName())
                        .type(infraDTO.getType())
                        .cmdbId(infraDTO.getCmdbId())
                        .infraProducts(new HashSet<>())
                        .createdDate(LocalDateTime.now())
                        .build();
                InfraProduct infraProduct = InfraProduct.builder()
                        .createdDate(LocalDateTime.now())
                        .infra(newInfra)
                        .product(product)
                        .build();
                newInfra.getInfraProducts().add(infraProduct);
                newInfras.add(newInfra);
                existingInfraMap.put(infraDTO.getCmdbId(), newInfra);
            } else {
                boolean modified = false;
                if (!Objects.equals(infra.getName(), infraDTO.getName())) {
                    infra.setName(infraDTO.getName());
                    modified = true;
                }
                if (!Objects.equals(infra.getType(), infraDTO.getType())) {
                    infra.setType(infraDTO.getType());
                    modified = true;
                }
                if (modified) {
                    infra.setLastModifiedDate(LocalDateTime.now());
                    updatedInfras.add(infra);
                }
                boolean hasLink = infra.getInfraProducts().stream()
                        .anyMatch(ip -> ip.getProduct().getId().equals(product.getId()));
                if (!hasLink) {
                    InfraProduct infraProduct = InfraProduct.builder()
                            .createdDate(LocalDateTime.now())
                            .infra(infra)
                            .product(product)
                            .build();
                    infra.getInfraProducts().add(infraProduct);
                }
            }
        }
    }

    private void saveNewInfrasAndProducts(List<Infra> newInfras, Product product, Map<String, Infra> existingInfraMap) {
        if (newInfras.isEmpty()) return;
        List<Infra> savedInfras = infraRepository.saveAll(newInfras);
        infraRepository.flush();
        List<InfraProduct> newInfraProducts = new ArrayList<>();
        for (Infra infra : savedInfras) {
            boolean hasLink = infra.getInfraProducts().stream()
                    .anyMatch(ip -> ip.getProduct().getId().equals(product.getId()));
            if (!hasLink) {
                InfraProduct infraProduct = InfraProduct.builder()
                        .createdDate(LocalDateTime.now())
                        .infra(infra)
                        .product(product)
                        .build();
                infra.getInfraProducts().add(infraProduct);
                newInfraProducts.add(infraProduct);
            }
            existingInfraMap.put(infra.getCmdbId(), infra);
        }
        if (!newInfraProducts.isEmpty()) {
            infraProductRepository.saveAll(newInfraProducts);
            infraProductRepository.flush();
        }
    }

    private void saveUpdatedInfras(List<Infra> updatedInfras) {
        if (updatedInfras.isEmpty()) return;
        infraRepository.saveAll(updatedInfras);
        infraRepository.flush();
    }

    private void processAllProperties(List<InfraDTO> requestInfras, Map<String, Infra> existingInfraMap) {
        for (InfraDTO infraDTO : requestInfras) {
            Infra infra = existingInfraMap.get(infraDTO.getCmdbId());
            if (infra != null) {
                processProperties(infra, infraDTO.getProperties());
            }
        }
    }

    private void processProperties(Infra infra, List<PropertyDTO> properties) {
        List<Property> existingProperties = propertyRepository.findByInfraId(infra.getId());
        Map<String, Property> existingPropertyMap = existingProperties.stream()
                .collect(Collectors.toMap(Property::getName, Function.identity()));

        List<String> processedKeys = properties.stream().map(PropertyDTO::getKey).toList();
        existingProperties.stream()
                .filter(property -> !processedKeys.contains(property.getName()))
                .filter(property -> property.getDeletedDate() == null)
                .forEach(property -> property.setDeletedDate(LocalDateTime.now()));
        propertyRepository.saveAll(existingProperties);

        List<Property> toCreate = new ArrayList<>();
        List<Property> toUpdate = new ArrayList<>();
        for (PropertyDTO propDTO : properties) {
            Property property = existingPropertyMap.get(propDTO.getKey());
            if (property == null) {
                Property newProp = buildNewProperty(infra, propDTO);
                toCreate.add(newProp);
            } else {
                Property updatedProp = prepareUpdatedProperty(property, propDTO);
                if (updatedProp != null) {
                    toUpdate.add(updatedProp);
                }
            }
        }
        propertyRepository.saveAll(toCreate);
        log.info("save all prop");
        propertyRepository.saveAll(toUpdate);
        log.info("update all prop");
    }

    private Property buildNewProperty(Infra infra, PropertyDTO propDTO) {
        return Property.builder()
                .infra(infra)
                .name(propDTO.getKey())
                .value(propDTO.getValue())
                .createdDate(LocalDateTime.now())
                .build();
    }

    private Property prepareUpdatedProperty(Property property, PropertyDTO propDTO) {
        if (!Objects.equals(property.getValue(), propDTO.getValue())) {
            property.setValue(propDTO.getValue());
            property.setDeletedDate(null);
            property.setLastModifiedDate(LocalDateTime.now());
            return property;
        }
        return null;
    }

    private void processRelations(List<RelationDTO> relations, Map<String, Infra> existingInfraMap) {
        log.info("start process for Relations");
        for (RelationDTO relationDTO : relations) {
            log.info("relationDTO is" + relationDTO);
            Infra infra = existingInfraMap.get(relationDTO.getCmdbId());
            log.info("infra is" + infra);

            if (infra != null) {
                processRelation(infra, relationDTO);
            }
        }
        log.info("The processRelations method is completed");
    }

    private void processRelation(final Infra infra, RelationDTO relationDTO) {
        List<Relation> existingRelations = relationRepository.findByParentId(infra.getCmdbId());
        log.info("existingRelations=" + existingRelations);
        Set<String> processedChildrenIds = new HashSet<>(relationDTO.getChildren());
        existingRelations.stream()
                .filter(child -> !processedChildrenIds.contains(child.getChildId()))
                .filter(child -> child.getDeletedDate() == null)
                .forEach(child -> child.setDeletedDate(LocalDateTime.now()));
        relationRepository.saveAll(existingRelations);

        Map<String, Relation> existingRelationsMap = existingRelations.stream()
                .collect(Collectors.toMap(Relation::getChildId, Function.identity()));

        List<Relation> newRelations = processedChildrenIds.stream()
                .filter(childId -> !existingRelationsMap.containsKey(childId))
                .map(childId -> infraRepository.findByCmdbId(childId)
                        .map(childInfra -> Relation.builder()
                                .parentId(infra.getCmdbId())
                                .childId(childInfra.getCmdbId())
                                .createdDate(LocalDateTime.now())
                                .build())
                        .orElse(null))
                .filter(Objects::nonNull)
                .toList();
        relationRepository.saveAll(newRelations);
    }
}