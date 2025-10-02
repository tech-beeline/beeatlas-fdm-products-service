package ru.beeline.fdmproducts.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.beeline.fdmlib.dto.graph.ProductInfluenceDTO;
import ru.beeline.fdmlib.dto.product.GetProductTechDto;
import ru.beeline.fdmlib.dto.product.GetProductsByIdsDTO;
import ru.beeline.fdmlib.dto.product.GetProductsDTO;
import ru.beeline.fdmlib.dto.product.ProductPutDto;
import ru.beeline.fdmproducts.client.CapabilityClient;
import ru.beeline.fdmproducts.client.GraphClient;
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
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    private final GraphClient graphClient;
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
                          GraphClient graphClient,
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
        this.graphClient = graphClient;
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

    public ValidationErrorResponse createOrUpdateProductRelations(List<ContainerDTO> containerDTOS,
                                                                  String code,
                                                                  String source) {
        log.info("Старт метода createOrUpdateProductRelations");
        ValidationErrorResponse errorEntity = new ValidationErrorResponse();
        validateContainers(containerDTOS, errorEntity);
        validateInterfaces(containerDTOS, errorEntity);
        validateMethods(containerDTOS, errorEntity);
        if (!containerDTOS.isEmpty()) {
            saveRelations(containerDTOS, code);
        }
        Product product = getProductByCode(code);
        product.setSource(source);
        product.setUploadDate(LocalDateTime.now());
        productRepository.save(product);
        return errorEntity;
    }

    private void validateContainers(List<ContainerDTO> containers, ValidationErrorResponse errorEntity) {
        Map<String, Long> codeCounts = containers.stream()
                .filter(c -> c.getCode() != null)
                .collect(Collectors.groupingBy(ContainerDTO::getCode, Collectors.counting()));
        Set<String> duplicates = codeCounts.entrySet()
                .stream()
                .filter(e -> e.getValue() > 1)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
        if (!duplicates.isEmpty()) {
            errorEntity.getContainerError().add("Контейнеры имеют одинаковый code: " + String.join(", ", duplicates));
        }
        containers.removeIf(c -> c.getCode() != null && duplicates.contains(c.getCode()));
        Iterator<ContainerDTO> it = containers.iterator();
        while (it.hasNext()) {
            ContainerDTO c = it.next();
            boolean invalid = false;
            if ((c.getName() == null || c.getName().trim().isEmpty()) && (c.getCode() == null || c.getCode()
                    .trim()
                    .isEmpty())) {
                errorEntity.getContainerError().add("Для контейнера не заполнен атрибут name и атрибут code");
                invalid = true;
            } else if (c.getName() == null || c.getName().trim().isEmpty()) {
                errorEntity.getContainerError()
                        .add("Для контейнера с кодом " + c.getCode() + " не заполнен атрибут name");
                invalid = true;
            } else if (c.getCode() == null || c.getCode().trim().isEmpty()) {
                errorEntity.getContainerError()
                        .add("Для контейнера с именем " + c.getName() + " не заполнен атрибут code");
                invalid = true;
            }
            if (invalid) {
                it.remove();
            }
        }
    }

    private void validateInterfaces(List<ContainerDTO> containers, ValidationErrorResponse errorEntity) {
        for (ContainerDTO container : containers) {
            if (container.getInterfaces() == null)
                continue;
            Map<String, Long> codeCounts = container.getInterfaces()
                    .stream()
                    .filter(i -> i.getCode() != null)
                    .collect(Collectors.groupingBy(InterfaceDTO::getCode, Collectors.counting()));
            Set<String> duplicates = codeCounts.entrySet()
                    .stream()
                    .filter(e -> e.getValue() > 1)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toSet());
            if (!duplicates.isEmpty()) {
                errorEntity.getInterfaceError()
                        .add("Интерфейсы имеют одинаковый code: " + String.join(", ", duplicates));
            }
            container.getInterfaces().removeIf(i -> i.getCode() != null && duplicates.contains(i.getCode()));
            Iterator<InterfaceDTO> it = container.getInterfaces().iterator();
            while (it.hasNext()) {
                InterfaceDTO iface = it.next();
                boolean invalid = false;
                if (iface.getName() == null || iface.getName().trim().isEmpty()) {
                    errorEntity.getInterfaceError()
                            .add("Для интерфейса с кодом " + iface.getCode() + " не заполнен атрибут name");
                    invalid = true;
                }
                if (iface.getCode() == null || iface.getCode().trim().isEmpty()) {
                    errorEntity.getInterfaceError()
                            .add("Для интерфейса с именем " + iface.getName() + " не заполнен атрибут code");
                    invalid = true;
                }
                if (invalid) {
                    it.remove();
                }
            }
        }
    }

    private void validateMethods(List<ContainerDTO> containers, ValidationErrorResponse errorEntity) {
        for (ContainerDTO container : containers) {
            if (container.getInterfaces() == null)
                continue;
            for (InterfaceDTO iface : container.getInterfaces()) {
                if (iface.getMethods() == null)
                    continue;
                Map<String, Long> nameCounts = iface.getMethods()
                        .stream()
                        .filter(m -> m.getName() != null)
                        .collect(Collectors.groupingBy(MethodDTO::getName, Collectors.counting()));
                Set<String> duplicates = nameCounts.entrySet()
                        .stream()
                        .filter(e -> e.getValue() > 1)
                        .map(Map.Entry::getKey)
                        .collect(Collectors.toSet());
                if (!duplicates.isEmpty()) {
                    errorEntity.getMethodError().add("Есть методы с одинаковым name: " + String.join(", ", duplicates));
                }
                Iterator<MethodDTO> it = iface.getMethods().iterator();
                while (it.hasNext()) {
                    MethodDTO method = it.next();
                    boolean invalid = false;
                    if (method.getName() == null || method.getName().trim().isEmpty()) {
                        errorEntity.getMethodError().add("Есть методы с незаполенным name");
                        invalid = true;
                    }
                    if (method.getName() != null && duplicates.contains(method.getName())) {
                        invalid = true;
                    }
                    validateMethodParameters(method, errorEntity);
                    if (invalid) {
                        it.remove();
                    } else {
                        enrichMethodType(iface, method);
                    }
                }
            }
        }
    }

    private void validateMethodParameters(MethodDTO method, ValidationErrorResponse errorEntity) {
        if (method.getParameters() == null)
            return;
        Iterator<ParameterDTO> itParam = method.getParameters().iterator();
        while (itParam.hasNext()) {
            ParameterDTO param = itParam.next();
            if (param.getName() == null || param.getName().trim().isEmpty()) {
                errorEntity.getParameterError()
                        .add("Для Parameters метода " + method.getName() + " не заполнен атрибут name");
                itParam.remove();
            }
        }
    }

    private void enrichMethodType(InterfaceDTO iface, MethodDTO method) {
        String protocol = iface.getProtocol();
        if (protocol == null)
            return;
        switch (protocol.toLowerCase()) {
            case "rest" -> {
                if (method.getName().contains(" ")) {
                    String[] parts = method.getName().split(" ", 2);
                    method.setType(parts[0]);
                    method.setName(parts[1]);
                } else {
                    method.setType(null);
                }
            }
            case "soap" -> method.setType("SOAP");
            case "grpc" -> method.setType("gRPC");
        }
    }

    public void saveRelations(List<ContainerDTO> containerDTOS, String code) {
        log.info("Обработка контейнеров продукта с code: " + code);
        Product product = getProductByCode(code);
        Map<String, ContainerProduct> existingContainers = containerRepository
                .findAllByCodeInAndProductId(containerDTOS.stream().map(ContainerDTO::getCode).toList(), product.getId())
                .stream()
                .collect(Collectors.toMap(ContainerProduct::getCode, c -> c));
        List<ContainerProduct> toSave = new ArrayList<>();
        Map<String, List<InterfaceDTO>> interfacesByCode = new HashMap<>();
        List<InterfaceDTO> allInterfaces = new ArrayList<>();
        List<MethodDTO> allMethods = new ArrayList<>();
        prepareContainersAndCollectData(containerDTOS,
                                        product,
                                        existingContainers,
                                        toSave,
                                        interfacesByCode,
                                        allInterfaces,
                                        allMethods);
        if (!toSave.isEmpty()) {
            log.info("Сохранение контейнеров. Количество: " + toSave.size());
            containerRepository.saveAll(toSave);
        }
        Map<String, Long> codesIdMap = loadInterfaceCapabilityMap(allInterfaces);
        Map<String, Long> methodCodesIdMap = loadMethodCapabilityMap(allMethods);
        Map<Integer, List<InterfaceDTO>> containerInterfaces = buildContainerInterfacesMap(existingContainers,
                                                                                           toSave,
                                                                                           interfacesByCode);
        for (Map.Entry<Integer, List<InterfaceDTO>> entry : containerInterfaces.entrySet()) {
            processInterfaces(entry.getValue(), entry.getKey(), codesIdMap, methodCodesIdMap);
            markInterfacesAsDeleted(entry.getKey(), entry.getValue());
            List<Interface> allDbInterfaces = interfaceRepository.findAllByContainerId(entry.getKey());
            for (Interface dbInterface : allDbInterfaces) {
                if (dbInterface.getDeletedDate() != null) {
                    cascadeDeleteInterface(dbInterface);
                }
            }
        }
    }

    private void prepareContainersAndCollectData(List<ContainerDTO> containerDTOS,
                                                 Product product,
                                                 Map<String, ContainerProduct> existingContainers,
                                                 List<ContainerProduct> toSave,
                                                 Map<String, List<InterfaceDTO>> interfacesByCode,
                                                 List<InterfaceDTO> allInterfaces,
                                                 List<MethodDTO> allMethods) {
        String container = "Container";
        for (ContainerDTO dto : containerDTOS) {
            validateField(dto.getName(), container, "name");
            validateField(dto.getCode(), container, "code");
            ContainerProduct containerEntity = existingContainers.get(dto.getCode());
            if (containerEntity == null) {
                containerEntity = containerMapper.convertToContainerProduct(dto, product);
                toSave.add(containerEntity);
            } else {
                if (!Objects.equals(containerEntity.getName(),
                                    dto.getName()) || !Objects.equals(containerEntity.getVersion(), dto.getVersion())) {
                    containerMapper.updateContainerProduct(containerEntity, dto, product);
                    toSave.add(containerEntity);
                }
            }
            List<InterfaceDTO> dtoInterfaces = dto.getInterfaces() != null ? dto.getInterfaces() : Collections.emptyList();
            log.info("В контейнере " + dto.getCode() + " Интерфесов: " + dtoInterfaces.size());
            interfacesByCode.put(dto.getCode(), dtoInterfaces);
            allInterfaces.addAll(dtoInterfaces);
            dtoInterfaces.stream()
                    .filter(dinterface -> dinterface.getMethods() != null)
                    .forEach(dinterface -> allMethods.addAll(dinterface.getMethods()));
        }
    }

    private Map<String, Long> loadInterfaceCapabilityMap(List<InterfaceDTO> allInterfaces) {
        List<String> allInterfaceCodes = allInterfaces.stream().map(InterfaceDTO::getCapabilityCode).toList();
        return capabilityClient.getIdCodes(allInterfaceCodes)
                .stream()
                .collect(Collectors.toMap(IdCodeDTO::getCode, IdCodeDTO::getId));
    }

    private Map<String, Long> loadMethodCapabilityMap(List<MethodDTO> allMethods) {
        List<String> allMethodCodes = allMethods.stream()
                .map(MethodDTO::getCapabilityCode)
                .filter(Objects::nonNull)
                .toList();
        return capabilityClient.getIdCodes(allMethodCodes)
                .stream()
                .collect(Collectors.toMap(IdCodeDTO::getCode, IdCodeDTO::getId));
    }

    private Map<Integer, List<InterfaceDTO>> buildContainerInterfacesMap(Map<String, ContainerProduct> existingContainers,
                                                                         List<ContainerProduct> toSave,
                                                                         Map<String, List<InterfaceDTO>> interfacesByCode) {
        Map<Integer, List<InterfaceDTO>> containerInterfaces = new HashMap<>();
        for (ContainerProduct cp : existingContainers.values()) {
            containerInterfaces.put(cp.getId(), interfacesByCode.getOrDefault(cp.getCode(), Collections.emptyList()));
        }
        for (ContainerProduct cp : toSave) {
            containerInterfaces.put(cp.getId(), interfacesByCode.getOrDefault(cp.getCode(), Collections.emptyList()));
        }
        return containerInterfaces;
    }

    private List<Interface> processInterfaces(List<InterfaceDTO> interfaces,
                                              Integer containerId,
                                              Map<String, Long> codesIdMap,
                                              Map<String, Long> methodCodesIdMap) {
        String method = "Interface";
        if (interfaces == null || interfaces.isEmpty()) {
            markInterfacesAsDeleted(containerId, Collections.emptyList());
            return Collections.emptyList();
        }
        List<String> codes = interfaces.stream().map(InterfaceDTO::getCode).toList();
        Map<String, Interface> existingInterfaces = interfaceRepository.findAllByContainerIdAndCodeIn(containerId,
                                                                                                      codes)
                .stream()
                .collect(Collectors.toMap(Interface::getCode, i -> i));
        List<Interface> toSave = new ArrayList<>();
        List<Interface> result = new ArrayList<>();
        for (InterfaceDTO dto : interfaces) {
            Interface interfaceObj = createOrUpdateInterfaceObject(dto,
                                                                   containerId,
                                                                   codesIdMap,
                                                                   existingInterfaces,
                                                                   toSave,
                                                                   method);
            result.add(interfaceObj);
        }
        if (!toSave.isEmpty()) {
            interfaceRepository.saveAll(toSave);
        }
        for (int i = 0; i < interfaces.size(); i++) {
            processInterfaceMethods(interfaces.get(i), result.get(i), methodCodesIdMap);
        }
        markInterfacesAsDeleted(containerId, interfaces);
        return result;
    }

    private void markInterfacesAsDeleted(Integer containerId, List<InterfaceDTO> newInterfaces) {
        List<Interface> allDbInterfaces = interfaceRepository.findAllByContainerId(containerId);
        Set<String> dtoCodes = newInterfaces.stream().map(InterfaceDTO::getCode).collect(Collectors.toSet());
        Date now = new Date();
        List<Interface> toDelete = allDbInterfaces.stream()
                .filter(dbIntf -> !dtoCodes.contains(dbIntf.getCode()))
                .filter(dbIntf -> dbIntf.getDeletedDate() == null)
                .peek(dbIntf -> dbIntf.setDeletedDate(now))
                .toList();
        if (!toDelete.isEmpty()) {
            interfaceRepository.saveAll(toDelete);
        }
    }

    private void processInterfaceMethods(InterfaceDTO dto, Interface interfaceObj, Map<String, Long> methodCodesIdMap) {
        List<MethodDTO> methods = dto.getMethods();
        if (methods == null || methods.isEmpty()) {
            markOperationsAsDeleted(interfaceObj.getId(), Collections.emptyList());
            return;
        }
        List<String> keys = methods.stream()
                .map(m -> m.getName() + "::" + (m.getType() != null ? m.getType() : ""))
                .toList();
        List<Operation> dbOperations = operationRepository.findAllByInterfaceId(interfaceObj.getId());
        Map<String, Operation> operationMap = dbOperations.stream()
                .filter(op -> keys.contains(op.getName() + "::" + (op.getType() != null ? op.getType() : "")))
                .collect(Collectors.toMap(operation -> operation.getName() + "::" + (operation.getType() != null ? operation.getType() : ""),
                                          operation -> operation));
        processMethods(methods, interfaceObj.getId(), interfaceObj.getTcId(), operationMap, methodCodesIdMap);
        markOperationsAsDeleted(interfaceObj.getId(), methods);
    }

    private void markOperationsAsDeleted(Integer interfaceId, List<MethodDTO> newMethods) {
        List<Operation> allDbOperations = operationRepository.findAllByInterfaceId(interfaceId);
        Set<String> newKeys = newMethods.stream()
                .map(m -> m.getName() + "::" + (m.getType() != null ? m.getType() : ""))
                .collect(Collectors.toSet());
        Date now = new Date();
        List<Operation> toDelete = new ArrayList<>();
        for (Operation op : allDbOperations) {
            String key = op.getName() + "::" + (op.getType() != null ? op.getType() : "");
            if (!newKeys.contains(key) && op.getDeletedDate() == null) {
                op.setDeletedDate(now);
                toDelete.add(op);
            }
        }
        if (!toDelete.isEmpty()) {
            operationRepository.saveAll(toDelete);
        }
    }

    private void cascadeDeleteInterface(Interface interfaceObj) {
        List<Operation> ops = operationRepository.findAllByInterfaceId(interfaceObj.getId());
        if (!ops.isEmpty()) {
            Date now = new Date();
            for (Operation op : ops) {
                if (op.getDeletedDate() == null) {
                    op.setDeletedDate(now);
                }
            }
            operationRepository.saveAll(ops);
            List<Integer> opIds = ops.stream().map(Operation::getId).toList();
            Map<Integer, List<Parameter>> params = loadParameters(opIds);
            params.values().forEach(paramList -> paramList.forEach(p -> {
                if (p.getDeletedDate() == null) {
                    p.setDeletedDate(now);
                }
            }));
            params.values().forEach(parameterRepository::saveAll);
        }
    }

    private Interface createOrUpdateInterfaceObject(InterfaceDTO dto,
                                                    Integer containerId,
                                                    Map<String, Long> codesIdMap,
                                                    Map<String, Interface> existingInterfaces,
                                                    List<Interface> toSave,
                                                    String method) {
        validateField(dto.getName(), method, "name");
        validateField(dto.getCode(), method, "code");
        if (dto.getCapabilityCode() == null) {
            throw new IllegalArgumentException("Capability code is empty");
        }
        Integer tcId = codesIdMap.get(dto.getCapabilityCode()) != null ? codesIdMap.get(dto.getCapabilityCode())
                .intValue() : null;
        Interface interfaceObj = existingInterfaces.get(dto.getCode());
        if (interfaceObj == null) {
            interfaceObj = interfaceMapper.convertToInterface(dto, containerId, tcId);
            toSave.add(interfaceObj);
        } else {
            if (interfaceObj.getDeletedDate() != null) {
                interfaceObj.setDeletedDate(null);
                interfaceObj.setUpdatedDate(new Date());
                toSave.add(interfaceObj);
            }
            if (!equalsInterfaces(interfaceObj, dto, tcId)) {
                interfaceMapper.updateInterface(interfaceObj, dto, containerId, tcId);
                toSave.add(interfaceObj);
            }
        }
        return interfaceObj;
    }

    private List<Operation> processMethods(List<MethodDTO> methods,
                                           Integer interfaceId,
                                           Integer tcIdInterface,
                                           Map<String, Operation> operationMap,
                                           Map<String, Long> methodCodesIdMap) {
        List<Operation> operations = createOrUpdateOperations(methods,
                                                              interfaceId,
                                                              tcIdInterface,
                                                              operationMap,
                                                              methodCodesIdMap);
        List<Integer> operationIds = operations.stream().map(Operation::getId).toList();
        Map<Integer, Sla> slaMap = loadSla(operationIds);
        Map<Integer, List<Parameter>> paramsByOperation = loadParameters(operationIds);
        processSlaAndParameters(methods, operations, slaMap, paramsByOperation);
        return operations;
    }

    private List<Operation> createOrUpdateOperations(List<MethodDTO> methods,
                                                     Integer interfaceId,
                                                     Integer tcIdInterface,
                                                     Map<String, Operation> operationMap,
                                                     Map<String, Long> methodCodesIdMap) {
        List<Operation> operations = new ArrayList<>();
        List<Operation> operationsToSave = new ArrayList<>();
        for (MethodDTO dto : methods) {
            String key = dto.getName() + "::" + (dto.getType() != null ? dto.getType() : "");
            Operation operation = createOrUpdateMethod(dto,
                                                       interfaceId,
                                                       tcIdInterface,
                                                       operationMap.get(key),
                                                       operationsToSave,
                                                       methodCodesIdMap);
            operations.add(operation);
        }
        if (!operationsToSave.isEmpty()) {
            operationRepository.saveAll(operationsToSave);
        }
        return operations;
    }

    private Map<Integer, Sla> loadSla(List<Integer> operationIds) {
        return slaRepository.findAllByOperationIdIn(operationIds)
                .stream()
                .collect(Collectors.toMap(Sla::getOperationId, Function.identity()));
    }

    private Map<Integer, List<Parameter>> loadParameters(List<Integer> operationIds) {
        return parameterRepository.findByOperationIdIn(operationIds)
                .stream()
                .collect(Collectors.groupingBy(Parameter::getOperationId));
    }

    private void processSlaAndParameters(List<MethodDTO> methods,
                                         List<Operation> operations,
                                         Map<Integer, Sla> slaMap,
                                         Map<Integer, List<Parameter>> paramsByOperation) {
        List<Sla> slaToSave = new ArrayList<>();
        List<Parameter> parametersToSave = new ArrayList<>();
        for (int i = 0; i < methods.size(); i++) {
            MethodDTO dto = methods.get(i);
            Operation op = operations.get(i);
            if (dto.getSla() != null) {
                Sla sla = slaMap.get(op.getId());
                if (sla == null) {
                    sla = slaMapper.convertToSla(dto, op.getId());
                } else {
                    slaMapper.updateSla(sla, dto);
                }
                slaToSave.add(sla);
            }
            List<Parameter> allParameters = paramsByOperation.getOrDefault(op.getId(), List.of());
            if (dto.getParameters() != null && !dto.getParameters().isEmpty()) {
                List<Parameter> existingOrCreated = processParameters(dto.getParameters(), op.getId());
                parametersToSave.addAll(existingOrCreated);
                markParametersAsDeletedIfMissing(existingOrCreated, allParameters);
            } else {
                markParametersAsDeletedIfMissing(Collections.emptyList(), allParameters);
            }
        }
        if (!slaToSave.isEmpty()) {
            slaRepository.saveAll(slaToSave);
        }
        if (!parametersToSave.isEmpty()) {
            parameterRepository.saveAll(parametersToSave);
        }
    }

    private void markParametersAsDeletedIfMissing(List<Parameter> existingOrCreated, List<Parameter> allParameters) {
        Date now = new Date();
        List<Parameter> toDelete = allParameters.stream()
                .filter(p -> existingOrCreated.stream()
                        .noneMatch(e -> e.getParameterName().equals(p.getParameterName()) && e.getParameterType()
                                .equals(p.getParameterType())))
                .filter(p -> p.getDeletedDate() == null)
                .toList();
        toDelete.forEach(p -> p.setDeletedDate(now));
        if (!toDelete.isEmpty()) {
            parameterRepository.saveAll(toDelete);
        }
    }

    private Operation createOrUpdateMethod(MethodDTO methodDTO,
                                           Integer interfaceId,
                                           Integer tcIdInterface,
                                           Operation existingOperation,
                                           List<Operation> operationsToSave,
                                           Map<String, Long> methodCodesIdMap) {
        Integer tcId = null;
        if (methodDTO.getCapabilityCode() != null) {
            Long tcIdLong = methodCodesIdMap.get(methodDTO.getCapabilityCode());
            tcId = (tcIdLong != null) ? tcIdLong.intValue() : null;
        }
        if (tcId == null) {
            tcId = tcIdInterface;
        }
        if (existingOperation == null) {
            Operation newOperation = operationMapper.convertToOperation(methodDTO, interfaceId, tcId);
            operationsToSave.add(newOperation);
            return newOperation;
        } else {
            if (existingOperation.getDeletedDate() != null) {
                existingOperation.setDeletedDate(null);
                existingOperation.setUpdatedDate(new Date());
                operationsToSave.add(existingOperation);
            }
            if (!Objects.equals(methodDTO.getDescription(), existingOperation.getDescription()) || !Objects.equals(
                    methodDTO.getReturnType(),
                    existingOperation.getReturnType()) || !Objects.equals(tcId, existingOperation.getTcId())) {
                operationMapper.updateOperation(existingOperation, methodDTO, tcId, interfaceId);
                operationsToSave.add(existingOperation);
            }
            return existingOperation;
        }
    }

    private List<Parameter> processParameters(List<ParameterDTO> parameters, Integer methodId) {
        String parameter = "Parameter";
        List<Parameter> existingOrCreatedParameters = new ArrayList<>();
        for (ParameterDTO parameterDTO : parameters) {
            validateField(parameterDTO.getName(), parameter, "name");
            validateField(parameterDTO.getType(), parameter, "type");
            existingOrCreatedParameters.add(createOrUpdateParameter(parameterDTO, methodId));
        }
        return existingOrCreatedParameters;
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
            if (updateParameter.getDeletedDate() != null) {
                updateParameter.setDeletedDate(null);
                parameterRepository.save(updateParameter);
            }
            return optionalParameter.get();
        }
    }

    private void validateField(String fieldValue, String entityName, String fieldName) {
        if (fieldValue == null || fieldValue.isEmpty()) {
            throw new ValidationException(String.format("Отсутствует обязательное поле '%s': %s",
                                                        entityName,
                                                        fieldName));
        }
    }

    private Boolean equalsInterfaces(Interface getInterface, InterfaceDTO interfaceDTO, Integer tcId) {
        return getInterface.getName().equals(interfaceDTO.getName()) && getInterface.getVersion()
                .equals(interfaceDTO.getVersion()) && getInterface.getSpecLink()
                .equals(interfaceDTO.getSpecLink()) && Objects.equals(getInterface.getTcId(),
                                                                      tcId) && getInterface.getProtocol()
                .equals(interfaceDTO.getProtocol());
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
        log.info("Старт метода: postFitnessFunctions");
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
        LocalAssessment assessment = assessmentRepository.save(LocalAssessment.builder()
                                                                       .sourceId(sourceId)
                                                                       .product(product)
                                                                       .sourceTypeId(enumSourceType.getId())
                                                                       .createdTime(LocalDateTime.now())
                                                                       .build());
        requests.forEach(request -> processAssessmentCheck(request, assessment));
        log.info("метод: postFitnessFunctions успешно завершен");
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
                List<ConnectOperationDTO> operationDTOS = discoveredOperations.stream().map(discoveredOperation -> {
                    log.info("connection getConnectionOperationId = {}",
                             discoveredOperation.getConnectionOperationId());
                    Operation operation = null;
                    if (discoveredOperation.getConnectionOperationId() != null) {
                        operation = operationRepository.findById(discoveredOperation.getConnectionOperationId()).get();
                        log.info("operationId = {}", operation.getId());
                    }
                    return createConnectOperationDTO(operation, discoveredOperation);
                }).collect(Collectors.toList());
                productMapicInterfaceDTO.setOperations(operationDTOS);
                productMapicInterfaceDTO.setConnectInterface(createMapicInterfaceDTO(discoveredInterface, anInterface));
                result.add(productMapicInterfaceDTO);
            });
        }
        return result;
    }

    private ConnectOperationDTO createConnectOperationDTO(Operation operation,
                                                          DiscoveredOperation discoveredOperation) {
        ConnectOperationDTO connectOperationDTO = ConnectOperationDTO.builder()
                .id(discoveredOperation.getId())
                .name(discoveredOperation.getName())
                .description(discoveredOperation.getDescription())
                .type(discoveredOperation.getType())
                .build();
        if (operation != null) {
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

    public List<ProductInterfaceDTO> getProductsFromStructurizr(String cmdb) {
        Product product = productRepository.findByAliasCaseInsensitive(cmdb);
        if (product == null) {
            throw new EntityNotFoundException("Продукт с данным cmdb не найден.");
        }
        List<ProductInterfaceDTO> result = new ArrayList<>();
        List<ContainerProduct> containerProducts = containerRepository.findAllByProductIdAndDeletedDateIsNull(product.getId());
        if (!containerProducts.isEmpty()) {
            List<Integer> containerIds = containerProducts.stream().map(ContainerProduct::getId).toList();
            List<Interface> interfaces = interfaceRepository.findAllByContainerIdInAndDeletedDateIsNull(containerIds);
            if (!interfaces.isEmpty()) {
                for (Interface interfaceObj : interfaces) {
                    ProductInterfaceDTO productInterfaceDTO = createProductInterface(interfaceObj);
                    List<DiscoveredInterface> dInterfaces = discoveredInterfaceRepository.findAllByConnectionInterfaceId(
                            interfaceObj.getId());
                    if (!dInterfaces.isEmpty()) {
                        List<MapicInterfaceDTO> mapicInterfaceDTOS = new ArrayList<>();
                        dInterfaces.forEach(discoveredInterface -> mapicInterfaceDTOS.add(createMapicInterfaceDTO(
                                discoveredInterface)));
                        productInterfaceDTO.setMapicInterfaces(mapicInterfaceDTOS);
                    }
                    List<Operation> operations = operationRepository.findAllByInterfaceIdAndDeletedDateIsNull(
                            interfaceObj.getId());
                    if (!operations.isEmpty()) {
                        List<OperationDTO> operationDTOS = new ArrayList<>();
                        for (Operation operation : operations) {
                            operationDTOS.add(createOperationDTO(operation,
                                                                 discoveredOperationRepository.findAllByConnectionOperationId(
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
                .code(interfaceObj.getCode())
                .createDate(interfaceObj.getCreatedDate())
                .updateDate(interfaceObj.getUpdatedDate())
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

    private OperationDTO createOperationDTO(Operation operation, List<DiscoveredOperation> discoveredOperations) {
        OperationDTO operationDTO = OperationDTO.builder()
                .id(operation.getId())
                .name(operation.getName())
                .description(operation.getDescription())
                .type(operation.getType())
                .createDate(operation.getCreatedDate())
                .updateDate(operation.getUpdatedDate())
                .build();
        List<MapicOperationDTO> result = new ArrayList<>();
        if (discoveredOperations != null && !discoveredOperations.isEmpty()) {
            for (DiscoveredOperation dOperation : discoveredOperations) {
                result.add(MapicOperationDTO.builder()
                                   .id(dOperation.getId())
                                   .name(dOperation.getName())
                                   .description(dOperation.getDescription())
                                   .type(dOperation.getType())
                                   .build());
            }
        }
        operationDTO.setMapicOperations(result);
        return operationDTO;
    }

    public List<ContainerInterfacesDTO> getContainersFromStructurizr(String cmdb) {
        Product product = productRepository.findByAliasCaseInsensitive(cmdb);
        if (product == null) {
            throw new EntityNotFoundException("Продукт с данным cmdb не найден.");
        }
        List<ContainerInterfacesDTO> result = new ArrayList<>();
        List<ContainerProduct> containerProducts = containerRepository.findAllByProductIdAndDeletedDateIsNull(product.getId());
        if (!containerProducts.isEmpty()) {
            for (ContainerProduct containerProduct : containerProducts) {
                result.add(ContainerInterfacesDTO.builder()
                                   .id(containerProduct.getId())
                                   .name(containerProduct.getName())
                                   .code(containerProduct.getCode())
                                   .createDate(containerProduct.getCreatedDate())
                                   .updateDate(containerProduct.getUpdatedDate())
                                   .interfaces(createInterfaceMethodDTOS(containerProduct.getId()))
                                   .build());
            }
        }
        return result;
    }

    private List<InterfaceMethodDTO> createInterfaceMethodDTOS(Integer containerId) {
        List<InterfaceMethodDTO> result = new ArrayList<>();
        List<Interface> interfaces = interfaceRepository.findAllByContainerIdAndDeletedDateIsNull(containerId);
        if (interfaces.isEmpty()) {
            return Collections.emptyList();
        }
        List<Integer> tcIds = interfaces.stream().map(Interface::getTcId).toList();
        List<Integer> interfaceIds = interfaces.stream().map(Interface::getId).toList();
        Map<Integer, List<DiscoveredInterface>> discoveredInterfaceMap = discoveredInterfaceRepository.findAllByConnectionInterfaceIdIn(
                interfaceIds).stream().collect(Collectors.groupingBy(DiscoveredInterface::getConnectionInterfaceId));
        Map<Integer, TcDTO> tcDTOMap = loadTcDTOMap(tcIds);
        for (Interface interfaceObj : interfaces) {
            result.add(InterfaceMethodDTO.builder()
                               .id(interfaceObj.getId())
                               .name(interfaceObj.getName())
                               .specLink(interfaceObj.getSpecLink())
                               .protocol(interfaceObj.getProtocol())
                               .description(interfaceObj.getDescription())
                               .version(interfaceObj.getVersion())
                               .code(interfaceObj.getCode())
                               .createDate(interfaceObj.getCreatedDate())
                               .updateDate(interfaceObj.getUpdatedDate())
                               .mapicInterfaces(createMapicInterface(discoveredInterfaceMap.get(interfaceObj.getId())))
                               .operations(createOperations(operationRepository.findAllByInterfaceIdAndDeletedDateIsNull(
                                       interfaceObj.getId())))
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

    private List<MapicInterfaceDTO> createMapicInterface(List<DiscoveredInterface> discoveredInterfaces) {
        List<MapicInterfaceDTO> result = new ArrayList<>();
        if (discoveredInterfaces != null && !discoveredInterfaces.isEmpty()) {
            for (DiscoveredInterface discoveredInterface : discoveredInterfaces) {
                result.add(MapicInterfaceDTO.builder()
                                   .id(discoveredInterface.getId())
                                   .name(discoveredInterface.getName())
                                   .description(discoveredInterface.getDescription())
                                   .build());
            }
        }
        return result;
    }

    private List<OperationFullDTO> createOperations(List<Operation> operations) {
        List<OperationFullDTO> result = new ArrayList<>();
        if (operations.isEmpty()) {
            return Collections.emptyList();
        }
        List<Integer> operationId = operations.stream().map(Operation::getId).toList();
        List<Integer> tcIds = operations.stream().map(Operation::getTcId).toList();
        List<Sla> slas = slaRepository.findAllByOperationIdIn(operationId);
        Map<Integer, List<DiscoveredOperation>> discoveredOperationMap = discoveredOperationRepository.findAllByConnectionOperationIdIn(
                operationId).stream().collect(Collectors.groupingBy(DiscoveredOperation::getConnectionOperationId));
        Map<Integer, Sla> slaMap = slas.stream().collect(Collectors.toMap(Sla::getOperationId, sla -> sla));
        Map<Integer, TcDTO> tcDTOMap = loadTcDTOMap(tcIds);
        for (Operation operation : operations) {
            result.add(OperationFullDTO.builder()
                               .id(operation.getId())
                               .description(operation.getDescription())
                               .name(operation.getName())
                               .type(operation.getType())
                               .mapicOperations(createMapicOperationFullDTO(discoveredOperationMap.get(operation.getId())))
                               .sla(createSlaV2DTO(slaMap.get(operation.getId())))
                               .techCapability(tcDTOMap.get(operation.getTcId()))
                               .createdDate(operation.getCreatedDate())
                               .updateDate(operation.getUpdatedDate())
                               .build());
        }
        return result;
    }

    private List<MapicOperationFullDTO> createMapicOperationFullDTO(List<DiscoveredOperation> discoveredOperations) {
        List<MapicOperationFullDTO> result = new ArrayList<>();
        if (discoveredOperations != null && !discoveredOperations.isEmpty()) {
            for (DiscoveredOperation discoveredOperation : discoveredOperations) {
                {
                    String contextApi = null;
                    Optional<DiscoveredInterface> optDiscoveredInterface = discoveredInterfaceRepository.findById(
                            discoveredOperation.getInterfaceId());
                    if (optDiscoveredInterface.isPresent()) {
                        contextApi = optDiscoveredInterface.get().getContext();
                    }
                    result.add(MapicOperationFullDTO.builder()
                                       .id(discoveredOperation.getId())
                                       .name(discoveredOperation.getName())
                                       .type(discoveredOperation.getType())
                                       .description(discoveredOperation.getDescription())
                                       .context(discoveredOperation.getContext())
                                       .contextApi(contextApi)
                                       .build());
                }
            }
        }
        return result;
    }

    private SlaV2DTO createSlaV2DTO(Sla sla) {
        if (sla != null) {
            return SlaV2DTO.builder().latency(sla.getLatency()).errorRate(sla.getErrorRate()).rps(sla.getRps()).build();
        }
        return null;
    }

    public List<ProductInfoShortDTO> getProductInfo() {
        return productTechMapper.mapToProductInfoShortDTO(productRepository.findAll());
    }

    public List<GetProductsByIdsDTO> getProductByIds(List<Integer> ids) {
        return productTechMapper.mapToGetProductsByIdsDTO(productRepository.findAllById(ids));
    }

    public void patchProductSource(String cmdb, String sourceName) {
        if (sourceName == null || sourceName.isEmpty()) {
            throw new IllegalArgumentException("Не передан параметр source-name");
        }
        Product product = getProductByCode(cmdb);
        product.setSource(sourceName);
        product.setUploadDate(LocalDateTime.now());
        productRepository.save(product);
    }

    public SystemRelationDto getInfluencesByCmdb(String cmdb) {
        Product product = productRepository.findByAliasCaseInsensitive(cmdb);
        if (product == null) {
            throw new EntityNotFoundException("Продукт с данным cmdb не найден.");
        }
        ProductInfluenceDTO influences = graphClient.getInfluences(cmdb);
        if (influences == null) {
            return SystemRelationDto.builder()
                    .influencingSystems(new ArrayList<>())
                    .dependentSystems(new ArrayList<>())
                    .build();
        }
        List<String> lowerAliasesInfl = influences.getInfluencingSystems().stream()
                .map(String::toLowerCase)
                .collect(Collectors.toList());

        List<String> lowerAliasesDepens = influences.getDependentSystems().stream()
                .map(String::toLowerCase)
                .collect(Collectors.toList());

        Map<String, Product> influenceProducts = productRepository.findByAliasInIgnoreCase(lowerAliasesInfl)
                .stream()
                .collect(Collectors.toMap(p -> p.getAlias().toLowerCase(), Function.identity()));
        Map<String, Product> dependProducts = productRepository.findByAliasInIgnoreCase(lowerAliasesDepens)
                .stream()
                .collect(Collectors.toMap(p -> p.getAlias().toLowerCase(), Function.identity()));

        List<SystemInfoDTO> dependentSystems = influences.getDependentSystems() == null ? new ArrayList<>() : influences.getDependentSystems()
                .stream()
                .map(system -> enrichSystemWithProduct(system, dependProducts))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        List<SystemInfoDTO> influencingSystems = influences.getInfluencingSystems() == null ? new ArrayList<>() : influences.getInfluencingSystems()
                .stream()
                .map(system -> enrichSystemWithProduct(system, influenceProducts))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        return SystemRelationDto.builder()
                .dependentSystems(dependentSystems)
                .influencingSystems(influencingSystems)
                .build();
    }

    private SystemInfoDTO enrichSystemWithProduct(String system, Map<String, Product> productMap) {
        Product product = productMap.get(system.toLowerCase());
        if (product == null) {
            return null;
        }
        return SystemInfoDTO.builder()
                .alias(product.getAlias())
                .description(product.getDescription())
                .gitUrl(product.getGitUrl())
                .id(product.getId().toString())
                .name(product.getName())
                .structurizrApiUrl(product.getStructurizrApiUrl())
                .structurizrWorkspaceName(product.getStructurizrWorkspaceName())
                .uploadSource(product.getSource())
                .uploadDate(product.getUploadDate())
                .build();
    }

    public List<Integer> getTCIdsByProductId(Integer id) {
        productRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("not found"));
        List<ContainerProduct> containerProducts = containerRepository.findAllByProductIdAndDeletedDateIsNull(id);
        log.info("containerProducts: " + containerProducts);
        if (containerProducts == null && containerProducts.size() == 0) {
            return new ArrayList<Integer>();
        }
        List<Interface> interfaces = interfaceRepository.findAllByContainerIdIn(containerProducts.stream()
                                                                                        .map(ContainerProduct::getId)
                                                                                        .collect(Collectors.toList()));
        log.info("interfaces: " + interfaces);
        List<Operation> operations = operationRepository.findAllByInterfaceIdIn(interfaces.stream()
                                                                                        .map(Interface::getId)
                                                                                        .collect(Collectors.toList()));
        log.info("operations: " + operations);
        return Stream.concat(
                        interfaces.stream().map(Interface::getTcId),
                        operations.stream().map(Operation::getTcId)
                )
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }
}