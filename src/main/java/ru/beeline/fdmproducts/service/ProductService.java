package ru.beeline.fdmproducts.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.beeline.fdmlib.dto.product.ProductPutDto;
import ru.beeline.fdmproducts.client.CapabilityClient;
import ru.beeline.fdmproducts.domain.ContainerProduct;
import ru.beeline.fdmproducts.domain.Interface;
import ru.beeline.fdmproducts.domain.Operation;
import ru.beeline.fdmproducts.domain.Parameter;
import ru.beeline.fdmproducts.domain.Product;
import ru.beeline.fdmproducts.domain.ServiceEntity;
import ru.beeline.fdmproducts.domain.Sla;
import ru.beeline.fdmproducts.domain.UserProduct;
import ru.beeline.fdmproducts.dto.ApiSecretDTO;
import ru.beeline.fdmproducts.dto.ContainerDTO;
import ru.beeline.fdmproducts.dto.InterfaceDTO;
import ru.beeline.fdmproducts.dto.MethodDTO;
import ru.beeline.fdmproducts.dto.ParameterDTO;
import ru.beeline.fdmproducts.dto.SearchCapabilityDTO;
import ru.beeline.fdmproducts.exception.EntityNotFoundException;
import ru.beeline.fdmproducts.exception.ValidationException;
import ru.beeline.fdmproducts.mapper.ContainerMapper;
import ru.beeline.fdmproducts.mapper.InterfaceMapper;
import ru.beeline.fdmproducts.mapper.OperationMapper;
import ru.beeline.fdmproducts.mapper.ParameterMapper;
import ru.beeline.fdmproducts.mapper.SlaMapper;
import ru.beeline.fdmproducts.repository.ContainerRepository;
import ru.beeline.fdmproducts.repository.InterfaceRepository;
import ru.beeline.fdmproducts.repository.OperationRepository;
import ru.beeline.fdmproducts.repository.ParameterRepository;
import ru.beeline.fdmproducts.repository.ProductRepository;
import ru.beeline.fdmproducts.repository.ServiceEntityRepository;
import ru.beeline.fdmproducts.repository.SlaRepository;
import ru.beeline.fdmproducts.repository.UserProductRepository;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Transactional
@Service
@Slf4j
public class ProductService {

    @Autowired
    private ContainerMapper containerMapper;

    @Autowired
    private InterfaceMapper interfaceMapper;

    @Autowired
    private OperationMapper operationMapper;

    @Autowired
    private SlaMapper slaMapper;

    @Autowired
    private ParameterMapper parameterMapper;

    @Autowired
    private CapabilityClient capabilityClient;
    private final UserProductRepository userProductRepository;
    private final ServiceEntityRepository serviceEntityRepository;
    private final ProductRepository productRepository;
    private final ContainerRepository containerRepository;
    private final InterfaceRepository interfaceRepository;
    private final OperationRepository operationRepository;
    private final ParameterRepository parameterRepository;
    private final SlaRepository slaRepository;


    public ProductService(UserProductRepository userProductRepository, ProductRepository productRepository,
                          ServiceEntityRepository serviceEntityRepository, ContainerRepository containerRepository,
                          InterfaceRepository interfaceRepository, OperationRepository operationRepository,
                          SlaRepository slaRepository, ParameterRepository parameterRepository) {
        this.userProductRepository = userProductRepository;
        this.productRepository = productRepository;
        this.serviceEntityRepository = serviceEntityRepository;
        this.containerRepository = containerRepository;
        this.interfaceRepository = interfaceRepository;
        this.operationRepository = operationRepository;
        this.slaRepository = slaRepository;
        this.parameterRepository = parameterRepository;
    }

    public List<Product> getProductsByUser(Integer userId) {
        return userProductRepository.findAllByUserId(userId).stream().map(UserProduct::getProduct).collect(Collectors.toList());
    }

    public Product getProductByCode(String code) {
        if (code == null || code.equals("\n") || code.equals(" \n")) {
            throw new IllegalArgumentException("Параметр alias не должен быть пустым.");
        }
        Product product = productRepository.findByAlias(code);
        if (product == null) {
            throw new EntityNotFoundException((String.format("Продукт c alias '%s' не найден", code)));
        }
        return product;
    }

    public List<Product> findAll() {
        return productRepository.findAll();
    }

    public void createOrUpdate(ProductPutDto productPutDto, String code) {
        validateProductPutDto(productPutDto);
        Product product = productRepository.findByAlias(code);
        if (product == null) {
            product = new Product();
            product.setAlias(code);
            product.setName(productPutDto.getName());
            product.setDescription(productPutDto.getDescription());
            product.setGitUrl(productPutDto.getGitUrl());
        }
        if (productPutDto.getName() != null) {
            product.setName(productPutDto.getName());
        }
        if (productPutDto.getDescription() != null) {
            product.setDescription(productPutDto.getDescription());
        }
        if (productPutDto.getGitUrl() != null) {
            product.setGitUrl(productPutDto.getGitUrl());
        }

        productRepository.save(product);
    }

