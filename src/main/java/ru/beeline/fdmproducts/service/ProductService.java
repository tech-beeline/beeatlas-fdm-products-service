package ru.beeline.fdmproducts.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.beeline.fdmlib.dto.product.GetProductTechDto;
import ru.beeline.fdmlib.dto.product.GetProductsDTO;
import ru.beeline.fdmlib.dto.product.ProductPutDto;
import ru.beeline.fdmproducts.client.CapabilityClient;
import ru.beeline.fdmproducts.client.TechradarClient;
import ru.beeline.fdmproducts.domain.*;
import ru.beeline.fdmproducts.dto.*;
import ru.beeline.fdmproducts.exception.DatabaseConnectionException;
import ru.beeline.fdmproducts.exception.EntityNotFoundException;
import ru.beeline.fdmproducts.exception.ValidationException;
import ru.beeline.fdmproducts.mapper.*;
import ru.beeline.fdmproducts.repository.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Transactional
@Service
@Slf4j
public class ProductService {

    private final ContainerMapper containerMapper;
    private final ProductTechMapper productTechMapper;
    private final InterfaceMapper interfaceMapper;
    private final OperationMapper operationMapper;
    private final SlaMapper slaMapper;
    private final ParameterMapper parameterMapper;
    private final AssessmentMapper assessmentMapper;
    private final CapabilityClient capabilityClient;
    private final TechradarClient techradarClient;
    private final UserProductRepository userProductRepository;
    private final ServiceEntityRepository serviceEntityRepository;
    private final ProductRepository productRepository;
    private final ContainerRepository containerRepository;
    private final InterfaceRepository interfaceRepository;
    private final OperationRepository operationRepository;
    private final ParameterRepository parameterRepository;
    private final SlaRepository slaRepository;
    private final TechProductRepository techProductRepository;
    private final LocalFitnessFunctionRepository fitnessFunctionRepository;
    private final LocalAssessmentRepository assessmentRepository;
    private final LocalAssessmentCheckRepository assessmentCheckRepository;
    private final EnumSourceTypeRepository enumSourceTypeRepository;
    private final PatternsAssessmentRepository patternsAssessmentRepository;
    private final PatternsCheckRepository patternsCheckRepository;
    private final DiscoveredInterfaceRepository discoveredInterfaceRepository;
    private final DiscoveredOperationRepository discoveredOperationRepository;

    public ProductService(ContainerMapper containerMapper,
                          ProductTechMapper productTechMapper,
                          InterfaceMapper interfaceMapper,
                          OperationMapper operationMapper,
                          SlaMapper slaMapper,
                          ParameterMapper parameterMapper,
                          AssessmentMapper assessmentMapper,
                          CapabilityClient capabilityClient,
                          TechradarClient techradarClient,
                          UserProductRepository userProductRepository,
                          ServiceEntityRepository serviceEntityRepository,
                          ProductRepository productRepository,
                          ContainerRepository containerRepository,
                          InterfaceRepository interfaceRepository,
                          OperationRepository operationRepository,
                          SlaRepository slaRepository,
                          ParameterRepository parameterRepository,
                          TechProductRepository techProductRepository,
                          LocalFitnessFunctionRepository fitnessFunctionRepository,
                          LocalAssessmentRepository assessmentRepository,
                          LocalAssessmentCheckRepository assessmentCheckRepository,
                          EnumSourceTypeRepository enumSourceTypeRepository,
                          PatternsAssessmentRepository patternsAssessmentRepository,
                          PatternsCheckRepository patternsCheckRepository,
                          DiscoveredInterfaceRepository discoveredInterfaceRepository,
                          DiscoveredOperationRepository discoveredOperationRepository) {
        this.containerMapper = containerMapper;
        this.productTechMapper = productTechMapper;
        this.interfaceMapper = interfaceMapper;
        this.operationMapper = operationMapper;
        this.slaMapper = slaMapper;
        this.parameterMapper = parameterMapper;
        this.assessmentMapper = assessmentMapper;
        this.capabilityClient = capabilityClient;
        this.techradarClient = techradarClient;
        this.userProductRepository = userProductRepository;
        this.serviceEntityRepository = serviceEntityRepository;
        this.productRepository = productRepository;
        this.containerRepository = containerRepository;
        this.interfaceRepository = interfaceRepository;
        this.operationRepository = operationRepository;
        this.slaRepository = slaRepository;
        this.parameterRepository = parameterRepository;
        this.techProductRepository = techProductRepository;
        this.fitnessFunctionRepository = fitnessFunctionRepository;
        this.assessmentRepository = assessmentRepository;
        this.assessmentCheckRepository = assessmentCheckRepository;
        this.enumSourceTypeRepository = enumSourceTypeRepository;
        this.patternsAssessmentRepository = patternsAssessmentRepository;
        this.patternsCheckRepository = patternsCheckRepository;
        this.discoveredInterfaceRepository = discoveredInterfaceRepository;
        this.discoveredOperationRepository = discoveredOperationRepository;
    }

    //кастыль на администратора, в хедеры вернул всепродукты
    public List<Product> getProductsByUser(Integer userId) {
        return userProductRepository.findAllByUserId(userId)
                .stream()
                .map(UserProduct::getProduct)
                .collect(Collectors.toList());
    }

