package ru.beeline.fdmproducts.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.beeline.fdmproducts.domain.*;
import ru.beeline.fdmproducts.dto.*;
import ru.beeline.fdmproducts.exception.EntityNotFoundException;
import ru.beeline.fdmproducts.repository.*;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
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
    @PersistenceContext
    private EntityManager entityManager;
    private static final int BATCH_SIZE = 1000;

    public void syncInfrastructure(String alias, InfraRequestDTO request) {
        log.info("ℹ️ start of the Product Infrastructure Synchronization method");
        String productAlias = URLDecoder.decode(alias, StandardCharsets.UTF_8);
        Product product = productRepository.findByAliasCaseInsensitive(productAlias);

        if (product == null) {
            throw new EntityNotFoundException("⚠️ Продукт не найден");
        }
        List<String> processedCmdbIds = request.getInfra().stream().map(InfraDTO::getCmdbId).toList();
        if (!processedCmdbIds.isEmpty()) {
            infraProductRepository.restoreInfraProducts(product.getId(), processedCmdbIds, LocalDateTime.now());
            infraProductRepository.markInfraProductsDeleted(product.getId(), processedCmdbIds, LocalDateTime.now());
        }
        Map<String, Infra> existingInfraMap = infraRepository.findInfrasByProductId(product.getId())
                .stream()
                .collect(Collectors.toMap(Infra::getCmdbId, Function.identity()));
        processInfras(request.getInfra(), product, existingInfraMap);
        request.getInfra().clear();
        request.setInfra(null);
        processRelations(request.getRelations(), existingInfraMap);
        existingInfraMap.clear();
        existingInfraMap = null;
        request.getRelations().clear();
        request.setRelations(null);
        System.gc();
        log.info("✅ The syncInfrastructure method is completed");
    }

    private void processInfras(List<InfraDTO> requestInfras, Product product, Map<String, Infra> existingInfraMap) {
        loadMissingInfras(requestInfras, existingInfraMap);
        List<Infra> newInfras = new ArrayList<>();
        List<Infra> updatedInfras = new ArrayList<>();
        processCreateOrUpdateInfras(requestInfras, existingInfraMap, product, newInfras, updatedInfras);
        saveNewInfrasAndProducts(newInfras, product, existingInfraMap);
        newInfras = null;
        saveUpdatedInfras(updatedInfras);
        updatedInfras = null;
        processAllProperties(requestInfras, existingInfraMap);
        requestInfras = null;
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
        if (newInfras.isEmpty()) {
            return;
        }
        for (int i = 0; i < newInfras.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, newInfras.size());
            List<Infra> batch = newInfras.subList(i, end);
            List<Infra> savedBatch = infraRepository.saveAllAndFlush(batch);
            List<InfraProduct> infraProductsBatch = new ArrayList<>();
            for (Infra infra : savedBatch) {
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
                    infraProductsBatch.add(infraProduct);
                }
                existingInfraMap.put(infra.getCmdbId(), infra);
            }
            if (!infraProductsBatch.isEmpty()) {
                infraProductRepository.saveAll(infraProductsBatch);
                infraProductRepository.flush();
            }
            entityManager.clear();
        }
        newInfras.clear();
        newInfras = null;
    }

    private void saveUpdatedInfras(List<Infra> updatedInfras) {
        if (updatedInfras.isEmpty())
            return;
        infraRepository.saveAllAndFlush(updatedInfras);
    }

    private void processAllProperties(List<InfraDTO> requestInfras, Map<String, Infra> existingInfraMap) {
        List<Integer> infraIds = requestInfras.stream()
                .map(dto -> existingInfraMap.get(dto.getCmdbId()))
                .filter(Objects::nonNull)
                .map(Infra::getId)
                .toList();
        for (int i = 0; i < infraIds.size(); i += BATCH_SIZE) {
            List<Integer> batchInfraIds = infraIds.subList(i, Math.min(i + BATCH_SIZE, infraIds.size()));
            List<Property> allProperties = propertyRepository.findByInfraIdIn(batchInfraIds);

            Map<Integer, List<Property>> propertiesByInfraId = allProperties.stream()
                    .collect(Collectors.groupingBy(p -> p.getInfra().getId()));
            List<Property> toCreate = new ArrayList<>();
            List<Property> toUpdate = new ArrayList<>();
            List<Property> toDelete = new ArrayList<>();
            for (InfraDTO infraDTO : requestInfras) {
                Infra infra = existingInfraMap.get(infraDTO.getCmdbId());
                if (infra != null && batchInfraIds.contains(infra.getId())) {
                    List<Property> existingProperties = propertiesByInfraId.getOrDefault(infra.getId(), List.of());
                    processProperties(infra, infraDTO.getProperties(), existingProperties, toCreate, toUpdate, toDelete);
                }
            }
            saveInBatches(toDelete, "deleted properties");
            saveInBatches(toCreate, "new properties");
            saveInBatches(toUpdate, "existing properties");
            entityManager.clear();
        }
    }

    private void saveInBatches(List<Property> props, String label) {
        for (int i = 0; i < props.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, props.size());
            List<Property> batch = props.subList(i, end);
            propertyRepository.saveAll(batch);
            propertyRepository.flush();
        }
        props.clear();
        props = null;
    }

    private void processProperties(Infra infra, List<PropertyDTO> properties, List<Property> existingProperties,
                                   List<Property> toCreate, List<Property> toUpdate, List<Property> toDelete) {
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
            } else {
                boolean valueChanged = !Objects.equals(existing.getValue(), dto.getValue());
                boolean wasDeleted = existing.getDeletedDate() != null;
                if (valueChanged || wasDeleted) {
                    existing.setValue(dto.getValue());
                    existing.setDeletedDate(null);
                    existing.setLastModifiedDate(LocalDateTime.now());
                    toUpdate.add(existing);
                }
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
        List<Relation> relationsForSave = new ArrayList<>();
        Map<String, List<Relation>> children = relationRepository.findByParentIdIn(
                        relations.stream().map(RelationDTO::getCmdbId).collect(Collectors.toList()))
                .stream().collect(Collectors.groupingBy(Relation::getParentId));
        List<String> allChildIds = relations.stream()
                .flatMap(r -> r.getChildren().stream())
                .collect(Collectors.toList());
        List<String> cacheInfra = new ArrayList<>();
        int batchSize = 1000;
        for (int i = 0; i < allChildIds.size(); i += batchSize) {
            List<String> batch = allChildIds.subList(i, Math.min(i + batchSize, allChildIds.size()));
            cacheInfra.addAll(infraRepository.findCmdbIdByCmdbIdIn(batch));
        }
        for (RelationDTO relationDTO : relations) {
            if (existingInfraMap.containsKey(relationDTO.getCmdbId())) {
                processRelation(relationDTO.getCmdbId(), relationDTO, relationsForSave, children, cacheInfra);
            }
            if (relationsForSave.size() >= BATCH_SIZE) {
                relationRepository.saveAll(relationsForSave);
                relationRepository.flush();
                relationsForSave.clear();
            }
        }
        if (!relationsForSave.isEmpty()) {
            relationRepository.saveAll(relationsForSave);
            relationRepository.flush();
        }
        relationsForSave = null;
        children.clear();
        cacheInfra.clear();
        children = null;
        cacheInfra = null;
    }

    private void processRelation(String cmdbId, RelationDTO relationDTO, List<Relation> relationsForSave,
                                 Map<String, List<Relation>> childrenRelation, List<String> cacheInfra) {

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

    public ProductInfraDto getProductInfraByName(String name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("name is empty");
        }
        Optional<Infra> infraOpt = infraRepository.findByNameCaseInsensitiveAndNotDeleted(name);
        if (infraOpt.isEmpty()) {
            throw new EntityNotFoundException("Infra with name " + name + " not found");
        }
        Infra infra = infraOpt.get();

        List<Integer> productIds = infraProductRepository.findProductIdsByInfraId(infra.getId());
        if (productIds.isEmpty()) {
            return ProductInfraDto.builder().name(name).parentSystems(Collections.emptyList()).build();
        }

        List<String> aliases = productRepository.findAliasesByIds(productIds);

        ProductInfraDto result =  ProductInfraDto.builder().name(name).parentSystems(aliases).build();
        return result;
    }

    public List<ProductInfraSearchDto> searchByParameterValue(String parameter, String value) {
        List<Property> properties = propertyRepository.findByNameAndValue(parameter, value);
        if (properties.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Infra> infras = properties.stream()
                .map(Property::getInfra)
                .filter(infra -> infra.getDeletedDate() == null)
                .collect(Collectors.toSet());

        List<ProductInfraSearchDto> result = new ArrayList<>();

        for (Infra infra : infras) {
            List<String> parentSystems = infra.getInfraProducts().stream()
                    .map(InfraProduct::getProduct)
                    .filter(Objects::nonNull)
                    .map(Product::getAlias)
                    .collect(Collectors.toList());

            result.add(ProductInfraSearchDto.builder()
                               .name(infra.getName())
                               .parameter(parameter)
                               .value(value)
                               .parentSystems(parentSystems)
                               .build());
        }

        return result;
    }

    public List<ProductInfraDto> getProductInfraContainsName(String name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("name is empty");
        }
        List<Infra> infraList = infraRepository.findByNameContainingIgnoreCaseAndNotDeleted(name);
        if (infraList.isEmpty()) {
            return Collections.emptyList();
        }

        List<ProductInfraDto> result = new ArrayList<>();
        for (Infra infra : infraList) {
            List<Integer> productIds = infraProductRepository.findProductIdsByInfraId(infra.getId());
            if (productIds.isEmpty()) {
                result.add(ProductInfraDto.builder()
                                   .name(infra.getName())
                                   .parentSystems(Collections.emptyList())
                                   .build());
            } else {
                List<String> aliases = productRepository.findAliasesByIds(productIds);
                result.add(ProductInfraDto.builder()
                                   .name(infra.getName())
                                   .parentSystems(aliases)
                                   .build());
            }
        }
        return result;
    }
}