    public void patchProduct(ProductPutDto productPutDto, String code) {
        validatePatchProductPutDto(productPutDto);
        Product product = productRepository.findByAlias(code);
        if (product == null) {
            throw new EntityNotFoundException((String.format("404 Пользователь c alias '%s' не найден", code)));
        } else {
            product.setStructurizrWorkspaceName(productPutDto.getStructurizrWorkspaceName());
            product.setStructurizrApiKey(productPutDto.getStructurizrApiKey());
            product.setStructurizrApiSecret(productPutDto.getStructurizrApiSecret());
            product.setStructurizrApiUrl(productPutDto.getStructurizrApiUrl());
            productRepository.save(product);
        }
    }

    public void postUserProduct(List<String> aliasList, String id) {
        if (aliasList.isEmpty()) {
            throw new IllegalArgumentException("400: Массив пустой. ");
        }
        Integer userId = Integer.valueOf(id);
        List<String> notFoundAliases = new ArrayList<>();
        for (String alias : aliasList) {
            Product product = productRepository.findByAlias(alias);
            if (product != null) {
                if (!userProductRepository.existsByUserIdAndProductId(userId, product.getId())) {
                    UserProduct userProduct = UserProduct.builder()
                            .userId(userId)
                            .product(product)
                            .build();
                    userProductRepository.save(userProduct);
                }
            } else {
                notFoundAliases.add(alias);
            }
            if (notFoundAliases.size() == aliasList.size()) {
                throw new IllegalArgumentException("Ни один из продуктов не найден.");

            }
        }
    }

    public void validateProductPutDto(ProductPutDto productPutDto) {
        StringBuilder errMsg = new StringBuilder();
        if (productPutDto.getName() == null || productPutDto.getName().equals("")) {
            errMsg.append("Отсутствует обязательное поле name");
        }
        if (!errMsg.toString().isEmpty()) {
            throw new ValidationException(errMsg.toString());
        }
    }

    public void validatePatchProductPutDto(ProductPutDto productPutDto) {
        StringBuilder errMsg = new StringBuilder();
        if (productPutDto.getStructurizrWorkspaceName() == null || productPutDto.getStructurizrWorkspaceName().equals("")) {
            errMsg.append("Отсутствует обязательное поле structurizrWorkspaceName");
        }
        if (productPutDto.getStructurizrApiKey() == null || productPutDto.getStructurizrApiKey().equals("")) {
            errMsg.append("Отсутствует обязательное поле structurizrApiKey");
        }
        if (productPutDto.getStructurizrApiSecret() == null || productPutDto.getStructurizrApiSecret().equals("")) {
            errMsg.append("Отсутствует обязательное поле structurizrApiSecret");
        }
        if (productPutDto.getStructurizrApiUrl() == null || productPutDto.getStructurizrApiUrl().equals("")) {
            errMsg.append("Отсутствует обязательное поле structurizrApiUrl");
        }
        if (!errMsg.toString().isEmpty()) {
            throw new ValidationException(errMsg.toString());
        }
    }

    public ApiSecretDTO getProductByApiKey(String apiKey) {
        apiKeyValidate(apiKey);
        Product product = productRepository.findByStructurizrApiKey(apiKey);
        if (product == null) {
            throw new EntityNotFoundException((String.format("Продукт c api-key '%s' не найден", apiKey)));
        }
        return ApiSecretDTO.builder()
                .id(product.getId())
                .apiSecret(product.getStructurizrApiSecret())
                .build();
    }

    public ApiSecretDTO getServiceSecretByApiKey(String apiKey) {
        apiKeyValidate(apiKey);
        ServiceEntity serviceEntity = serviceEntityRepository.findByApiKey(apiKey);
        if (serviceEntity == null) {
            throw new EntityNotFoundException((String.format("Продукт c api-key '%s' не найден", apiKey)));
        }
        return ApiSecretDTO.builder()
                .id(serviceEntity.getId())
                .apiSecret(serviceEntity.getApiSecret())
                .build();
    }

    private void apiKeyValidate(String apiKey) {
        if (apiKey == null) {
            throw new IllegalArgumentException("Параметр api-key не должен быть пустым.");
        }
    }

