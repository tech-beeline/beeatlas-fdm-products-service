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
        log.info("start of the Product Infrastructure Synchronization method:" + request.toString());
        Product product = productRepository.findByAliasCaseInsensitive(productAlias);
        log.info("product is" + product);

        if (product == null) {
            throw new EntityNotFoundException("Продукт не найден");
        }

        List<String> processedCmdbIds = request.getInfra().stream().map(InfraDTO::getCmdbId).toList();
        if (!processedCmdbIds.isEmpty()) {
            infraProductRepository.markInfraProductsDeleted(product.getId(), processedCmdbIds, LocalDateTime.now());
        }
        Map<String, Infra> existingInfraMap = infraRepository.findInfrasByProductId(product.getId())
                .stream()
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
        Set<String> requestCmdbIds = requestInfras.stream().map(InfraDTO::getCmdbId).collect(Collectors.toSet());
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

    private void processCreateOrUpdateInfras(List<InfraDTO> requestInfras,
                                             Map<String, Infra> existingInfraMap,
                                             Product product,
                                             List<Infra> newInfras,
                                             List<Infra> updatedInfras) {
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
                boolean hasLink = infra.getInfraProducts()
                        .stream()
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
        if (newInfras.isEmpty())
            return;
        List<Infra> savedInfras = infraRepository.saveAllAndFlush(newInfras);
        List<InfraProduct> newInfraProducts = new ArrayList<>();
        for (Infra infra : savedInfras) {
            boolean hasLink = infra.getInfraProducts()
                    .stream()
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
        if (updatedInfras.isEmpty())
            return;
        infraRepository.saveAllAndFlush(updatedInfras);
    }

    private void processAllProperties(List<InfraDTO> requestInfras, Map<String, Infra> existingInfraMap) {
        log.info("Received {} InfraDTOs to process", requestInfras.size());
        List<Integer> infraIds = requestInfras.stream()
                .map(dto -> existingInfraMap.get(dto.getCmdbId()))
                .filter(Objects::nonNull)
                .map(Infra::getId)
                .toList();
        List<Property> allProperties = propertyRepository.findByInfraIdIn(infraIds);
        log.info("Fetched total {} properties for all Infra", allProperties.size());
        Map<Integer, List<Property>> propertiesByInfraId = allProperties.stream()
                .collect(Collectors.groupingBy(p -> p.getInfra().getId()));
        List<Property> toCreate = new ArrayList<>();
        List<Property> toUpdate = new ArrayList<>();
        List<Property> toDelete = new ArrayList<>();
        for (InfraDTO infraDTO : requestInfras) {
            Infra infra = existingInfraMap.get(infraDTO.getCmdbId());
            if (infra != null) {
                List<Property> existingProperties = propertiesByInfraId.getOrDefault(infra.getId(), List.of());
                processProperties(infra, infraDTO.getProperties(), existingProperties, toCreate, toUpdate, toDelete);
            }
        }
        propertyRepository.saveAll(toDelete);
        log.info("Updated {} deleted properties", toUpdate.size());
        propertyRepository.saveAll(toCreate);
        log.info("Saved {} new properties", toCreate.size());
        propertyRepository.saveAll(toUpdate);
        log.info("Updated {} existing properties", toUpdate.size());
        log.info("Finished processing all InfraDTO properties");
    }

    private void processProperties(Infra infra,
                                   List<PropertyDTO> properties,
                                   List<Property> existingProperties,
                                   List<Property> toCreate,
                                   List<Property> toUpdate,
                                   List<Property> toDelete) {
        Map<String, Property> existingPropertyMap = existingProperties.stream()
                .collect(Collectors.toMap(Property::getName, Function.identity()));
        Set<String> incomingKeys = properties.stream().map(PropertyDTO::getKey).collect(Collectors.toSet());
        toDelete.addAll(existingProperties.stream()
                                .filter(property -> !incomingKeys.contains(property.getName()) && property.getDeletedDate() == null)
                                .peek(p -> p.setDeletedDate(LocalDateTime.now()))
                                .collect(Collectors.toList()));

        for (PropertyDTO dto : properties) {
            Property existing = existingPropertyMap.get(dto.getKey());
            if (existing == null) {
                toCreate.add(buildNewProperty(infra, dto));
            } else if (!Objects.equals(existing.getValue(), dto.getValue())) {
                existing.setValue(dto.getValue());
                existing.setDeletedDate(null);
                existing.setLastModifiedDate(LocalDateTime.now());
                toUpdate.add(existing);
            }
        }
    }

    private Property buildNewProperty(Infra infra, PropertyDTO propDTO) {
        return Property.builder()
                .infra(infra)
                .name(propDTO.getKey())
                .value(propDTO.getValue())
                .createdDate(LocalDateTime.now())
                .build();
    }

    private void processRelations(List<RelationDTO> relations, Map<String, Infra> existingInfraMap) {
        log.info("start process for Relations with size" + relations.size());
        List<Relation> relationsForSave = new ArrayList<>();
        Map<String, List<Relation>> children = relationRepository.findByParentIdIn(relations.stream()
                                                                                           .map(RelationDTO::getCmdbId)
                                                                                           .collect(Collectors.toList()))
                .stream()
                .collect(Collectors.groupingBy(Relation::getParentId));
        List<String> chldIds = relations.stream()
                .flatMap(r -> r.getChildren().stream())
                .collect(Collectors.toList());
        List<String> cacheInfra = new ArrayList<>();
        if(!chldIds.isEmpty()){
        cacheInfra = infraRepository.findCmdbIdByCmdbIdIn(chldIds);
        }
        for (RelationDTO relationDTO : relations) {
            if (existingInfraMap.containsKey(relationDTO.getCmdbId())) {
                processRelation(relationDTO.getCmdbId(), relationDTO, relationsForSave, children, cacheInfra);
            }
        }
        children.clear();
        cacheInfra.clear();

        relationRepository.saveAll(relationsForSave);
        log.info("The processRelations method is completed");
    }

    private void processRelation(String cmdbId,
                                 RelationDTO relationDTO,
                                 List<Relation> relationsForSave,
                                 Map<String, List<Relation>> childrenRelation,
                                 List<String> cacheInfra) {

        List<Relation> existingRelations = childrenRelation.get(cmdbId);
        if (existingRelations == null) {
            existingRelations = new ArrayList<>();
        }
        Set<String> processedChildrenIds = new HashSet<>(relationDTO.getChildren());
        Map<String, Relation> existingRelationsMap = existingRelations.stream()
                .collect(Collectors.toMap(Relation::getChildId, Function.identity()));

        List<Relation> newRelations = processedChildrenIds.stream()
                .filter(childId -> !existingRelationsMap.containsKey(childId))
                .map(childId -> getInfra(childId, cacheInfra).map(childInfra -> Relation.builder()
                        .parentId(cmdbId)
                        .childId(childId)
                        .createdDate(LocalDateTime.now())
                        .build()).orElse(null))
                .filter(Objects::nonNull)
                .toList();
        relationsForSave.addAll(newRelations);
    }

    private Optional<Infra> getInfra(String childId, List<String> cacheInfra) {
        return cacheInfra.contains(childId) ? Optional.of(new Infra()) : Optional.empty();
    }
}