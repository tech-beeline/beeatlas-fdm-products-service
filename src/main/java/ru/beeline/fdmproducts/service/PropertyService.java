package ru.beeline.fdmproducts.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.beeline.fdmproducts.domain.Infra;
import ru.beeline.fdmproducts.domain.Property;
import ru.beeline.fdmproducts.exception.EntityNotFoundException;
import ru.beeline.fdmproducts.repository.InfraRepository;
import ru.beeline.fdmproducts.repository.PropertyRepository;

import java.time.LocalDateTime;

@Transactional
@Service
@Slf4j
public class PropertyService {
    @Autowired
    private InfraRepository infraRepository;
    @Autowired
    private PropertyRepository propertyRepository;

    public Property addProperty(Integer infraId, String name, String value) {
        Infra infra = infraRepository.findById(infraId)
                .orElseThrow(() -> new EntityNotFoundException("Infra not found"));

        Property property = Property.builder()
                .infra(infra)
                .name(name)
                .value(value)
                .createdDate(LocalDateTime.now())
                .build();

        return propertyRepository.save(property);
    }
}