    public List<Product> getProductsByUserAdmin(Integer userId, String userRoles) {
        List<String> roles = Arrays.stream(userRoles.split(","))
                .map(role -> role.replaceAll("^[^a-zA-Z]+|[^a-zA-Z]+$", ""))
                .collect(Collectors.toList());
        if (roles.contains("ADMINISTRATOR")) {
            return productRepository.findAll();
        } else {
            return userProductRepository.findAllByUserId(userId)
                    .stream()
                    .map(UserProduct::getProduct)
                    .collect(Collectors.toList());
        }
    }

    public Product getProductByCode(String code) {
        if (code == null || code.equals("\n") || code.equals(" \n")) {
            throw new IllegalArgumentException("Параметр alias не должен быть пустым.");
        }
        Product product = productRepository.findByAliasCaseInsensitive(code);
        if (product == null) {
            throw new EntityNotFoundException((String.format("Продукт c alias '%s' не найден", code)));
        }
        return product;
    }

    public ProductInfoDTO getProductInfoByCode(String code) {
        if (code == null || code.equals("\n") || code.equals(" \n")) {
            throw new IllegalArgumentException("Параметр alias не должен быть пустым.");
        }
        Product product = productRepository.findByAliasCaseInsensitive(code);
        if (product == null) {
            throw new EntityNotFoundException((String.format("Продукт c alias '%s' не найден", code)));
        }
        return productTechMapper.mapToProductInfoDTO(product);
    }

    public List<Product> findAllWithTechProductNotDeleted() {
        return productRepository.findAllWithTechProductNotDeleted();
    }

