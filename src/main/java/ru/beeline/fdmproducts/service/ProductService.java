package ru.beeline.fdmproducts.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.beeline.fdmlib.dto.auth.UserInfoDTO;
import ru.beeline.fdmlib.dto.auth.UserProfileDTO;
import ru.beeline.fdmlib.dto.auth.UserProfileShortDTO;
import ru.beeline.fdmlib.dto.graph.ProductInfluenceDTO;
import ru.beeline.fdmlib.dto.product.GetProductTechDto;
import ru.beeline.fdmlib.dto.product.GetProductsByIdsDTO;
import ru.beeline.fdmlib.dto.product.GetProductsDTO;
import ru.beeline.fdmlib.dto.product.ProductPutDto;
import ru.beeline.fdmproducts.client.*;
import ru.beeline.fdmproducts.controller.RequestContext;
import ru.beeline.fdmproducts.domain.*;
import ru.beeline.fdmproducts.dto.*;
import ru.beeline.fdmproducts.dto.dashboard.E2eProcessInfoDTO;
import ru.beeline.fdmproducts.dto.dashboard.GetInfoProcessDTO;
import ru.beeline.fdmproducts.dto.dashboard.ResultDTO;
import ru.beeline.fdmproducts.exception.DatabaseConnectionException;
import ru.beeline.fdmproducts.exception.EntityNotFoundException;
import ru.beeline.fdmproducts.exception.ForbiddenException;
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
    private final OperationMapper operationMapper;
    private final DiscoveredOperationMapper discoveredOperationMapper;
    private final SlaMapper slaMapper;
    private final ParameterMapper parameterMapper;
    private final AssessmentMapper assessmentMapper;
    private final CapabilityClient capabilityClient;
    private final TechradarClient techradarClient;
    private final UserClient userClient;
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
    private final DashboardClient dashboardClient;
    private final LocalAcObjectRepository localAcObjectRepository;
    private final LocalAcObjectDetailRepository localAcObjectDetailRepository;


    public ProductService(ContainerMapper containerMapper,
                          OperationMapper operationMapper,
                          SlaMapper slaMapper,
                          ParameterMapper parameterMapper,
                          AssessmentMapper assessmentMapper,
                          DiscoveredOperationMapper discoveredOperationMapper,
                          CapabilityClient capabilityClient,
                          TechradarClient techradarClient,
                          UserClient userClient,
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
                          DiscoveredOperationRepository discoveredOperationRepository, DashboardClient dashboardClient, LocalAcObjectRepository localAcObjectRepository, LocalAcObjectDetailRepository localAcObjectDetailRepository) {
        this.containerMapper = containerMapper;
        this.operationMapper = operationMapper;
        this.discoveredOperationMapper = discoveredOperationMapper;
        this.slaMapper = slaMapper;
        this.parameterMapper = parameterMapper;
        this.assessmentMapper = assessmentMapper;
        this.capabilityClient = capabilityClient;
        this.techradarClient = techradarClient;
        this.userClient = userClient;
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
        this.dashboardClient = dashboardClient;
        this.localAcObjectRepository = localAcObjectRepository;
        this.localAcObjectDetailRepository = localAcObjectDetailRepository;
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

    public ProductFullDTO getProductDTOByCode(String code) {
        Product product = getProductByCode(code);
        UserProfileDTO user = new UserProfileDTO();
        if (product.getOwnerID() != null) {
            user = userClient.findUserProfilesById(product.getOwnerID());
        }
        return ProductTechMapper.mapToProductFullDTO(product, user);
    }

    public ProductInfoDTO getProductInfoByCode(String code) {
        if (code == null || code.equals("\n") || code.equals(" \n")) {
            throw new IllegalArgumentException("Параметр alias не должен быть пустым.");
        }
        Product product = productRepository.findByAliasCaseInsensitive(code);
        if (product == null) {
            throw new EntityNotFoundException((String.format("Продукт c alias '%s' не найден", code)));
        }
        return ProductTechMapper.mapToProductInfoDTO(product);
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

    public void postUserProduct(List<String> aliasList, Integer userId) {
        if (aliasList.isEmpty()) {
            throw new IllegalArgumentException("400: Массив пустой. ");
        }
        List<String> notFoundAliases = new ArrayList<>();
        for (String alias : aliasList) {
            Product product = productRepository.findByAliasCaseInsensitive(alias);
            if (product != null) {
                if (!userProductRepository.existsByUserIdAndProductId(userId, product.getId())) {
                    log.info("Создание связи для пользователя " + userId + " и продукта" + product.getId());
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
        Map<String, ContainerProduct> existingContainers = containerRepository.findAllByCodeInAndProductId(containerDTOS.stream()
                                .map(ContainerDTO::getCode)
                                .toList(),
                        product.getId())
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
        markContainersAsDeleted(product.getId(), containerDTOS);
        Map<String, Long> codesIdMap = loadInterfaceCapabilityMap(allInterfaces);
        Map<String, Long> methodCodesIdMap = loadMethodCapabilityMap(allMethods);
        Map<Integer, List<InterfaceDTO>> containerInterfaces = buildContainerInterfacesMap(existingContainers,
                toSave,
                interfacesByCode);
        for (Map.Entry<Integer, List<InterfaceDTO>> entry : containerInterfaces.entrySet()) {
            processInterfaces(entry.getValue(), entry.getKey(), codesIdMap, methodCodesIdMap);
            markInterfacesAsDeleted(entry.getKey(), entry.getValue());
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
                }
                if (containerEntity.getDeletedDate() != null) {
                    containerEntity.setDeletedDate(null);
                    containerEntity.setUpdatedDate(new Date());
                }
                toSave.add(containerEntity);
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

    private void cascadeDeleteContainer(ContainerProduct container) {
        List<Interface> interfaces = interfaceRepository.findAllByContainerId(container.getId());
        if (!interfaces.isEmpty()) {
            LocalDateTime now = LocalDateTime.now();
            for (Interface interfaceObj : interfaces) {
                if (interfaceObj.getDeletedDate() == null) {
                    interfaceObj.setDeletedDate(now);
                }
                cascadeDeleteInterface(interfaceObj);
            }
            interfaceRepository.saveAll(interfaces);
        }
    }

    private void markContainersAsDeleted(Integer productId, List<ContainerDTO> newContainers) {
        List<String> dtoCodes = newContainers.stream().map(ContainerDTO::getCode).toList();
        containerRepository.markContainersAsDeleted(productId, dtoCodes, new Date());
        List<ContainerProduct> markedContainers = containerRepository.findAllByProductIdAndDeletedDateIsNotNull(
                productId);
        for (ContainerProduct container : markedContainers) {
            cascadeDeleteContainer(container);
        }
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
        List<Interface> toDelete = allDbInterfaces.stream()
                .filter(dbIntf -> !dtoCodes.contains(dbIntf.getCode()))
                .filter(dbIntf -> dbIntf.getDeletedDate() == null)
                .peek(dbIntf -> dbIntf.setDeletedDate(LocalDateTime.now()))
                .toList();
        if (!toDelete.isEmpty()) {
            interfaceRepository.saveAll(toDelete);
            for (Interface interfaceObj : toDelete) {
                cascadeDeleteInterface(interfaceObj);
            }
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
        List<Operation> toDelete = new ArrayList<>();
        for (Operation op : allDbOperations) {
            String key = op.getName() + "::" + (op.getType() != null ? op.getType() : "");
            if (!newKeys.contains(key) && op.getDeletedDate() == null) {
                op.setDeletedDate(LocalDateTime.now());
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
            LocalDateTime now = LocalDateTime.now();
            for (Operation op : ops) {
                if (op.getDeletedDate() == null) {
                    op.setDeletedDate(LocalDateTime.now());
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
            interfaceObj = InterfaceMapper.convertToInterface(dto, containerId, tcId);
            toSave.add(interfaceObj);
        } else {
            if (interfaceObj.getDeletedDate() != null) {
                interfaceObj.setDeletedDate(null);
                interfaceObj.setUpdatedDate(LocalDateTime.now());
                toSave.add(interfaceObj);
            }
            if (!equalsInterfaces(interfaceObj, dto, tcId)) {
                InterfaceMapper.updateInterface(interfaceObj, dto, containerId, tcId);
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
        List<Parameter> toDelete = allParameters.stream()
                .filter(p -> existingOrCreated.stream()
                        .noneMatch(e -> e.getParameterName().equals(p.getParameterName()) && e.getParameterType()
                                .equals(p.getParameterType())))
                .filter(p -> p.getDeletedDate() == null)
                .toList();
        toDelete.forEach(p -> p.setDeletedDate(LocalDateTime.now()));
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
                existingOperation.setUpdatedDate(LocalDateTime.now());
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
                            Collectors.mapping(techProduct -> ProductTechMapper.mapToGetProductsDTO(
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

    public void postFitnessFunctions(String alias, String sourceType,
                                     List<FitnessFunctionDTO> requests, Integer sourceId) {
        log.info("Старт метода: postFitnessFunctions");
        validateRequest(requests);
        Product product = productRepository.findByAliasCaseInsensitive(alias);
        if (product == null) {
            throw new EntityNotFoundException("Product не найден.");
        }
        EnumSourceType enumSourceType = enumSourceTypeRepository.findByName(sourceType)
                .orElseThrow(() -> new IllegalArgumentException("Невозможный источник."));
        if (enumSourceType.getIdentifySource() && sourceId == null) {
            throw new IllegalArgumentException("Для указанного источника обязательна передача идентификатора.");
        }
        if (sourceId != null && assessmentRepository.findBySourceIdAndProduct(sourceId, product).isPresent()) {
            throw new IllegalArgumentException(
                    String.format("Запись с sourceId: %s и product: %s уже существует в бд", sourceId, product.getId()));
        }
        LocalAssessment assessment = assessmentRepository.save(LocalAssessment.builder()
                .sourceId(sourceId)
                .product(product)
                .sourceTypeId(enumSourceType.getId())
                .createdTime(LocalDateTime.now())
                .build());
        for (FitnessFunctionDTO request : requests) {
            LocalAssessmentCheck assessmentCheck = processAssessmentCheck(request, assessment);
            if (assessmentCheck == null) {
                continue;
            }
            if (request.getAssessmentObjects() != null && !request.getAssessmentObjects().isEmpty()) {
                Map<Integer, List<DetailsDTO>> savedLA = saveLocalAcObject(request.getAssessmentObjects(), assessmentCheck);
                saveDetails(savedLA);
            }
        }
        log.info("метод: postFitnessFunctions успешно завершен");
    }

    private void saveDetails(Map<Integer, List<DetailsDTO>> detailsMap) {
        List<LocalAcObjectDetail> saveList = new ArrayList<>();
        detailsMap.forEach((acObjectId, detailsDto) -> {
            for (DetailsDTO dto : detailsDto) {
                saveList.add(LocalAcObjectDetail.builder()
                        .lacoId(acObjectId)
                        .key(dto.getKey())
                        .value(dto.getValue())
                        .build());
            }
        });
        localAcObjectDetailRepository.saveAll(saveList);
    }

    private Map<Integer, List<DetailsDTO>> saveLocalAcObject(List<AssessmentObjectDTO> assessmentObjectDTOS,
                                                             LocalAssessmentCheck assessmentCheck) {
        Map<Integer, List<DetailsDTO>> localAcObjectMap = new HashMap<>();
        List<LocalAcObject> entities = assessmentObjectDTOS.stream()
                .map(dto -> LocalAcObject.builder()
                        .isCheck(dto.getIsCheck())
                        .lacId(assessmentCheck.getId())
                        .build())
                .collect(Collectors.toList());
        List<LocalAcObject> savedEntities = localAcObjectRepository.saveAll(entities);
        for (int i = 0; i < savedEntities.size(); i++) {
            localAcObjectMap.put(
                    savedEntities.get(i).getId(),
                    assessmentObjectDTOS.get(i).getDetails()
            );
        }
        return localAcObjectMap;
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
                assessment = assessmentRepository.findFirstBySourceTypeIdAndProductIdOrderByCreatedTimeDesc(enumSourceType.getId(),
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
        return assessmentMapper.mapToAssessmentResponseDTO(assessment, product,
                enumSourceTypeRepository.findById(assessment.getSourceTypeId()).get().getName());
    }

    private void validateRequest(List<FitnessFunctionDTO> requests) {
        boolean hasErrors = requests.stream().anyMatch(req -> req.getCode() == null || req.getIsCheck() == null);

        if (hasErrors) {
            throw new IllegalArgumentException("Missing required fields");
        }
    }

    private LocalAssessmentCheck processAssessmentCheck(FitnessFunctionDTO request, LocalAssessment assessment) {
        return fitnessFunctionRepository.findByCode(request.getCode())
                .map(fitnessFunction -> {
                    LocalAssessmentCheck check = LocalAssessmentCheck.builder()
                            .fitnessFunction(fitnessFunction)
                            .assessmentDescription(request.getAssessmentDescription())
                            .assessment(assessment)
                            .isCheck(request.getIsCheck())
                            .resultDetails(request.getResultDetails())
                            .build();
                    return assessmentCheckRepository.save(check);
                })
                .orElse(null);
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

    public List<ProductMapicInterfaceDTO> getProductsFromMapic(String cmdb, Boolean showHidden) {
        Product product = productRepository.findByAliasCaseInsensitive(cmdb);
        if (product == null) {
            throw new EntityNotFoundException("Продукт с данным cmdb не найден.");
        }
        List<DiscoveredInterface> discoveredInterfaces = showHidden ? discoveredInterfaceRepository.findAllByProduct(product)
                : discoveredInterfaceRepository.findAllByProductAndDeletedDateIsNull(product);
        if (discoveredInterfaces.isEmpty()) {
            return Collections.emptyList();
        }
        List<Integer> discoveredInterfaceIds = discoveredInterfaces.stream()
                .map(DiscoveredInterface::getId)
                .collect(Collectors.toList());
        List<DiscoveredOperation> allDiscoveredOperations = showHidden
                ? discoveredOperationRepository.findAllByInterfaceIdIn(discoveredInterfaceIds)
                : discoveredOperationRepository.findAllByInterfaceIdInAndDeletedDateIsNull(discoveredInterfaceIds);
        Map<Integer, List<DiscoveredOperation>> discoveredOperationsByInterfaceId = allDiscoveredOperations.stream()
                .collect(Collectors.groupingBy(DiscoveredOperation::getInterfaceId));
        List<Integer> connectionOperationIds = allDiscoveredOperations.stream()
                .map(DiscoveredOperation::getConnectionOperationId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        Map<Integer, Operation> operationsById = operationRepository.findAllById(connectionOperationIds).stream()
                .collect(Collectors.toMap(Operation::getId, Function.identity()));
        return buildProductMapicInterfaceDTOs(discoveredInterfaces, discoveredOperationsByInterfaceId, operationsById);
    }

    private List<ProductMapicInterfaceDTO> buildProductMapicInterfaceDTOs(List<DiscoveredInterface> discoveredInterfaces,
                                                                          Map<Integer, List<DiscoveredOperation>> discoveredOperationsByInterfaceId,
                                                                          Map<Integer, Operation> operationsById) {
        return discoveredInterfaces.stream().map(discoveredInterface -> {
            ProductMapicInterfaceDTO dto = InterfaceMapper.createProductMapicInterface(discoveredInterface);
            List<DiscoveredOperation> interfaceDiscoveredOperations = discoveredOperationsByInterfaceId.getOrDefault(
                    discoveredInterface.getId(), Collections.emptyList());
            if (interfaceDiscoveredOperations != null && !interfaceDiscoveredOperations.isEmpty()) {
                dto.setContextProvider(interfaceDiscoveredOperations.get(0).getContext());
            }
            List<ConnectOperationDTO> operationDTOs = interfaceDiscoveredOperations.stream()
                    .map(discoveredOperation -> {
                        log.info("connection getConnectionOperationId = {}", discoveredOperation.getConnectionOperationId());
                        Operation operation = null;
                        if (discoveredOperation.getConnectionOperationId() != null) {
                            operation = operationsById.get(discoveredOperation.getConnectionOperationId());
                            if (operation != null) {
                                log.info("operationId = {}", operation.getId());
                            }
                        }
                        return InterfaceMapper.createConnectOperationDTO(operation, discoveredOperation);
                    })
                    .collect(Collectors.toList());
            dto.setOperations(operationDTOs);
            dto.setConnectInterface(InterfaceMapper.createMapicInterfaceDTO(discoveredInterface,
                    discoveredInterface.getConnectedInterface()));
            return dto;
        }).collect(Collectors.toList());
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
                    ProductInterfaceDTO productInterfaceDTO = InterfaceMapper.createProductInterface(interfaceObj);
                    List<DiscoveredInterface> dInterfaces = discoveredInterfaceRepository.findAllByConnectionInterfaceId(
                            interfaceObj.getId());
                    if (!dInterfaces.isEmpty()) {
                        List<MapicInterfaceDTO> mapicInterfaceDTOS = new ArrayList<>();
                        dInterfaces.forEach(discoveredInterface -> mapicInterfaceDTOS.add(InterfaceMapper.createMapicInterfaceDTO(
                                discoveredInterface)));
                        productInterfaceDTO.setMapicInterfaces(mapicInterfaceDTOS);
                    }
                    List<Operation> operations = operationRepository.findAllByInterfaceIdAndDeletedDateIsNull(
                            interfaceObj.getId());
                    if (!operations.isEmpty()) {
                        List<OperationDTO> operationDTOS = new ArrayList<>();
                        for (Operation operation : operations) {
                            operationDTOS.add(operationMapper.createOperationDTO(operation,
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

    public List<ContainerInterfacesDTO> getContainersFromStructurizr(String cmdb, Boolean showHidden) {
        Product product = productRepository.findByAliasCaseInsensitive(cmdb);
        if (product == null) {
            throw new EntityNotFoundException("Продукт с данным cmdb не найден.");
        }
        List<ContainerProduct> containerProducts = showHidden ? containerRepository.findAllByProductId(product.getId())
                : containerRepository.findAllByProductIdAndDeletedDateIsNull(product.getId());
        log.info("Количество containerProducts = {}", containerProducts.size());
        return buildContainerInterfacesDTO(containerProducts, showHidden);
    }

    private List<ContainerInterfacesDTO> buildContainerInterfacesDTO(List<ContainerProduct> containerProducts, Boolean showHidden) {
        if (containerProducts.isEmpty()) {
            return Collections.emptyList();
        }
        List<Integer> containerIds = containerProducts.stream().map(ContainerProduct::getId).collect(Collectors.toList());
        List<Interface> allInterfaces = showHidden ? interfaceRepository.findAllByContainerIdIn(containerIds)
                : interfaceRepository.findAllByContainerIdInAndDeletedDateIsNull(containerIds);
        log.info("Количество Interface = {}", allInterfaces.size());
        Map<Integer, List<Interface>> interfacesByContainerId = allInterfaces.stream()
                .collect(Collectors.groupingBy(Interface::getContainerId));
        List<Integer> allInterfaceIds = allInterfaces.stream().map(Interface::getId).collect(Collectors.toList());
        List<Operation> allOperations = showHidden ? operationRepository.findAllByInterfaceIdIn(allInterfaceIds)
                : operationRepository.findAllByInterfaceIdInAndDeletedDateIsNull(allInterfaceIds);
        Map<Integer, List<DiscoveredInterface>> discoveredInterfaceMap = discoveredInterfaceRepository
                .findAllByConnectionInterfaceIdIn(allInterfaceIds).stream()
                .collect(Collectors.groupingBy(DiscoveredInterface::getConnectionInterfaceId));
        List<Integer> interfaceTcIds = allInterfaces.stream().map(Interface::getTcId).filter(Objects::nonNull)
                .distinct().collect(Collectors.toList());
        Map<Integer, TcDTO> interfaceTcDTOMap = loadTcDTOMap(interfaceTcIds);
        List<Integer> allOperationIds = allOperations.stream().map(Operation::getId).collect(Collectors.toList());
        List<Integer> allOperationTcIds = allOperations.stream().map(Operation::getTcId).filter(Objects::nonNull)
                .distinct().collect(Collectors.toList());
        List<Sla> allSlas = slaRepository.findAllByOperationIdIn(allOperationIds);
        Map<Integer, Sla> slaMap = allSlas.stream().collect(Collectors.toMap(Sla::getOperationId, sla -> sla));
        Map<Integer, List<DiscoveredOperation>> discoveredOperationMap = discoveredOperationRepository
                .findAllByConnectionOperationIdIn(allOperationIds).stream()
                .collect(Collectors.groupingBy(DiscoveredOperation::getConnectionOperationId));
        Map<Integer, TcDTO> operationTcDTOMap = loadTcDTOMap(allOperationTcIds);
        Map<Integer, OperationFullDTO> operationDTOMap = createAllOperationsDTO(allOperations, slaMap, discoveredOperationMap,
                operationTcDTOMap);
        Map<Integer, List<OperationFullDTO>> operationsDTOByInterfaceId = allOperations.stream()
                .collect(Collectors.groupingBy(Operation::getInterfaceId,
                        Collectors.mapping(op -> operationDTOMap.get(op.getId()), Collectors.toList())));
        return buildContainerDTOs(containerProducts, interfacesByContainerId, operationsDTOByInterfaceId,
                discoveredInterfaceMap, interfaceTcDTOMap);
    }

    private List<ContainerInterfacesDTO> buildContainerDTOs(List<ContainerProduct> containerProducts,
                                                            Map<Integer, List<Interface>> interfacesByContainerId,
                                                            Map<Integer, List<OperationFullDTO>> operationsDTOByInterfaceId,
                                                            Map<Integer, List<DiscoveredInterface>> discoveredInterfaceMap,
                                                            Map<Integer, TcDTO> interfaceTcDTOMap) {
        log.info("Создание ContainerDTO");
        List<ContainerInterfacesDTO> result = containerProducts.stream()
                .map(containerProduct -> {
                    List<Interface> containerInterfaces = interfacesByContainerId.getOrDefault(containerProduct.getId(),
                            Collections.emptyList());
                    return ContainerInterfacesDTO.builder()
                            .id(containerProduct.getId())
                            .name(containerProduct.getName())
                            .code(containerProduct.getCode())
                            .createDate(containerProduct.getCreatedDate())
                            .updateDate(containerProduct.getUpdatedDate())
                            .deletedDate(containerProduct.getDeletedDate())
                            .interfaces(createInterfaceMethodDTOS(containerInterfaces, operationsDTOByInterfaceId,
                                    discoveredInterfaceMap, interfaceTcDTOMap))
                            .build();
                })
                .collect(Collectors.toList());
        log.info("Количество ContainerInterfacesDTO = {}", result.size());
        return result;
    }

    private List<InterfaceMethodDTO> createInterfaceMethodDTOS(List<Interface> interfaces,
                                                               Map<Integer, List<OperationFullDTO>> operationsDTOByInterfaceId,
                                                               Map<Integer, List<DiscoveredInterface>> discoveredInterfaceMap,
                                                               Map<Integer, TcDTO> tcDTOMap) {
        return InterfaceMapper.createInterfaceMethodDTOList(interfaces, operationsDTOByInterfaceId,
                discoveredInterfaceMap, tcDTOMap);
    }

    private Map<Integer, OperationFullDTO> createAllOperationsDTO(List<Operation> allOperations,
                                                                  Map<Integer, Sla> slaMap,
                                                                  Map<Integer, List<DiscoveredOperation>> discoveredOperationMap,
                                                                  Map<Integer, TcDTO> tcDTOMap) {
        Map<Integer, OperationFullDTO> result = new HashMap<>();
        for (Operation operation : allOperations) {
            result.put(operation.getId(), OperationFullDTO.builder()
                    .id(operation.getId())
                    .description(operation.getDescription())
                    .name(operation.getName())
                    .type(operation.getType())
                    .mapicOperations(discoveredOperationMapper.createMapicOperationFullDTO(
                            discoveredOperationMap.get(operation.getId())))
                    .sla(createSlaV2DTO(slaMap.get(operation.getId())))
                    .techCapability(tcDTOMap.get(operation.getTcId()))
                    .createdDate(operation.getCreatedDate())
                    .updateDate(operation.getUpdatedDate())
                    .deletedDate(operation.getDeletedDate())
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

    private SlaV2DTO createSlaV2DTO(Sla sla) {
        if (sla != null) {
            return SlaV2DTO.builder().latency(sla.getLatency()).errorRate(sla.getErrorRate()).rps(sla.getRps()).build();
        }
        return null;
    }

    public List<ProductInfoShortDTO> getProductInfo() {
        List<UserProfileShortDTO> userProfileShortDTOS = new ArrayList<>();
        List<Product> products = productRepository.findAll();
        List<Integer> ownerIds = products.stream()
                .map(Product::getOwnerID)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (!ownerIds.isEmpty()) {
            userProfileShortDTOS = userClient.findUserProfilesByIdIn(ownerIds);
        }
        Map<Integer, UserProfileShortDTO> userProfileShortDTOMap = userProfileShortDTOS.stream()
                .collect(Collectors.toMap(
                        UserProfileShortDTO::getId,
                        obj -> obj
                ));
        return products.stream()
                .map(product -> ProductTechMapper.mapToProductInfoShortDTO(product,
                        product.getOwnerID() == null ? "" :
                                userProfileShortDTOMap.get(product.getOwnerID())
                                        .getFullName()))
                .collect(Collectors.toList());
    }

    public List<GetProductsByIdsDTO> getProductByIds(List<Integer> ids) {
        return ProductTechMapper.mapToGetProductsByIdsDTO(productRepository.findAllById(ids));
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
        SystemRelationDto result = SystemRelationDto.builder()
                .influencingSystems(new ArrayList<>())
                .dependentSystems(new ArrayList<>())
                .build();
        if (influences != null) {
            result.setDependentSystems(processSystems(influences.getDependentSystems(),
                    findProductsByAlias(lowerAliases(influences.getDependentSystems()))));
            result.setInfluencingSystems(processSystems(influences.getInfluencingSystems(),
                    findProductsByAlias(lowerAliases(influences.getInfluencingSystems()))));
        }
        return result;
    }

    private List<SystemInfoDTO> processSystems(List<String> systems, Map<String, Product> products) {
        if (systems == null) {
            return new ArrayList<>();
        }
        Map<Integer, UserProfileShortDTO> userProfiles = getUserProfiles(products);
        return systems.stream()
                .map(system -> ProductTechMapper.enrichSystemWithProduct(system, products, userProfiles))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private List<String> lowerAliases(List<String> list) {
        return list.stream().map(String::toLowerCase).collect(Collectors.toList());
    }

    private Map<String, Product> findProductsByAlias(List<String> Aliases) {
        return productRepository.findByAliasInIgnoreCase(Aliases)
                .stream()
                .collect(Collectors.toMap(p -> p.getAlias().toLowerCase(), Function.identity()));
    }

    private Map<Integer, UserProfileShortDTO> getUserProfiles(Map<String, Product> products) {
        List<UserProfileShortDTO> userProfiles = userClient.findUserProfiles(products.values()
                .stream()
                .map(Product::getOwnerID)
                .toList());
        return userProfiles.stream().collect(Collectors.toMap(UserProfileShortDTO::getId, Function.identity()));
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
        return Stream.concat(interfaces.stream().map(Interface::getTcId), operations.stream().map(Operation::getTcId))
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    public ApiKeyDTO getKey(Integer id) {
        Product product = productRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("not found"));
        if (!RequestContext.getUserProducts().contains(id)) {
            throw new ForbiddenException("403 Forbidden.");
        }
        return ApiKeyDTO.builder()
                .structurizrApiKey(product.getStructurizrApiKey())
                .structurizrApiSecret(product.getStructurizrApiSecret())
                .build();
    }

    public void updateOwnerAndPriority(String cmdb, String email, String fullName, String critical, String extId) {
        Product product = productRepository.findByAliasCaseInsensitive(cmdb);
        if (product == null) {
            throw new EntityNotFoundException("Продукт с данным cmdb не найден.");
        }
        UserInfoDTO userInfo = userClient.getUserInfo(email, fullName, extId);
        if (userInfo != null) {
            product.setCritical(critical);
            product.setOwnerID(userInfo.getId());
            productRepository.save(product);
        }
        if (!userProductRepository.existsByUserIdAndProductId(userInfo.getId(), product.getId())) {
            UserProduct userProduct = UserProduct.builder().userId(userInfo.getId()).product(product).build();
            userProductRepository.save(userProduct);
        }
    }

    public ProductInfoShortV2DTO getParent(Integer id, String type) {
        ProductInfoShortV2DTO result = null;
        switch (type) {
            case "arch_container" -> {
                result = ProductTechMapper.mapToProductInfoShortV2DTO(productRepository.findProductByContainerProductID(
                        id).orElseThrow(() -> new EntityNotFoundException("not" + " found")));
            }
            case "arch_interface" -> {
                result = ProductTechMapper.mapToProductInfoShortV2DTO(productRepository.findProductByInterfaceId(id)
                        .orElseThrow(() -> new EntityNotFoundException(
                                "not" + " found")));
            }
            case "arch_operation" -> {
                result = ProductTechMapper.mapToProductInfoShortV2DTO(productRepository.findProductByOperationID(id)
                        .orElseThrow(() -> new EntityNotFoundException(
                                "not" + " found")));
            }
            default -> throw new IllegalArgumentException("Не валидный аттрибут type");
        }
        return result;
    }

    public List<ResultDTO> getE2eProcessByCmdb(String cmdb) {
        List<ResultDTO> result = new ArrayList<>();
        Product product = getProductByCode(cmdb);
        if (product != null) {
            Set<E2eProcessInfoDTO> e2eProcessInfoDTOS = new HashSet<>(dashboardClient.getE2eSystemInfo(product.getAlias()));
            if (e2eProcessInfoDTOS.isEmpty()) {
                return new ArrayList<>();
            }
            log.info("e2eProcessInfoDTOSet size: " + e2eProcessInfoDTOS.size());
            for (E2eProcessInfoDTO e2eProcessInfoDTO : e2eProcessInfoDTOS) {
                List<GetInfoProcessDTO> infoProcessDTOS = dashboardClient.getInfoMessage(e2eProcessInfoDTO.getBi().getUid());
                List<String> filterInfoProcessDTOS = infoProcessDTOS.stream().
                        filter(infoProcessDTO -> e2eProcessInfoDTO.getMessage().getOperation().getUid()
                                .equals(infoProcessDTO.getOperation_guid()))
                        .map(GetInfoProcessDTO::getClient_code)
                        .filter(Objects::nonNull)
                        .toList();
                result.add(ResultDTO.builder()
                        .e2e(e2eProcessInfoDTO.getProcess().getName())
                        .operation(e2eProcessInfoDTO.getMessage().getOperation().getName())
                        .client(filterInfoProcessDTOS)
                        .build());
            }
            return result.stream()
                    .collect(Collectors.toMap(
                            resultDTO -> Arrays.asList(resultDTO.getE2e(), resultDTO.getOperation()),
                            resultDTO -> new HashSet<>(resultDTO.getClient()),
                            (set1, set2) -> {
                                set1.addAll(set2);
                                return set1;
                            },
                            LinkedHashMap::new
                    ))
                    .entrySet().stream()
                    .map(entry -> new ResultDTO(
                            entry.getKey().get(0),
                            entry.getKey().get(1),
                            new ArrayList<>(entry.getValue())
                    ))
                    .toList();
        }
        return result;
    }
}