    public void createOrUpdateProductRelations(List<ContainerDTO> containerDTOS, String code) {
        Product product = getProductByCode(code);
        for (ContainerDTO containerDTO : containerDTOS) {
            String list = "Container";
            validateField(containerDTO.getName(), list, "name");
            validateField(containerDTO.getCode(), list, "code");
            ContainerProduct container = createOrUpdateContainer(containerDTO, product);
            Integer containerId = container.getId();
            if (containerDTO.getInterfaces() != null && !containerDTO.getInterfaces().isEmpty()) {

                List<Interface> existingOrCreatedInterface = new ArrayList<>();
                for (InterfaceDTO interfaceDTO : containerDTO.getInterfaces()) {
                    list = "Interface";
                    validateField(interfaceDTO.getName(), list, "name");
                    validateField(interfaceDTO.getCode(), list, "code");
                    Interface createdOrUpdatedInterface = createOrUpdateInterface(interfaceDTO, containerId);
                    Integer interfaceId = createdOrUpdatedInterface.getId();
                    existingOrCreatedInterface.add(createdOrUpdatedInterface);
                    List<Operation> existingOrCreatedOperation = new ArrayList<>();
                    if (interfaceDTO.getMethods() != null) {
                        for (MethodDTO methodDTO : interfaceDTO.getMethods()) {
                            list = "Method";
                            validateField(methodDTO.getName(), list, "name");
                            Operation createdOrUpdatedOperation = createOrUpdateOperation(methodDTO, interfaceId);
                            Integer operationId = createdOrUpdatedOperation.getId();
                            existingOrCreatedOperation.add(createdOrUpdatedOperation);
                            if (methodDTO.getSla() != null) {
                                createOrUpdateSla(methodDTO, operationId);
                            }
                            List<Parameter> existingOrCreatedParameters = new ArrayList<>();
                            if (methodDTO.getParameters() != null) {
                                for (ParameterDTO parameterDTO : methodDTO.getParameters()) {
                                    list = "Parameter";
                                    validateField(parameterDTO.getName(), list, "name");
                                    validateField(parameterDTO.getType(), list, "type");
                                    Parameter createdOrUpdatedParameter = createOrUpdateParameter(parameterDTO, operationId);
                                    existingOrCreatedParameters.add(createdOrUpdatedParameter);
                                }
                            }
                            List<Parameter> allParameters = parameterRepository.findByOperationId(operationId);
                            markAsDeleted(existingOrCreatedParameters, allParameters);
                        }
                    }
                    List<Operation> allOperations = operationRepository.findByInterfaceIdAndDeletedDateIsNull(interfaceId);
                    markAsDeleted(existingOrCreatedOperation, allOperations);
                }
                List<Interface> allInterfaces = interfaceRepository.findByContainerIdAndDeletedDateIsNull(containerId);
                markAsDeleted(existingOrCreatedInterface, allInterfaces);
            }
        }
    }

    private void validateField(String fieldValue, String entityName, String fieldName) {
        if (fieldValue == null || fieldValue.isEmpty()) {
            throw new ValidationException(String.format("Отсутствует обязательное поле '%s': %s", entityName, fieldName));
        }
    }

    private ContainerProduct createOrUpdateContainer(ContainerDTO containerDTO, Product product) {
        Optional<ContainerProduct> optionalContainerProduct = containerRepository.findByCode(containerDTO.getCode());
        if (optionalContainerProduct.isEmpty()) {
            ContainerProduct containerProduct = containerMapper.convertToContainerProduct(containerDTO, product);
            containerRepository.save(containerProduct);
            return containerProduct;
        } else {
            ContainerProduct container = optionalContainerProduct.get();
            if (!container.getName().equals(containerDTO.getName()) ||
                    !container.getVersion().equals(containerDTO.getVersion())) {
                containerMapper.updateContainerProduct(container, containerDTO, product);
                containerRepository.save(container);
            }
            return container;
        }
    }

    private Interface createOrUpdateInterface(InterfaceDTO interfaceDTO, Integer containerId) {
        if (interfaceDTO.getCapabilityCode() == null) {
            throw new IllegalArgumentException("Capability code is empty");
        }
        Optional<Interface> optionalInterface = interfaceRepository.findByCodeAndContainerId(interfaceDTO.getCode(), containerId);
        List<SearchCapabilityDTO> searchCapabilityDTOS = capabilityClient.getCapabilities(interfaceDTO.getCapabilityCode());
        Integer tcId = null;
        if (!searchCapabilityDTOS.isEmpty()) {
            SearchCapabilityDTO searchCapabilityDTO = searchCapabilityDTOS.get(0);
            if (searchCapabilityDTO.getCode().equals(interfaceDTO.getCapabilityCode())) {
                tcId = searchCapabilityDTO.getId();
            }
        }
        if (optionalInterface.isEmpty()) {
            Interface newInterface = interfaceMapper.convertToInterface(interfaceDTO, containerId, tcId);
            interfaceRepository.save(newInterface);
            return newInterface;
        } else {
            Interface getInterface = optionalInterface.get();
            if (getInterface.getDeletedDate() != null) {
                getInterface.setDeletedDate(null);
                getInterface.setUpdatedDate(new Date());
                interfaceRepository.save(getInterface);
            }
            if (!equalsInterfaces(getInterface, interfaceDTO, tcId)) {
                interfaceMapper.updateInterface(getInterface, interfaceDTO, containerId, tcId);
                interfaceRepository.save(getInterface);
            }
            return getInterface;
        }
    }