    public void createOrUpdate(ProductPutDto productPutDto, String code) {
        validateProductPutDto(productPutDto);
        Product product = productRepository.findByAliasCaseInsensitive(code);
        if (product == null) {
            product = new Product();
            product.setAlias(code);
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
        if (productPutDto.getStructurizrWorkspaceName() != null) {
            product.setStructurizrWorkspaceName(productPutDto.getStructurizrWorkspaceName());
        }
        if (productPutDto.getStructurizrApiKey() != null) {
            product.setStructurizrApiKey(productPutDto.getStructurizrApiKey());
        }
        if (productPutDto.getStructurizrApiSecret() != null) {
            product.setStructurizrApiSecret(productPutDto.getStructurizrApiSecret());
        }
        if (productPutDto.getStructurizrApiUrl() != null) {
            product.setStructurizrApiUrl(productPutDto.getStructurizrApiUrl());
        }
        productRepository.save(product);
    }

    public void patchProduct(ProductPutDto productPutDto, String code) {
        validatePatchProductPutDto(productPutDto);
        Product product = productRepository.findByAliasCaseInsensitive(code);
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
            Product product = productRepository.findByAliasCaseInsensitive(alias);
            if (product != null) {
                if (!userProductRepository.existsByUserIdAndProductId(userId, product.getId())) {
                    UserProduct userProduct = UserProduct.builder().userId(userId).product(product).build();
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
        if (productPutDto.getStructurizrWorkspaceName() == null || productPutDto.getStructurizrWorkspaceName()
                .equals("")) {
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
        return ApiSecretDTO.builder().id(product.getId()).apiSecret(product.getStructurizrApiSecret()).build();
    }

    public ApiSecretDTO getServiceSecretByApiKey(String apiKey) {
        apiKeyValidate(apiKey);
        ServiceEntity serviceEntity = serviceEntityRepository.findByApiKey(apiKey);
        if (serviceEntity == null) {
            throw new EntityNotFoundException((String.format("Продукт c api-key '%s' не найден", apiKey)));
        }
        return ApiSecretDTO.builder().id(serviceEntity.getId()).apiSecret(serviceEntity.getApiSecret()).build();
    }

    private void apiKeyValidate(String apiKey) {
        if (apiKey == null) {
            throw new IllegalArgumentException("Параметр api-key не должен быть пустым.");
        }
    }

    public void createOrUpdateProductRelations(List<ContainerDTO> containerDTOS, String code) {
        log.info("Старт метода createOrUpdateProductRelations ");
        Product product = getProductByCode(code);
        List<String> containerDtoCodes = containerDTOS.stream().map(ContainerDTO::getCode).toList();
        List<ContainerProduct> containerProducts = containerRepository.findAllByCodeIn(containerDtoCodes);
        Map<String, ContainerProduct> containerProductMap = containerProducts.stream()
                .collect(Collectors.toMap(
                        ContainerProduct::getCode,
                        containerProduct -> containerProduct
                ));
        List<ContainerProduct> saveUpdateContainers = new ArrayList<>();
        for (ContainerDTO containerDTO : containerDTOS) {
            String list = "Container";
            validateField(containerDTO.getName(), list, "name");
            validateField(containerDTO.getCode(), list, "code");
            ContainerProduct container = createOrUpdateContainer(containerDTO, product, containerProductMap.get(containerDTO.getCode()));
            saveUpdateContainers.add(container);
            Integer containerId = container.getId();
            if (containerDTO.getInterfaces() != null && !containerDTO.getInterfaces().isEmpty()) {
                List<Interface> existingOrCreatedInterface = new ArrayList<>();
                List<String> codeInterfaces = containerDTO.getInterfaces().stream().map(InterfaceDTO::getCode).toList();
                List<Interface> interfaces = interfaceRepository.findByCodeInAndContainerId(codeInterfaces, containerId);
                Map<String, Interface> interfaceMap = interfaces.stream()
                        .collect(Collectors.toMap(Interface::getCode, inter -> inter));
                for (InterfaceDTO interfaceDTO : containerDTO.getInterfaces()) {
                    log.info(" обработка interfaceDTO");
                    list = "Interface";
                    validateField(interfaceDTO.getName(), list, "name");
                    validateField(interfaceDTO.getCode(), list, "code");
                    Interface createdOrUpdatedInterface = createOrUpdateInterface(interfaceDTO, containerId,
                            interfaceMap.get(interfaceDTO.getCode()));
                    Integer interfaceId = createdOrUpdatedInterface.getId();
                    existingOrCreatedInterface.add(createdOrUpdatedInterface);
                    List<Operation> existingOrCreatedOperation = new ArrayList<>();
                    if (interfaceDTO.getMethods() != null && !interfaceDTO.getMethods().isEmpty()) {
                        List<String> methodNames = interfaceDTO.getMethods().stream()
                                .map(MethodDTO::getName)
                                .toList();
                        List<Operation> operations = operationRepository.findByNameInAndInterfaceId(methodNames, interfaceId);
                        Map<String, Operation> operationMap = operations.stream()
                                .collect(Collectors.toMap(Operation::getName, operation -> operation));
                        List<Sla> slas = new ArrayList<>();
                        for (MethodDTO methodDTO : interfaceDTO.getMethods()) {
                            list = "Method";
                            validateField(methodDTO.getName(), list, "name");
                            Operation createdOrUpdatedOperation = createOrUpdateOperation(methodDTO, interfaceId,
                                    createdOrUpdatedInterface.getTcId(), operationMap.get(methodDTO.getName()));
                            Integer operationId = createdOrUpdatedOperation.getId();
                            existingOrCreatedOperation.add(createdOrUpdatedOperation);
                            if (methodDTO.getSla() != null) {
                                slas.add(createOrUpdateSla(methodDTO, operationId));
                            }
                            List<Parameter> existingOrCreatedParameters = new ArrayList<>();
                            if (methodDTO.getParameters() != null && !methodDTO.getParameters().isEmpty()) {

                                for (ParameterDTO parameterDTO : methodDTO.getParameters()) {
                                    list = "Parameter";
                                    validateField(parameterDTO.getName(), list, "name");
                                    validateField(parameterDTO.getType(), list, "type");
                                    Parameter createdOrUpdatedParameter = createOrUpdateParameter(parameterDTO, operationId);
                                    existingOrCreatedParameters.add(createdOrUpdatedParameter);
                                }
                            }
                            slaRepository.saveAll(slas);
                            List<Parameter> allParameters = parameterRepository.findByOperationId(operationId);
                            markAsDeleted(existingOrCreatedParameters, allParameters);
                        }
                    }
                    operationRepository.saveAll(existingOrCreatedOperation);
                    List<Operation> allOperations = operationRepository.findByInterfaceIdAndDeletedDateIsNull(interfaceId);
                    markAsDeleted(existingOrCreatedOperation, allOperations);
                }
                interfaceRepository.saveAll(existingOrCreatedInterface);
                List<Interface> allInterfaces = interfaceRepository.findAllByContainerIdAndDeletedDateIsNull(containerId);
                markAsDeleted(existingOrCreatedInterface, allInterfaces);
            }
        }
        containerRepository.saveAll(saveUpdateContainers);
        log.info("метод  createOrUpdateProductRelations method завершен");
    }

    private void validateField(String fieldValue, String entityName, String fieldName) {
        if (fieldValue == null || fieldValue.isEmpty()) {
            throw new ValidationException(String.format("Отсутствует обязательное поле '%s': %s", entityName, fieldName));
        }
    }

    private ContainerProduct createOrUpdateContainer(ContainerDTO containerDTO, Product product, ContainerProduct containerProduct) {
        if (containerProduct == null) {
            log.info("Создание контейнера с code: " + containerDTO.getCode());
            ContainerProduct saveContainerProduct = containerMapper.convertToContainerProduct(containerDTO, product);
            return saveContainerProduct;
        } else {
            log.info("Обновление контейнера с code: " + containerDTO.getCode());
            if (!containerProduct.getName().equals(containerDTO.getName()) || !containerProduct.getVersion()
                    .equals(containerDTO.getVersion())) {
                containerMapper.updateContainerProduct(containerProduct, containerDTO, product);
            }
            return containerProduct;
        }
    }

    private Interface createOrUpdateInterface(InterfaceDTO interfaceDTO, Integer containerId, Interface interfaceObj) {
        if (interfaceDTO.getCapabilityCode() == null) {
            throw new IllegalArgumentException("Capability code is empty");
        }
        log.info("Запрос в /api/v1/find?search=" + interfaceDTO.getCapabilityCode());
        List<SearchCapabilityDTO> searchCapabilityDTOS = capabilityClient.getCapabilities(interfaceDTO.getCapabilityCode());
        Integer tcId = null;
        if (searchCapabilityDTOS != null && !searchCapabilityDTOS.isEmpty()) {
            SearchCapabilityDTO searchCapabilityDTO = searchCapabilityDTOS.get(0);
            if (searchCapabilityDTO.getCode().equals(interfaceDTO.getCapabilityCode())) {
                tcId = searchCapabilityDTO.getId();
            }
        }
        if (interfaceObj == null) {
            log.info("Создание интерфейса с Code: " + interfaceDTO.getCode());
            Interface newInterface = interfaceMapper.convertToInterface(interfaceDTO, containerId, tcId);
            return newInterface;
        } else {
            log.info("Обновление интерфейса с Code: " + interfaceDTO.getCode());
            if (interfaceObj.getDeletedDate() != null) {
                interfaceObj.setDeletedDate(null);
                interfaceObj.setUpdatedDate(new Date());
            }
            if (!equalsInterfaces(interfaceObj, interfaceDTO, tcId)) {
                interfaceMapper.updateInterface(interfaceObj, interfaceDTO, containerId, tcId);
            }
            return interfaceObj;
        }
    }

    private Boolean equalsInterfaces(Interface getInterface, InterfaceDTO interfaceDTO, Integer tcId) {
        return getInterface.getName().equals(interfaceDTO.getName()) && getInterface.getVersion()
                .equals(interfaceDTO.getVersion()) && getInterface.getSpecLink()
                .equals(interfaceDTO.getSpecLink()) && Objects.equals(getInterface.getTcId(),
                tcId) && getInterface.getProtocol()
                .equals(interfaceDTO.getProtocol());
    }

    private Operation createOrUpdateOperation(MethodDTO methodDTO, Integer interfaceId, Integer tcIdInterface, Operation existingOperation) {
        Integer tcId = getTCId(methodDTO.getCapabilityCode());
        if (tcId == null) {
            tcId = tcIdInterface;
        }
        if (existingOperation == null) {
            log.info("Создание метода с name: " + methodDTO.getName());
            Operation operation = operationMapper.convertToOperation(methodDTO, interfaceId, tcId);
            return operation;
        } else {
            log.info("Обновление метода с name: " + methodDTO.getName());
            Operation updateOperation = existingOperation;
            if (updateOperation.getDeletedDate() != null) {
                updateOperation.setDeletedDate(null);
                updateOperation.setUpdatedDate(new Date());
            }
            if (!methodDTO.getDescription().equals(updateOperation.getDescription()) || !methodDTO.getReturnType()
                    .equals(updateOperation.getReturnType()) || !Objects.equals(tcId, updateOperation.getTcId())) {
                operationMapper.updateOperation(updateOperation, methodDTO, tcId, interfaceId);
            }
            return updateOperation;
        }
    }

    private Integer getTCId(String code) {
        List<SearchCapabilityDTO> capabilities = capabilityClient.getCapabilities(code);
        if (!capabilities.isEmpty()) {
            SearchCapabilityDTO searchCapabilityDTO = capabilities.get(0);
            if (searchCapabilityDTO.getCode().equals(code)) {
                return searchCapabilityDTO.getId();
            }
        }
        return null;
    }

    private Sla createOrUpdateSla(MethodDTO methodDTO, Integer operationId) {
        Optional<Sla> optionalSla = slaRepository.findByOperationId(operationId);
        Sla sla;
        if (optionalSla.isEmpty()) {
            sla = slaMapper.convertToSla(methodDTO, operationId);
        } else {
            sla = optionalSla.get();
            slaMapper.updateSla(sla, methodDTO);
        }
        return sla;
    }

    private Parameter createOrUpdateParameter(ParameterDTO parameterDTO, Integer operationId) {
        Optional<Parameter> optionalParameter = parameterRepository.findByOperationIdAndParameterNameAndParameterType(
                operationId,
                parameterDTO.getName(),
                parameterDTO.getType());
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
        allEntities.stream().filter(entity -> !existingEntities.contains(entity)).forEach(entity -> {
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

    public List<GetProductTechDto> getAllProductsAndTechRelations() {
        try {
            List<TechProduct> techProducts = techProductRepository.findAll();
            Map<Integer, List<GetProductsDTO>> productsDTOByTechId = techProducts.stream()
                    .filter(techProduct -> techProduct.getProduct() != null)
                    .collect(Collectors.groupingBy(TechProduct::getTechId,
                            Collectors.mapping(techProduct -> productTechMapper.mapToGetProductsDTO(
                                    techProduct.getProduct()), Collectors.toList())));
            List<GetProductTechDto> productTechDtoList = productsDTOByTechId.entrySet()
                    .stream()
                    .map(entry -> GetProductTechDto.builder().techId(entry.getKey()).products(entry.getValue()).build())
                    .collect(Collectors.toList());
            return productTechDtoList;
        } catch (DataAccessException e) {
            throw new DatabaseConnectionException("Database is currently unavailable. Please try again later");
        } catch (Exception e) {
            throw new RuntimeException("Error processing products and tech relations");
        }
    }

    public void postFitnessFunctions(String alias,
                                     String sourceType,
                                     List<FitnessFunctionDTO> requests,
                                     Integer sourceId) {
        validateRequest(requests);
        Product product = productRepository.findByAliasCaseInsensitive(alias);
        if (product == null) {
            throw new EntityNotFoundException("Missing product");
        }
        EnumSourceType enumSourceType = enumSourceTypeRepository.findByName(sourceType)
                .orElseThrow(() -> new IllegalArgumentException("Невозможный источник."));
        if (enumSourceType.getIdentifySource() && sourceId == null) {
            throw new IllegalArgumentException("Для указанного источника обязательна передача идентификатора.");
        }
        Optional<LocalAssessment> existingAssessment = assessmentRepository.findBySourceIdAndProduct(sourceId, product);
        if (existingAssessment.isPresent()) {
            throw new IllegalArgumentException("Оценка для данного источника и продукта уже существует.");
        }
        LocalAssessment assessment = assessmentRepository.save(LocalAssessment.builder()
                .sourceId(sourceId)
                .product(product)
                .sourceTypeId(enumSourceType.getId())
                .createdTime(LocalDateTime.now())
                .build());
        requests.forEach(request -> processAssessmentCheck(request, assessment));
    }

    public AssessmentResponseDTO getFitnessFunctions(String alias, Integer sourceId, String sourceType) {
        Product product = productRepository.findByAliasCaseInsensitive(alias);
        if (product == null) {
            throw new EntityNotFoundException("Missing product");
        }
        LocalAssessment assessment;
        if (sourceType != null && !sourceType.isEmpty()) {
            EnumSourceType enumSourceType = enumSourceTypeRepository.findByName(sourceType)
                    .orElseThrow(() -> new IllegalArgumentException("Невозможный источник."));
            if (enumSourceType.getIdentifySource()) {
                if (sourceId == null) {
                    throw new IllegalArgumentException("Для указанного источника обязательна передача идентификатора.");
                } else {
                    assessment = assessmentRepository.findBySourceIdAndProductIdAndSourceTypeId(sourceId,
                                    product.getId(),
                                    enumSourceType.getId())
                            .orElseThrow(() -> new EntityNotFoundException(String.format(
                                    "Запись в таблице local_assessment с sourceId: %s, " + "SourceTypeId: %s, productId: %s не найдена",
                                    sourceId,
                                    enumSourceType.getId(),
                                    product.getId())));
                    return assessmentMapper.mapToAssessmentResponseDTO(assessment, product, sourceType);
                }
            } else {
                assessment = assessmentRepository.findLatestBySourceTypeIdAndProductId(enumSourceType.getId(),
                                product.getId())
                        .orElseThrow(() -> new EntityNotFoundException(String.format(
                                "Запись в таблице local_assessment с SourceTypeId: %s," + " productId: %s не найдена",
                                enumSourceType.getId(),
                                product.getId())));
                return assessmentMapper.mapToAssessmentResponseDTO(assessment, product, sourceType);
            }
        } else {
            List<LocalAssessment> assessments = assessmentRepository.findLatestByProductId(product.getId());
            if (assessments.isEmpty()) {
                throw new EntityNotFoundException("Assessment not found");
            }
            assessment = assessments.get(0);
        }
        return assessmentMapper.mapToAssessmentResponseDTO(assessment, product, sourceType);
    }

    private void validateRequest(List<FitnessFunctionDTO> requests) {
        boolean hasErrors = requests.stream().anyMatch(req -> req.getCode() == null || req.getIsCheck() == null);

        if (hasErrors) {
            throw new IllegalArgumentException("Missing required fields");
        }
    }

    private void processAssessmentCheck(FitnessFunctionDTO request, LocalAssessment assessment) {
        fitnessFunctionRepository.findByCode(request.getCode()).ifPresent(fitnessFunction -> {
            LocalAssessmentCheck check = new LocalAssessmentCheck();
            check.setFitnessFunction(fitnessFunction);
            check.setAssessment(assessment);
            check.setIsCheck(request.getIsCheck());
            check.setResultDetails(request.getResultDetails());
            assessmentCheckRepository.save(check);
        });
    }

    public List<String> getMnemonics() {
        return productRepository.findAllAliases();
    }

    public void postPatternProduct(String alias,
                                   String sourceType,
                                   List<PostPatternProductDTO> postPatternProductDTOS,
                                   Integer sourceId) {
        for (PostPatternProductDTO postPatternProductDTO : postPatternProductDTOS) {
            validatePostPatternProductDTO(postPatternProductDTO);
        }
        Product product = productRepository.findByAliasCaseInsensitive(alias);
        if (product == null) {
            throw new EntityNotFoundException("Указанный продукт не существует");
        }
        EnumSourceType enumSourceType = enumSourceTypeRepository.findByName(sourceType)
                .orElseThrow(() -> new IllegalArgumentException("невозможный источник"));
        if (enumSourceType.getIdentifySource() && sourceId == null) {
            throw new IllegalArgumentException("Для указанного источника обязательна передача идентификатора");
        }
        PatternsAssessment patternsAssessment = savePatternsAssessment(product.getId(), enumSourceType, sourceId);
        List<PatternsCheck> checksToSave = new ArrayList<>();
        for (PostPatternProductDTO dto : postPatternProductDTOS) {
            PatternsCheck check = PatternsCheck.builder()
                    .assessment(patternsAssessment)
                    .patternCode(dto.getCode())
                    .isCheck(dto.getIsCheck())
                    .resultDetails(dto.getResultDetails())
                    .build();
            checksToSave.add(check);
        }
        patternsCheckRepository.saveAll(checksToSave);
    }

    private void validatePostPatternProductDTO(PostPatternProductDTO dto) {
        StringBuilder errMsg = new StringBuilder();
        if (dto.getCode() == null || dto.getCode().trim().isEmpty()) {
            errMsg.append("Отсутствует обязательное поле code; ");
        }
        if (dto.getIsCheck() == null) {
            errMsg.append("Отсутствует обязательное поле isCheck; ");
        }
        if (!errMsg.toString().isEmpty()) {
            throw new ValidationException(errMsg.toString().trim());
        }
    }

    private PatternsAssessment savePatternsAssessment(Integer productId,
                                                      EnumSourceType enumSourceType,
                                                      Integer sourceId) {
        PatternsAssessment assessment = PatternsAssessment.builder()
                .productId(productId)
                .sourceType(enumSourceType)
                .sourceId(sourceId)
                .createDate(LocalDateTime.now())
                .build();
        return patternsAssessmentRepository.save(assessment);
    }

    public List<PatternDTO> getProductPatterns(String alias, Integer sourceId, String sourceType) {
        Product product = getProductByCode(alias);
        validateSourceParams(sourceId, sourceType);
        List<PatternDTO> patternDTOList = techradarClient.getPatternsAutoCheck();
        log.info("patternDTOList from techradarClient size: " + patternDTOList.size());
        if (patternDTOList.isEmpty()) {
            return Collections.emptyList();
        }
        PatternsAssessment patternsAssessment = findAssessment(product.getId(), sourceType, sourceId);
        if (patternsAssessment == null) {
            return Collections.emptyList();
        }
        List<String> patternsCheckCodes = patternsAssessment.getChecks()
                .stream()
                .map(PatternsCheck::getPatternCode)
                .toList();
        patternDTOList = patternDTOList.stream().filter(dto -> patternsCheckCodes.contains(dto.getCode())).toList();
        return patternDTOList;
    }

    private void validateSourceParams(Integer sourceId, String sourceType) {
        boolean isSourceTypeEmpty = sourceType == null || sourceType.isEmpty();
        if (sourceId != null && isSourceTypeEmpty) {
            throw new IllegalArgumentException("Не указан тип источника");
        }
        if (!isSourceTypeEmpty) {
            EnumSourceType enumSourceType = enumSourceTypeRepository.findByName(sourceType)
                    .orElseThrow(() -> new IllegalArgumentException("Указан несуществующий источник"));
            if (enumSourceType.getIdentifySource() && sourceId == null) {
                throw new IllegalArgumentException("Не передан идентификатор источника");
            }
        }
    }

    private PatternsAssessment findAssessment(Integer productId, String sourceType, Integer sourceId) {
        if (sourceType == null || sourceType.isEmpty()) {
            return patternsAssessmentRepository.findFirstByProductIdOrderByCreateDateDesc(productId).orElse(null);
        }
        if (sourceId != null) {
            return patternsAssessmentRepository.findBySourceType_NameAndSourceId(sourceType, sourceId).orElse(null);
        }
        return patternsAssessmentRepository.findFirstBySourceType_NameOrderByCreateDateDesc(sourceType).orElse(null);
    }

    public List<ProductMapicInterfaceDTO> getProductsFromMapic(String cmdb) {
        Product product = productRepository.findByAliasCaseInsensitive(cmdb);
        if (product == null) {
            throw new EntityNotFoundException("Продукт с данным cmdb не найден.");
        }
        List<ProductMapicInterfaceDTO> result = new ArrayList<>();
        List<DiscoveredInterface> discoveredInterfaces = discoveredInterfaceRepository.findAllByProduct(product);
        if (!discoveredInterfaces.isEmpty()) {
            discoveredInterfaces.forEach(discoveredInterface -> {
                ProductMapicInterfaceDTO productMapicInterfaceDTO = createProductMapicInterface(discoveredInterface);

                Interface anInterface = discoveredInterface.getConnectedInterface();
                List<DiscoveredOperation> discoveredOperations = discoveredOperationRepository.findAllByInterfaceId(
                        discoveredInterface.getId());
                if (discoveredOperations != null && !discoveredOperations.isEmpty()) {
                    productMapicInterfaceDTO.setContextProvider(discoveredOperations.get(0).getContext());
                }
                List<ConnectOperationDTO> operationDTOS = discoveredOperations.stream()
                        .filter(d -> d.getConnectionOperationId() != null)
                        .map(discoveredOperation -> {
                            log.info("connection getConnectionOperationId = {}",
                                    discoveredOperation.getConnectionOperationId());
                            Operation operation = operationRepository.getById(discoveredOperation.getConnectionOperationId());
                            log.info("operationId = {}", operation.getId());
                            return createConnectOperationDTO(operation, discoveredOperation);
                        })
                        .collect(Collectors.toList());
                productMapicInterfaceDTO.setOperations(operationDTOS);
                productMapicInterfaceDTO.setConnectInterface(createMapicInterfaceDTO(discoveredInterface, anInterface));
                result.add(productMapicInterfaceDTO);
            });
        }
        return result;
    }

    public List<ProductInterfaceDTO> getProductsFromStructurizr(String cmdb) {
        Product product = productRepository.findByAliasCaseInsensitive(cmdb);
        if (product == null) {
            throw new EntityNotFoundException("Продукт с данным cmdb не найден.");
        }
        List<ProductInterfaceDTO> result = new ArrayList<>();
        List<ContainerProduct> containerProducts = containerRepository.findAllByProductId(product.getId());
        if (!containerProducts.isEmpty()) {
            List<Integer> containerIds = containerProducts.stream().map(ContainerProduct::getId).toList();
            List<Interface> interfaces = interfaceRepository.findAllByContainerIdIn(containerIds);
            if (!interfaces.isEmpty()) {
                for (Interface interfaceObj : interfaces) {
                    ProductInterfaceDTO productInterfaceDTO = createProductInterface(interfaceObj);
                    Optional<DiscoveredInterface> dInterface = discoveredInterfaceRepository.findByConnectionInterfaceId(
                            interfaceObj.getId());
                    dInterface.ifPresent(discoveredInterface -> productInterfaceDTO.setMapicInterface(
                            createMapicInterfaceDTO(discoveredInterface)));
                    List<Operation> operations = operationRepository.findAllByInterfaceId(interfaceObj.getId());
                    if (!operations.isEmpty()) {
                        List<OperationDTO> operationDTOS = new ArrayList<>();
                        for (Operation operation : operations) {
                            operationDTOS.add(createOperationDTO(operation,
                                    discoveredOperationRepository.findByConnectionOperationId(
                                            operation.getId())));
                        }
                        productInterfaceDTO.setOperations(operationDTOS);
                    }
                    result.add(productInterfaceDTO);
                }
            }
        }
        return result;
    }

    private ProductInterfaceDTO createProductInterface(Interface interfaceObj) {
        return ProductInterfaceDTO.builder()
                .id(interfaceObj.getId())
                .name(interfaceObj.getName())
                .version(interfaceObj.getVersion())
                .description(interfaceObj.getDescription())
                .build();
    }

    private ProductMapicInterfaceDTO createProductMapicInterface(DiscoveredInterface interfaceObj) {
        return ProductMapicInterfaceDTO.builder()
                .id(interfaceObj.getId())
                .name(interfaceObj.getName())
                .version(interfaceObj.getVersion())
                .description(interfaceObj.getDescription())
                .externalId(interfaceObj.getExternalId())
                .apiId(interfaceObj.getApiId())
                .context(interfaceObj.getContext())
                .build();
    }

    private MapicInterfaceDTO createMapicInterfaceDTO(DiscoveredInterface dInterface) {
        return MapicInterfaceDTO.builder()
                .id(dInterface.getId())
                .name(dInterface.getName())
                .description(dInterface.getDescription())
                .build();
    }

    private MapicInterfaceDTO createMapicInterfaceDTO(DiscoveredInterface dInterface, Interface anInterface) {
        return MapicInterfaceDTO.builder()
                .id(dInterface.getConnectionInterfaceId())
                .name(anInterface != null ? anInterface.getName() : null)
                .description(anInterface != null ? anInterface.getDescription() : null)
                .build();
    }

    private OperationDTO createOperationDTO(Operation operation, DiscoveredOperation discoveredOperation) {
        OperationDTO operationDTO = OperationDTO.builder()
                .id(operation.getId())
                .name(operation.getName())
                .description(operation.getDescription())
                .type(operation.getType())
                .build();
        if (discoveredOperation != null) {
            MapicOperationDTO mapicOperationDTO = MapicOperationDTO.builder()
                    .id(discoveredOperation.getId())
                    .name(discoveredOperation.getName())
                    .description(discoveredOperation.getDescription())
                    .type(discoveredOperation.getType())
                    .build();
            operationDTO.setMapicOperation(mapicOperationDTO);
        }
        return operationDTO;
    }

    private ConnectOperationDTO createConnectOperationDTO(Operation operation,
                                                          DiscoveredOperation discoveredOperation) {
        ConnectOperationDTO connectOperationDTO = ConnectOperationDTO.builder()
                .id(discoveredOperation.getId())
                .name(discoveredOperation.getName())
                .description(discoveredOperation.getDescription())
                .type(discoveredOperation.getType())
                .build();
        if (discoveredOperation != null) {
            MapicOperationDTO mapicOperationDTO = MapicOperationDTO.builder()
                    .id(discoveredOperation.getConnectionOperationId())
                    .name(operation.getName())
                    .description(operation.getDescription())
                    .type(operation.getType())
                    .build();
            connectOperationDTO.setConnectOperation(mapicOperationDTO);
        }
        return connectOperationDTO;
    }

    public List<ContainerInterfacesDTO> getContainersFromStructurizr(String cmdb) {
        Product product = productRepository.findByAliasCaseInsensitive(cmdb);
        if (product == null) {
            throw new EntityNotFoundException("Продукт с данным cmdb не найден.");
        }
        List<ContainerInterfacesDTO> result = new ArrayList<>();
        List<ContainerProduct> containerProducts = containerRepository.findAllByProductId(product.getId());
        if (!containerProducts.isEmpty()) {
            for (ContainerProduct containerProduct : containerProducts) {
                result.add(ContainerInterfacesDTO.builder()
                        .id(containerProduct.getId())
                        .name(containerProduct.getName())
                        .code(containerProduct.getCode())
                        .interfaces(createInterfaceMethodDTOS(containerProduct.getId()))
                        .build());
            }
        }
        return result;
    }

    private List<InterfaceMethodDTO> createInterfaceMethodDTOS(Integer containerId) {
        List<InterfaceMethodDTO> result = new ArrayList<>();
        List<Interface> interfaces = interfaceRepository.findAllByContainerId(containerId);
        if (interfaces.isEmpty()) {
            return Collections.emptyList();
        }
        List<Integer> tcIds = interfaces.stream().map(Interface::getTcId).toList();
        List<Integer> interfaceIds = interfaces.stream().map(Interface::getId).toList();
        List<DiscoveredInterface> discoveredInterfaces = discoveredInterfaceRepository.findAllByConnectionInterfaceIdIn(interfaceIds);
        Map<Integer, DiscoveredInterface> discoveredInterfaceMap = discoveredInterfaces.stream()
                .collect(Collectors.toMap(DiscoveredInterface::getConnectionInterfaceId, obj -> obj));
        Map<Integer, TcDTO> tcDTOMap = loadTcDTOMap(tcIds);
        for (Interface interfaceObj : interfaces) {
            result.add(InterfaceMethodDTO.builder()
                    .id(interfaceObj.getId())
                    .name(interfaceObj.getName())
                    .specLink(interfaceObj.getSpecLink())
                    .protocol(interfaceObj.getProtocol())
                    .description(interfaceObj.getDescription())
                    .version(interfaceObj.getVersion())
                    .mapicInterface(createMapicInterface(discoveredInterfaceMap.get(interfaceObj.getId())))
                    .operations(createOperations(
                            operationRepository.findAllByInterfaceId(interfaceObj.getId())))
                    .techCapability(tcDTOMap.get(interfaceObj.getTcId()))
                    .build());
        }
        return result;
    }

    private Map<Integer, TcDTO> loadTcDTOMap(List<Integer> tcIds) {
        if (tcIds == null || tcIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<TcDTO> tcDtos = capabilityClient.getTcs(tcIds);
        return tcDtos.stream().collect(Collectors.toMap(TcDTO::getId, tc -> tc));
    }

    private MapicInterfaceDTO createMapicInterface(DiscoveredInterface discoveredInterface) {
        if (discoveredInterface != null) {
            return MapicInterfaceDTO.builder()
                    .id(discoveredInterface.getId())
                    .name(discoveredInterface.getName())
                    .description(discoveredInterface.getDescription())
                    .build();
        }
        return null;
    }

    private List<OperationFullDTO> createOperations(List<Operation> operations) {
        List<OperationFullDTO> result = new ArrayList<>();
        if (operations.isEmpty()) {
            return Collections.emptyList();
        }
        List<Integer> operationId = operations.stream().map(Operation::getId).toList();
        List<Integer> tcIds = operations.stream().map(Operation::getTcId).toList();
        List<Sla> slas = slaRepository.findAllByOperationIdIn(operationId);
        List<DiscoveredOperation> discoveredOperations = discoveredOperationRepository.findAllByConnectionOperationIdIn(operationId);
        Map<Integer, Sla> slaMap = slas.stream().collect(Collectors.toMap(Sla::getOperationId, sla -> sla));
        Map<Integer, TcDTO> tcDTOMap = loadTcDTOMap(tcIds);
        Map<Integer, DiscoveredOperation> discoveredOperationMap = discoveredOperations.stream()
                .collect(Collectors.toMap(DiscoveredOperation::getConnectionOperationId, obj -> obj));
        for (Operation operation : operations) {
            result.add(OperationFullDTO.builder()
                    .id(operation.getId())
                    .description(operation.getDescription())
                    .name(operation.getName())
                    .type(operation.getType())
                    .mapicOperation(createMapicOperationFullDTO(discoveredOperationMap.get(operation.getId())))
                    .sla(createSlaV2DTO(slaMap.get(operation.getId())))
                    .techCapability(tcDTOMap.get(operation.getTcId()))
                    .build());
        }
        return result;
    }

    private MapicOperationFullDTO createMapicOperationFullDTO(DiscoveredOperation discoveredOperation) {
        if (discoveredOperation != null) {
            String contextApi = null;
            Optional<DiscoveredInterface> optDiscoveredInterface = discoveredInterfaceRepository
                    .findById(discoveredOperation.getInterfaceId());
            if (optDiscoveredInterface.isPresent()) {
                contextApi = optDiscoveredInterface.get().getContext();
            }
            return MapicOperationFullDTO.builder()
                    .id(discoveredOperation.getId())
                    .name(discoveredOperation.getName())
                    .type(discoveredOperation.getType())
                    .description(discoveredOperation.getDescription())
                    .context(discoveredOperation.getContext())
                    .contextApi(contextApi)
                    .build();
        }
        return null;
    }

    private SlaV2DTO createSlaV2DTO(Sla sla) {
        if (sla != null) {
            return SlaV2DTO.builder()
                    .latency(sla.getLatency())
                    .errorRate(sla.getErrorRate())
                    .rps(sla.getRps())
                    .build();
        }
        return null;
    }

    public List<ProductInfoShortDTO> getProductInfo() {
        return productTechMapper.mapToProductInfoShortDTO(productRepository.findAll());
    }
}