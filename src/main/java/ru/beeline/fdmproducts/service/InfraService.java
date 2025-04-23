package ru.beeline.fdmproducts.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.beeline.fdmproducts.domain.Infra;
import ru.beeline.fdmproducts.domain.Product;
import ru.beeline.fdmproducts.domain.Property;
import ru.beeline.fdmproducts.domain.Relation;
import ru.beeline.fdmproducts.dto.InfraDTO;
import ru.beeline.fdmproducts.dto.InfraRequestDTO;
import ru.beeline.fdmproducts.dto.PropertyDTO;
import ru.beeline.fdmproducts.dto.RelationDTO;
import ru.beeline.fdmproducts.exception.EntityNotFoundException;
import ru.beeline.fdmproducts.repository.InfraRepository;
import ru.beeline.fdmproducts.repository.ProductRepository;
import ru.beeline.fdmproducts.repository.PropertyRepository;
import ru.beeline.fdmproducts.repository.RelationRepository;

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

    public void syncInfrastructure(String productAlias, InfraRequestDTO request) {
        log.info("start of the Product Infrastructure Synchronization method");
        Product product = productRepository.findByAliasCaseInsensitive(productAlias);

        if (product == null) {
            throw new EntityNotFoundException("Продукт не найден");
        }

        List<Infra> existingInfras = infraRepository.findByProductId(product.getId());
        List<String> processedCmdbIds = request.getInfra().stream().map(InfraDTO::getCmdbId).toList();
        existingInfras.stream()
                .filter(infra -> !processedCmdbIds.contains(infra.getCmdbId()))
                .filter(infra -> infra.getDeletedDate() == null)
                .forEach(infra -> infra.setDeletedDate(LocalDateTime.now()));
        infraRepository.saveAll(existingInfras);

        Map<String, Infra> existingInfraMap = existingInfras.stream()
                .collect(Collectors.toMap(Infra::getCmdbId, Function.identity()));

        processInfras(request.getInfra(), product, existingInfraMap);

        processRelations(request.getRelations(), existingInfraMap);
        log.info("The syncInfrastructure method is completed");
    }

    private void processInfras(List<InfraDTO> requestInfras, Product product, Map<String, Infra> existingInfraMap) {
        for (InfraDTO infraDTO : requestInfras) {
            Infra infra = existingInfraMap.get(infraDTO.getCmdbId());

            if (infra == null) {
                infra = createNewInfra(infraDTO, product);
            } else {
                updateExistingInfra(infra, infraDTO);
            }

            processProperties(infra, infraDTO.getProperties());
        }
        log.info("The processInfras method is completed");
    }

    private Infra createNewInfra(InfraDTO dto, Product product) {
        Infra newInfra = Infra.builder()
                .name(dto.getName())
                .type(dto.getType())
                .cmdbId(dto.getCmdbId())
                .product(product)
                .createdDate(LocalDateTime.now())
                .build();

        return infraRepository.save(newInfra);
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
            infraRepository.save(infra);
        }
    }

    private void processProperties(Infra infra, List<PropertyDTO> properties) {
        List<Property> existingProperties = propertyRepository.findByInfraId(infra.getId());
        Map<String, Property> existingPropertyMap = existingProperties.stream()
                .collect(Collectors.toMap(Property::getName, Function.identity()));

        List<String> processedKeys = properties.stream().map(PropertyDTO::getKey).toList();
        existingProperties.stream()
                .filter(property -> !processedKeys.contains(property.getName()))
                .forEach(property -> property.setDeletedDate(LocalDateTime.now()));
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

        propertyRepository.save(newProperty);
    }

    private void updateExistingProperty(Property property, PropertyDTO propDTO) {
        if (!Objects.equals(property.getValue(), propDTO.getValue())) {
            property.setValue(propDTO.getValue());
            property.setLastModifiedDate(LocalDateTime.now());
            propertyRepository.save(property);
        }
    }

    private void processRelations(List<RelationDTO> relations, Map<String, Infra> existingInfraMap) {
        for (RelationDTO relationDTO : relations) {
            Infra infra = existingInfraMap.get(relationDTO.getCmdbId());

            if (infra != null) {
                processRelation(infra, relationDTO);
            }
        }
        log.info("The processRelations method is completed");
    }

    private void processRelation(final Infra infra, RelationDTO relationDTO) {
        List<Relation> existingRelations = relationRepository.findByParentCmdbId(infra.getCmdbId());
        Set<String> processedChildrenIds = new HashSet<>(relationDTO.getChildren());

        existingRelations.stream()
                .filter(child -> !processedChildrenIds.contains(child.getChild().getCmdbId()))
                .forEach(child -> child.setDeletedDate(LocalDateTime.now()));
        relationRepository.saveAll(existingRelations);

        Map<String, Relation> existingRelationsMap = existingRelations.stream()
                .collect(Collectors.toMap(rel -> rel.getChild().getCmdbId(), Function.identity()));

        processedChildrenIds.stream()
                .filter(childId -> !existingRelationsMap.containsKey(childId))
                .forEach(childId -> createNewRelation(infra, infraRepository.findByCmdbId(childId).get()));
    }

    private void createNewRelation(Infra parentInfra, Infra childInfra) {
        Relation newRelation = Relation.builder()
                .parent(parentInfra)
                .child(childInfra)
                .createdDate(LocalDateTime.now())
                .build();

        relationRepository.save(newRelation);
    }
}