    private Boolean equalsInterfaces(Interface getInterface, InterfaceDTO interfaceDTO, Integer tcId) {
        return getInterface.getName().equals(interfaceDTO.getName()) &&
                getInterface.getVersion().equals(interfaceDTO.getVersion()) &&
                getInterface.getSpecLink().equals(interfaceDTO.getSpecLink()) &&
                Objects.equals(getInterface.getTcId(), tcId) &&
                getInterface.getProtocol().equals(interfaceDTO.getProtocol());
    }

    private Operation createOrUpdateOperation(MethodDTO methodDTO, Integer interfaceId) {
        Optional<Operation> optionalOperation = operationRepository.findByNameAndInterfaceId(methodDTO.getName(), interfaceId);
        if (optionalOperation.isEmpty()) {
            Operation operation = operationMapper.convertToOperation(methodDTO, interfaceId);
            operationRepository.save(operation);
            return operation;
        } else {
            Operation updateOperation = optionalOperation.get();
            if (updateOperation.getDeletedDate() != null) {
                updateOperation.setDeletedDate(null);
                updateOperation.setUpdatedDate(new Date());
                operationRepository.save(updateOperation);
            }
            if (!methodDTO.getDescription().equals(updateOperation.getDescription()) ||
                    !methodDTO.getReturnType().equals(updateOperation.getReturnType())) {
                operationMapper.updateOperation(updateOperation, methodDTO);
                operationRepository.save(updateOperation);
            }
            return updateOperation;
        }
    }

    private void createOrUpdateSla(MethodDTO methodDTO, Integer operationId) {
        Optional<Sla> optionalSla = slaRepository.findByOperationId(operationId);
        Sla sla;
        if (optionalSla.isEmpty()) {
            sla = slaMapper.convertToSla(methodDTO, operationId);
        } else {
            sla = optionalSla.get();
            slaMapper.updateSla(sla, methodDTO);
        }
        slaRepository.save(sla);
    }

    private Parameter createOrUpdateParameter(ParameterDTO parameterDTO, Integer operationId) {
        Optional<Parameter> optionalParameter =
                parameterRepository.findByOperationIdAndParameterNameAndParameterType(operationId,
                        parameterDTO.getName(), parameterDTO.getType());
        if (optionalParameter.isEmpty()) {
            Parameter parameter = parameterMapper.convertToParameter(parameterDTO, operationId);
            parameterRepository.save(parameter);
            return parameter;
        } else {
            Parameter updateParameter = optionalParameter.get();
            updateParameter.setDeletedDate(null);
            parameterRepository.save(updateParameter);
            return optionalParameter.get();
        }
    }

    private void markAsDeleted(List<?> existingEntities, List<?> allEntities) {
        List<Parameter> parametersToDelete = new ArrayList<>();
        List<Operation> operationsToDelete = new ArrayList<>();
        List<Interface> interfacesToDelete = new ArrayList<>();
        allEntities.stream()
                .filter(entity -> !existingEntities.contains(entity))
                .forEach(entity -> {
                    if (entity instanceof Parameter) {
                        parametersToDelete.add((Parameter) entity);
                    } else if (entity instanceof Operation) {
                        operationsToDelete.add((Operation) entity);
                    } else if (entity instanceof Interface) {
                        interfacesToDelete.add((Interface) entity);
                    }
                });
        if (!parametersToDelete.isEmpty()) {
            parametersToDelete.forEach(parameter -> parameter.setDeletedDate(new Date()));
            parameterRepository.saveAll(parametersToDelete);
        }
        if (!operationsToDelete.isEmpty()) {
            operationsToDelete.forEach(operation -> operation.setDeletedDate(new Date()));
            operationRepository.saveAll(operationsToDelete);
        }
        if (!interfacesToDelete.isEmpty()) {
            interfacesToDelete.forEach(interfaceEntity -> interfaceEntity.setDeletedDate(new Date()));
            interfaceRepository.saveAll(interfacesToDelete);
        }
    }
}