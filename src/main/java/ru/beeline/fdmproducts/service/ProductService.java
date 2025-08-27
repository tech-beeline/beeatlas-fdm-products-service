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
    private final FitnessFunctionMapper fitnessFunctionMapper;
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
    private final DiscoveredInterfaceMapper discoveredInterfaceMapper;
    private final DiscoveredInterfaceRepository discoveredInterfaceRepository;
    private final DiscoveredOperationRepository discoveredOperationRepository;

    public ProductService(ContainerMapper containerMapper,
                          ProductTechMapper productTechMapper,
                          InterfaceMapper interfaceMapper,
                          OperationMapper operationMapper,
                          SlaMapper slaMapper,
                          ParameterMapper parameterMapper,
                          AssessmentMapper assessmentMapper,
                          FitnessFunctionMapper fitnessFunctionMapper,
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
                          DiscoveredInterfaceMapper discoveredInterfaceMapper,
                          DiscoveredInterfaceRepository discoveredInterfaceRepository,
                          DiscoveredOperationRepository discoveredOperationRepository) {
        this.containerMapper = containerMapper;
        this.productTechMapper = productTechMapper;
        this.interfaceMapper = interfaceMapper;
        this.operationMapper = operationMapper;
        this.slaMapper = slaMapper;
        this.parameterMapper = parameterMapper;
        this.assessmentMapper = assessmentMapper;
        this.fitnessFunctionMapper = fitnessFunctionMapper;
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
        this.discoveredInterfaceMapper = discoveredInterfaceMapper;
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
                                    Parameter createdOrUpdatedParameter = createOrUpdateParameter(parameterDTO,
                                            operationId);
                                    existingOrCreatedParameters.add(createdOrUpdatedParameter);
                                }
                            }
                            List<Parameter> allParameters = parameterRepository.findByOperationId(operationId);
                            markAsDeleted(existingOrCreatedParameters, allParameters);
                        }
                    }
                    List<Operation> allOperations = operationRepository.findByInterfaceIdAndDeletedDateIsNull(
                            interfaceId);
                    markAsDeleted(existingOrCreatedOperation, allOperations);
                }
                List<Interface> allInterfaces = interfaceRepository.findAllByContainerIdAndDeletedDateIsNull(containerId);
                markAsDeleted(existingOrCreatedInterface, allInterfaces);
            }
        }
    }

    private void validateField(String fieldValue, String entityName, String fieldName) {
        if (fieldValue == null || fieldValue.isEmpty()) {
            throw new ValidationException(String.format("Отсутствует обязательное поле '%s': %s",
                    entityName,
                    fieldName));
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
            if (!container.getName().equals(containerDTO.getName()) || !container.getVersion()
                    .equals(containerDTO.getVersion())) {
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
        Optional<Interface> optionalInterface = interfaceRepository.findByCodeAndContainerId(interfaceDTO.getCode(),
                containerId);
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
        return getInterface.getName().equals(interfaceDTO.getName()) && getInterface.getVersion()
                .equals(interfaceDTO.getVersion()) && getInterface.getSpecLink()
                .equals(interfaceDTO.getSpecLink()) && Objects.equals(getInterface.getTcId(),
                tcId) && getInterface.getProtocol()
                .equals(interfaceDTO.getProtocol());
    }

    private Operation createOrUpdateOperation(MethodDTO methodDTO, Integer interfaceId) {
        Optional<Operation> optionalOperation = operationRepository.findByNameAndInterfaceId(methodDTO.getName(),
                interfaceId);
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
            if (!methodDTO.getDescription().equals(updateOperation.getDescription()) || !methodDTO.getReturnType()
                    .equals(updateOperation.getReturnType())) {
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
                List<ConnectOperationDTO> operationDTOS = discoveredOperations.stream()
                        .filter(d -> d.getConnectionOperationId() != null)
                        .map(discoveredOperation -> {
                            log.info("connection getConnectionOperationId = {}", discoveredOperation.getConnectionOperationId());
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
                    Optional<DiscoveredInterface> dInterface = discoveredInterfaceRepository.findByConnectionInterfaceId(interfaceObj.getId());
                    dInterface.ifPresent(discoveredInterface -> productInterfaceDTO.setMapicInterface(createMapicInterfaceDTO(discoveredInterface)));
                    List<Operation> operations = operationRepository.findAllByInterfaceId(interfaceObj.getId());
                    if (!operations.isEmpty()) {
                        List<OperationDTO> operationDTOS = new ArrayList<>();
                        for (Operation operation : operations) {
                            operationDTOS.add(createOperationDTO(operation, discoveredOperationRepository
                                    .findByConnectionOperationId(operation.getId())));
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

    private ConnectOperationDTO createConnectOperationDTO(Operation operation, DiscoveredOperation discoveredOperation) {
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
}