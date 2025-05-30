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
        for (InfraDTO infraDTO : requestInfras) {
            Infra infra = existingInfraMap.get(infraDTO.getCmdbId());

            if (infra == null) {
                Optional<Infra> optionalInfra = infraRepository.findByCmdbId(infraDTO.getCmdbId());
                if (optionalInfra.isEmpty()) {
                    infra = createNewInfra(infraDTO, product);
                    existingInfraMap.put(infraDTO.getCmdbId(), infra);
                } else {
                    updateExistingInfra(optionalInfra.get(), infraDTO);
                }
            } else {
                updateExistingInfra(infra, infraDTO);
            }

            processProperties(infra, infraDTO.getProperties());
        } log.info("The processInfras method is completed");
    }

    private Infra createNewInfra(InfraDTO dto, Product product) {
        System.out.println("createNewInfra before flash");
        infraRepository.flush();
        System.out.println("createNewInfra after flash");
        Infra newInfra = Infra.builder()
                .name(dto.getName())
                .type(dto.getType())
                .cmdbId(dto.getCmdbId())
                .infraProducts(new HashSet<>())
                .createdDate(LocalDateTime.now())
                .build();
        System.out.println("cmdb= " + newInfra.getCmdbId());
        log.info("createNewInfra cmdb=", dto.getCmdbId());
        newInfra = infraRepository.save(newInfra);
        infraRepository.flush();
        System.out.println("INFRA SAVED");
        InfraProduct infraProduct = InfraProduct.builder()
                .createdDate(LocalDateTime.now())
                .infra(newInfra)
                .product(product)
                .build();
        newInfra.getInfraProducts().add(infraProduct);
        log.info("save new infraProduct");
        infraProduct = infraProductRepository.save(infraProduct);
        log.info("infraProduct has been saved");
        return infraProduct.getInfra();

    }

    private void updateExistingInfra(Infra infra, InfraDTO dto) {
        boolean modified = false;

        if (!Objects.equals(infra.getName(), dto.getName())) {
            infra.setName(dto.getName());
            modified = true;
        }

        if (!Objects.equals(infra.getType(), dto.getType())) {
            infra.setType(dto.getType());
            modified = true;
        }

        if (modified) {
            infra.setLastModifiedDate(LocalDateTime.now());
            log.info("update infra name or type");
            infraRepository.save(infra);
            infraRepository.flush();
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
        log.info("save all prop");
        propertyRepository.saveAll(existingProperties);

        for (PropertyDTO propDTO : properties) {
            Property property = existingPropertyMap.get(propDTO.getKey());

            if (property == null) {
                createNewProperty(infra, propDTO);
            } else {
                updateExistingProperty(property, propDTO);
            }
        }

    }

    private void createNewProperty(Infra infra, PropertyDTO propDTO) {
        Property newProperty = Property.builder()
                .infra(infra)
                .name(propDTO.getKey())
                .value(propDTO.getValue())
                .createdDate(LocalDateTime.now())
                .build();
        log.info("create prop");
        propertyRepository.save(newProperty);
    }

    private void updateExistingProperty(Property property, PropertyDTO propDTO) {
        if (!Objects.equals(property.getValue(), propDTO.getValue())) {
            property.setValue(propDTO.getValue());
            property.setDeletedDate(null);
            property.setLastModifiedDate(LocalDateTime.now());
            log.info("update prop");
            propertyRepository.save(property);
        }
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
                .collect(Collectors.toMap(rel -> rel.getChildId(), Function.identity()));

        processedChildrenIds.stream()
                .filter(childId -> !existingRelationsMap.containsKey(childId))
                .forEach(childId -> createNewRelation(infra, infraRepository.findByCmdbId(childId).get()));
    }

    private void createNewRelation(Infra parentInfra, Infra childInfra) {

        Relation newRelation = Relation.builder()
                .parentId(parentInfra.getCmdbId())
                .childId(childInfra.getCmdbId())
                .createdDate(LocalDateTime.now())
                .build();
        log.info("createNewRelation" + newRelation);

        relationRepository.save(newRelation);
    }
}