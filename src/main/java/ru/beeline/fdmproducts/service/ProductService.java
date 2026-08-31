/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import ru.beeline.fdmproducts.client.*;
import ru.beeline.fdmproducts.domain.*;
import ru.beeline.fdmproducts.dto.*;
import ru.beeline.fdmproducts.dto.dashboard.E2eMethodUsageClientDTO;
import ru.beeline.fdmproducts.dto.dashboard.E2eMethodUsageDetailDTO;
import ru.beeline.fdmproducts.dto.dashboard.E2eMethodUsagesDTO;
import ru.beeline.fdmproducts.dto.dashboard.ResultDTO;
import ru.beeline.fdmproducts.dto.ffunction.FitnessFunctionDTO;
import ru.beeline.fdmproducts.dto.techradar.TechAdvancedGetDTO;
import ru.beeline.fdmproducts.exception.DatabaseConnectionException;
import ru.beeline.fdmproducts.exception.EntityNotFoundException;
import ru.beeline.fdmproducts.exception.ValidationException;
import ru.beeline.fdmproducts.mapper.*;
import ru.beeline.fdmproducts.repository.*;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
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
    private final ProductAvailabilityRepository productAvailabilityRepository;
    private final DiscoveredParameterRepository discoveredParameterRepository;
    private final LocalAssessmentRepository localAssessmentRepository;
    private final LocalAssessmentCheckRepository localAssessmentCheckRepository;
    private final OperationRelationRepository operationRelationRepository;
    private final ProductBranchRepository productBranchRepository;

    private static final String DEFAULT_BRANCH = "main";

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
                          DiscoveredOperationRepository discoveredOperationRepository,
                          DashboardClient dashboardClient,
                          LocalAcObjectRepository localAcObjectRepository,
                          LocalAcObjectDetailRepository localAcObjectDetailRepository,
                          ProductAvailabilityRepository productAvailabilityRepository,
                          DiscoveredParameterRepository discoveredParameterRepository,
                          LocalAssessmentRepository localAssessmentRepository,
                          LocalAssessmentCheckRepository localAssessmentCheckRepository,
                          OperationRelationRepository operationRelationRepository,
                          ProductBranchRepository productBranchRepository) {
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
        this.productAvailabilityRepository = productAvailabilityRepository;
        this.discoveredParameterRepository = discoveredParameterRepository;
        this.localAssessmentRepository = localAssessmentRepository;
        this.localAssessmentCheckRepository = localAssessmentCheckRepository;
        this.operationRelationRepository = operationRelationRepository;
        this.productBranchRepository = productBranchRepository;
    }

    private String normalizeBranch(String branch) {
        return (branch == null || branch.isBlank()) ? DEFAULT_BRANCH : branch.trim().toLowerCase(Locale.ROOT);
    }

    private Optional<ProductBranch> findBranch(String alias, String branch) {
        return productBranchRepository.findByAliasAndBranchName(alias, normalizeBranch(branch));
    }

    private Integer getOrCreateBranchId(String alias, String branch) {
        return productBranchRepository.upsert(alias, normalizeBranch(branch));
    }

    public List<Product> getProductsByUser(Integer userId) {
        return userProductRepository.findAllByUserId(userId)
                .stream()
                .map(UserProduct::getProduct)
                .collect(Collectors.toList());
    }

    public List<ProductAuthDTO> getProductsForAuth(Integer userId) {
        return userProductRepository.findProductsForAuthByUserId(userId);
    }

    public List<Product> getProductsByUserAdmin(Integer userId) {
        UserInfoDTO userInfo = userClient.getUserInfoById(userId);
        boolean isAdmin = userInfo != null && userInfo.getRoles() != null
                && userInfo.getRoles().contains("ADMINISTRATOR");
        if (isAdmin) {
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
        UserProfileDTO user = new UserProfileDTO();
        if (product.getOwnerID() != null) {
            user = userClient.findUserProfilesById(product.getOwnerID());
        }
        return ProductTechMapper.mapToProductInfoDTO(product, user);
    }

    public ProductInfoV2DTO getProductInfoByCodeV2(String code) {
        if (code == null || code.equals("\n") || code.equals(" \n")) {
            throw new IllegalArgumentException("Параметр alias не должен быть пустым.");
        }
        Product product = productRepository.findByAliasCaseInsensitive(code);
        if (product == null) {
            throw new EntityNotFoundException((String.format("Продукт c alias '%s' не найден", code)));
        }
        UserProfileDTO user = new UserProfileDTO();
        if (product.getOwnerID() != null) {
            user = userClient.findUserProfilesById(product.getOwnerID());
        }

        List<TechAdvancedGetDTO> techAdvancedGetDTOS = techradarClient.getTechById(product.getTechProducts()
                .stream()
                .filter(techProduct -> techProduct.getDeletedDate() == null)
                .map(TechProduct::getTechId)
                .collect(Collectors.toList()));

        Map<Integer, TechAdvancedGetDTO> techAdvancedGetDTOMap =
                techAdvancedGetDTOS.stream()
                        .collect(Collectors.toMap(TechAdvancedGetDTO::getId,
                                Function.identity()));
        return ProductTechMapper.mapToProductInfoV2DTO(product, user, techAdvancedGetDTOMap);
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

    public void updateProduct(PutUpdateProductDTO putUpdateProductDTO) {
        Product product = productRepository.findByAliasCaseInsensitive(putUpdateProductDTO.getAlias());
        if (product == null) {
            product = Product.builder()
                    .alias(putUpdateProductDTO.getAlias())
                    .name(putUpdateProductDTO.getName())
                    .description(putUpdateProductDTO.getDescription())
                    .gitUrl(putUpdateProductDTO.getGitUrl())
                    .critical(putUpdateProductDTO.getCritical())
                    .ownerID(putUpdateProductDTO.getOwnerId())
                    .build();
            productRepository.save(product);
            List<Integer> employeesIds = prepareEmployeesIds(putUpdateProductDTO);
            synchronizeUserProducts(product, employeesIds);
        } else {
            updateProductIfChanged(product, putUpdateProductDTO);
            updateUserProductRelations(product, putUpdateProductDTO);
        }
    }

    @Transactional
    public void updateUserProductRelations(Product product, PutUpdateProductDTO request) {
        List<Integer> employeesIds = prepareEmployeesIds(request);
        List<UserProduct> existing = userProductRepository.findAllByProductId(product.getId());
        Set<Integer> targetUserIds = new HashSet<>(employeesIds);
        Set<Integer> existingUserIds = existing.stream()
                .map(UserProduct::getUserId)
                .collect(Collectors.toSet());
        List<UserProduct> toRemove = existing.stream()
                .filter(up -> !targetUserIds.contains(up.getUserId()))
                .toList();
        List<UserProduct> toCreate = targetUserIds.stream()
                .filter(userId -> !existingUserIds.contains(userId))
                .map(userId -> UserProduct.builder()
                        .userId(userId)
                        .product(product)
                        .build())
                .toList();
        if (!toCreate.isEmpty()) {
            userProductRepository.saveAll(toCreate);
        }
        if (!toRemove.isEmpty()) {
            userProductRepository.deleteAll(toRemove);
        }
    }

    private void synchronizeUserProducts(Product product, List<Integer> employeesIds) {
        List<UserProduct> userProducts = new ArrayList<>();
        for (Integer employeesId : employeesIds) {
            userProducts.add(UserProduct.builder().userId(employeesId)
                    .product(product).build());
        }
        userProductRepository.saveAll(userProducts);
    }

    private List<Integer> prepareEmployeesIds(PutUpdateProductDTO putUpdateProductDTO) {
        List<Integer> employeesIds = putUpdateProductDTO.getEmployeesIds();
        if (employeesIds != null) {
            if (putUpdateProductDTO.getOwnerId() != null) {
                employeesIds.add(putUpdateProductDTO.getOwnerId());
            }
            return employeesIds.stream().distinct().filter(Objects::nonNull).toList();
        }
        return new ArrayList<>();
    }

    private void updateProductIfChanged(Product product, PutUpdateProductDTO putUpdateProductDTO) {
        boolean changed = false;
        if (!Objects.equals(product.getName(), putUpdateProductDTO.getName())) {
            product.setName(putUpdateProductDTO.getName());
            changed = true;
        }
        if (!Objects.equals(product.getDescription(), putUpdateProductDTO.getDescription())) {
            product.setDescription(putUpdateProductDTO.getDescription());
            changed = true;
        }
        if (!Objects.equals(product.getGitUrl(), putUpdateProductDTO.getGitUrl())) {
            product.setGitUrl(putUpdateProductDTO.getGitUrl());
            changed = true;
        }
        if (!Objects.equals(product.getCritical(), putUpdateProductDTO.getCritical())) {
            product.setCritical(putUpdateProductDTO.getCritical());
            changed = true;
        }
        if (!Objects.equals(product.getOwnerID(), putUpdateProductDTO.getOwnerId())) {
            product.setOwnerID(putUpdateProductDTO.getOwnerId());
            changed = true;
        }
        if (changed) {
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

    @Transactional
    public void replaceUserProducts(List<String> aliasList, Integer userId) {
        if (aliasList.isEmpty()) {
            throw new IllegalArgumentException("400: Массив пустой. ");
        }
        Map<String, Product> productsByLowerAlias = findProductsByAlias(lowerAliases(aliasList));
        List<String> notFoundAliases = new ArrayList<>();
        Set<Product> targetProducts = new LinkedHashSet<>();
        for (String alias : aliasList) {
            Product product = productsByLowerAlias.get(alias.toLowerCase());
            if (product != null) {
                targetProducts.add(product);
            } else {
                notFoundAliases.add(alias);
            }
            if (notFoundAliases.size() == aliasList.size()) {
                throw new IllegalArgumentException("Ни один из продуктов не найден.");
            }
        }
        Set<Integer> targetProductIds = targetProducts.stream()
                .map(Product::getId)
                .collect(Collectors.toSet());
        List<UserProduct> currentLinks = userProductRepository.findAllByUserId(userId);
        int removed = 0;
        for (UserProduct link : currentLinks) {
            if (!targetProductIds.contains(link.getProduct().getId())) {
                userProductRepository.delete(link);
                removed++;
            }
        }
        int added = 0;
        for (Product product : targetProducts) {
            if (!userProductRepository.existsByUserIdAndProductId(userId, product.getId())) {
                UserProduct userProduct = UserProduct.builder().userId(userId).product(product).build();
                userProductRepository.save(userProduct);
                added++;
            }
        }
        log.info("replaceUserProducts userId={}: получено алиасов={}, не найдено={}, было связей={}, удалено={}, добавлено={}",
                userId, aliasList.size(), notFoundAliases.size(), currentLinks.size(), removed, added);
    }

    public void validateProductPutDto(ProductPutDto productPutDto) {
        StringBuilder errMsg = new StringBuilder();
        if (productPutDto.getName() == null || productPutDto.getName().equals("")) {
            errMsg.append("409 Ошибка валидации тела запроса: Отсутствует обязательное поле name");
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
            throw new ValidationException("409 Ошибка валидации тела запроса: " + errMsg);
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

    public List<GetProductsByIdsDTO> getProductByAliases(List<String> aliases) {
        if (aliases == null) {
            throw new IllegalArgumentException("Не передан обязательный параметр aliases");
        }
        List<String> normalizedAliases = normalizeAliases(aliases);
        if (normalizedAliases.isEmpty()) {
            throw new IllegalArgumentException("Параметр aliases не может быть пустым");
        }
        return ProductTechMapper.mapToGetProductsByIdsDTO(
                productRepository.findByAliasInIgnoreCase(normalizedAliases));
    }

    private List<String> normalizeAliases(List<String> aliases) {
        return aliases.stream()
                .filter(alias -> alias != null && !alias.isEmpty())
                .map(String::toLowerCase)
                .distinct()
                .collect(Collectors.toList());
    }

    private void apiKeyValidate(String apiKey) {
        if (apiKey == null) {
            throw new IllegalArgumentException("Параметр api-key не должен быть пустым.");
        }
    }

    public ValidationErrorResponse createOrUpdateProductRelations(List<ContainerDTO> containerDTOS,
                                                                  String code,
                                                                  String branch,
                                                                  String source) {
        log.info("Старт метода Создание, обновление связей продукта с code: {}, branch: {}", code, branch);
        log.debug("тело запроса: source={}, body={}", source, containerDTOS);
        ValidationErrorResponse errorEntity = new ValidationErrorResponse();
        validateContainers(containerDTOS, errorEntity);
        validateInterfaces(containerDTOS, errorEntity);
        validateMethods(containerDTOS, errorEntity);
        Product product = getProductByCode(code);
        Integer productBranchId = getOrCreateBranchId(product.getAlias(), branch);
        if (!containerDTOS.isEmpty()) {
            log.info("Обработка контейнеров продукта с code: " + code);
            saveRelations(containerDTOS, productBranchId);
        } else {
            log.info("Пустой список, каскадное удаление данных о продукте: {}", code);
            deleteAllContainers(productBranchId);
        }
        product.setSource(source);
        product.setUploadDate(LocalDateTime.now());
        productRepository.save(product);
        log.info("Метода Создание, обновление связей продукта с code: {} завершен.", code);
        return errorEntity;
    }

    private void deleteAllContainers(Integer productBranchId) {
        LocalDateTime deleteDateNow = LocalDateTime.now();
        List<Integer> containerIds = containerRepository.findContainerIdsByProductBranchIdAndDeletedDateIsNull(productBranchId);
        if (containerIds.isEmpty()) {
            return;
        }
        List<Interface> interfaces = interfaceRepository.findAllByContainerIdInAndDeletedDateIsNull(containerIds);
        if (interfaces.isEmpty()) {
            containerRepository.markAllContainersAsDeleted(productBranchId, new Date());
            log.info("Удаление Containers с productBranchId: {}", productBranchId);
            return;
        }
        List<Integer> interfaceIds = interfaces.stream().map(Interface::getId).toList();
        List<Operation> operations = operationRepository.findAllByInterfaceIdInAndDeletedDateIsNull(interfaceIds);
        Map<Integer, Integer> interfaceIdByOperationId = operations.stream()
                .collect(Collectors.toMap(Operation::getId, Operation::getInterfaceId));
        Map<Integer, Integer> containerIdByInterfaceId = interfaces.stream()
                .collect(Collectors.toMap(Interface::getId, Interface::getContainerId));
        Set<Integer> operationIdsInUse = findOperationIdsInUse(new ArrayList<>(interfaceIdByOperationId.keySet()));
        Set<Integer> protectedContainerIds = operationIdsInUse.stream()
                .map(interfaceIdByOperationId::get)
                .map(containerIdByInterfaceId::get)
                .collect(Collectors.toSet());
        if (!protectedContainerIds.isEmpty()) {
            log.info("Containers не будут удалены т.к. их operation учавствуют в operation_relations: {}",
                    protectedContainerIds);
        }
        List<Integer> containersToDelete = containerIds.stream()
                .filter(id -> !protectedContainerIds.contains(id))
                .toList();
        if (containersToDelete.isEmpty()) {
            return;
        }
        Set<Integer> containersToDeleteSet = new HashSet<>(containersToDelete);
        List<Integer> interfacesToDelete = interfaces.stream()
                .filter(i -> containersToDeleteSet.contains(i.getContainerId()))
                .map(Interface::getId)
                .toList();
        Set<Integer> interfacesToDeleteSet = new HashSet<>(interfacesToDelete);
        List<Integer> operationsToDelete = operations.stream()
                .filter(o -> interfacesToDeleteSet.contains(o.getInterfaceId()))
                .map(Operation::getId)
                .toList();
        if (!operationsToDelete.isEmpty()) {
            parameterRepository.markAllParametersAsDeleted(operationsToDelete, deleteDateNow);
            log.info("Удаление Parameters, operationIds size: {} шт.", operationsToDelete.size());
            operationRepository.markOperationsAsDeletedByIds(operationsToDelete, deleteDateNow);
            log.info("Удаление Operations, count: {} шт.", operationsToDelete.size());
        }
        if (!interfacesToDelete.isEmpty()) {
            interfaceRepository.markInterfacesAsDeletedByIds(interfacesToDelete, deleteDateNow);
            log.info("Удаление Interfaces, count: {} шт.", interfacesToDelete.size());
        }
        containerRepository.markContainersAsDeletedByIds(containersToDelete, new Date());
        log.info("Удаление Containers, count: {} шт.", containersToDelete.size());
    }

    private Set<Integer> findOperationIdsInUse(List<Integer> operationIds) {
        if (operationIds == null || operationIds.isEmpty()) {
            return Collections.emptySet();
        }
        return new HashSet<>(operationRelationRepository.findOperationIdsInUse(operationIds));
    }

    private void validateContainers(List<ContainerDTO> containers, ValidationErrorResponse errorEntity) {
        log.info("Валидация контейнеров");
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
        log.info("Валидация интерфейсов");
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
        if (protocol == null) {
            method.setType(null);
            return;
        }
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
            default -> method.setType(null);
        }
    }

    public void saveRelations(List<ContainerDTO> containerDTOS, Integer productBranchId) {
        Map<String, ContainerProduct> existingContainers = containerRepository.findAllByCodeInAndProductBranchId(containerDTOS.stream()
                                .map(ContainerDTO::getCode)
                                .toList(),
                        productBranchId)
                .stream()
                .collect(Collectors.toMap(ContainerProduct::getCode, c -> c,
                        (a, b) -> a.getDeletedDate() == null ? a : b));
        List<ContainerProduct> toSave = new ArrayList<>();
        Map<String, List<InterfaceDTO>> interfacesByCode = new HashMap<>();
        List<InterfaceDTO> allInterfaces = new ArrayList<>();
        List<MethodDTO> allMethods = new ArrayList<>();
        prepareContainersAndCollectData(containerDTOS,
                productBranchId,
                existingContainers,
                toSave,
                interfacesByCode,
                allInterfaces,
                allMethods);
        if (!toSave.isEmpty()) {
            log.info("Сохранение контейнеров. Количество: " + toSave.size());
            containerRepository.saveAll(toSave);
        }
        markContainersAsDeleted(productBranchId, containerDTOS);
        log.info("TC: загрузка capability для productBranchId={}, interfaces={}, methods={}",
                productBranchId, allInterfaces.size(), allMethods.size());
        Map<String, Long> codesIdMap = loadInterfaceCapabilityMap(allInterfaces);
        Map<String, Long> methodCodesIdMap = loadMethodCapabilityMap(allMethods);
        log.info("TC: карты загружены, interfaceMapSize={}, methodMapSize={}",
                codesIdMap.size(), methodCodesIdMap.size());
        Map<Integer, List<InterfaceDTO>> containerInterfaces = buildContainerInterfacesMap(existingContainers,
                toSave,
                interfacesByCode);
        for (Map.Entry<Integer, List<InterfaceDTO>> entry : containerInterfaces.entrySet()) {
            processInterfaces(entry.getValue(), entry.getKey(), codesIdMap, methodCodesIdMap);
            markInterfacesAsDeleted(entry.getKey(), entry.getValue());
        }
    }

    private void prepareContainersAndCollectData(List<ContainerDTO> containerDTOS, Integer productBranchId,
                                                 Map<String, ContainerProduct> existingContainers,
                                                 List<ContainerProduct> toSave, Map<String, List<InterfaceDTO>> interfacesByCode,
                                                 List<InterfaceDTO> allInterfaces,
                                                 List<MethodDTO> allMethods) {
        String container = "Container";
        for (ContainerDTO dto : containerDTOS) {
            validateField(dto.getName(), container, "name");
            validateField(dto.getCode(), container, "code");
            ContainerProduct containerEntity = existingContainers.get(dto.getCode());
            if (containerEntity == null) {
                containerEntity = containerMapper.convertToContainerProduct(dto, productBranchId);
                toSave.add(containerEntity);
            } else {
                if (!Objects.equals(containerEntity.getName(),
                        dto.getName()) || !Objects.equals(containerEntity.getVersion(), dto.getVersion())) {
                    containerMapper.updateContainerProduct(containerEntity, dto, productBranchId);
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
        List<String> allInterfaceCodes = allInterfaces.stream().map(InterfaceDTO::getCapabilityCode).distinct().toList();
        log.info("Загрузка tc для Interface: запрос к сервису capability, получить tcId по codes={}", allInterfaceCodes);
        Map<String, Long> result = capabilityClient.getIdCodes(allInterfaceCodes)
                .stream()
                .collect(Collectors.toMap(IdCodeDTO::getCode, IdCodeDTO::getId, (a, b) -> b,
                        () -> new TreeMap<>(String.CASE_INSENSITIVE_ORDER)));
        log.info("TC interfaces: получена карта code->tcId={}", result);
        log.info("Находим коды, для которых не нашлись ID");
        List<String> unresolved = allInterfaceCodes.stream()
                .filter(Objects::nonNull)
                .filter(code -> !result.containsKey(code))
                .toList();
        if (!unresolved.isEmpty()) {
            log.warn("!!!⚠️ Найдены коды, для которых не нашлось tcId, для codes: {}", unresolved);
            emptyCodes(unresolved);
        }
        return result;
    }

    private Map<String, Long> loadMethodCapabilityMap(List<MethodDTO> allMethods) {
        List<String> allMethodCodes = allMethods.stream()
                .map(MethodDTO::getCapabilityCode)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        log.info("Загрузка tc для Method: запрос к сервису capability, получить tcId по codes={}", allMethodCodes);
        Map<String, Long> result = capabilityClient.getIdCodes(allMethodCodes)
                .stream()
                .collect(Collectors.toMap(IdCodeDTO::getCode, IdCodeDTO::getId, (a, b) -> b,
                        () -> new TreeMap<>(String.CASE_INSENSITIVE_ORDER)));
        log.info("Загрузка tc для Method: получена карта code->tcId={}", result);
        List<String> unresolved = allMethodCodes.stream()
                .filter(code -> !result.containsKey(code))
                .distinct()
                .toList();
        if (!unresolved.isEmpty()) {
            emptyCodes(unresolved);
        }
        return result;
    }

    private void emptyCodes(List<String> unresolved) {
        List<String> emptyCodes = unresolved.stream()
                .filter(code -> code == null || code.trim().isEmpty())
                .toList();
        if (!emptyCodes.isEmpty()) {
            log.warn("!!!⚠️ Обнаружены пустые значения capabilityCode: (количество: {})", emptyCodes.size());
        }
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

    private void markContainersAsDeleted(Integer productBranchId, List<ContainerDTO> newContainers) {
        Set<String> dtoCodes = newContainers.stream().map(ContainerDTO::getCode).collect(Collectors.toSet());
        List<ContainerProduct> candidates = containerRepository.findAllByProductBranchIdAndDeletedDateIsNull(productBranchId)
                .stream()
                .filter(c -> !dtoCodes.contains(c.getCode()))
                .toList();
        for (ContainerProduct container : candidates) {
            if (isContainerProtected(container.getId())) {
                log.info("Container code={} не удалён т.к. его operation учавствуют в operation_relations",
                        container.getCode());
                continue;
            }
            container.setDeletedDate(new Date());
            containerRepository.save(container);
            cascadeDeleteContainer(container);
        }
    }

    private boolean isContainerProtected(Integer containerId) {
        List<Integer> interfaceIds = interfaceRepository.findInterfaceIdsByContainerIdInAndDeletedDateIsNull(
                List.of(containerId));
        if (interfaceIds.isEmpty()) {
            return false;
        }
        List<Integer> operationIds = operationRepository.findOperationIdsByInterfaceIdInAndDeletedDateIsNull(
                interfaceIds);
        return !findOperationIdsInUse(operationIds).isEmpty();
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
        List<Interface> candidates = allDbInterfaces.stream()
                .filter(dbIntf -> !dtoCodes.contains(dbIntf.getCode()))
                .filter(dbIntf -> dbIntf.getDeletedDate() == null)
                .toList();
        if (candidates.isEmpty()) {
            return;
        }
        List<Integer> candidateInterfaceIds = candidates.stream().map(Interface::getId).toList();
        Map<Integer, List<Integer>> operationIdsByInterface = operationRepository.findAllByInterfaceIdInAndDeletedDateIsNull(
                        candidateInterfaceIds)
                .stream()
                .collect(Collectors.groupingBy(Operation::getInterfaceId,
                        Collectors.mapping(Operation::getId, Collectors.toList())));
        Set<Integer> operationIdsInUse = findOperationIdsInUse(operationIdsByInterface.values()
                .stream()
                .flatMap(List::stream)
                .toList());
        List<Interface> toDelete = new ArrayList<>();
        for (Interface interfaceObj : candidates) {
            boolean protectedInterface = operationIdsByInterface.getOrDefault(interfaceObj.getId(), List.of())
                    .stream()
                    .anyMatch(operationIdsInUse::contains);
            if (protectedInterface) {
                log.info("Interface code={} не удалён т.к. его operation учавствуют в operation_relations",
                        interfaceObj.getCode());
                continue;
            }
            interfaceObj.setDeletedDate(LocalDateTime.now());
            toDelete.add(interfaceObj);
        }
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
        List<Operation> candidates = allDbOperations.stream()
                .filter(op -> !newKeys.contains(op.getName() + "::" + (op.getType() != null ? op.getType() : "")))
                .filter(op -> op.getDeletedDate() == null)
                .toList();
        if (candidates.isEmpty()) {
            return;
        }
        Set<Integer> operationIdsInUse = findOperationIdsInUse(candidates.stream().map(Operation::getId).toList());
        List<Operation> toDelete = new ArrayList<>();
        for (Operation op : candidates) {
            if (operationIdsInUse.contains(op.getId())) {
                log.info("Operation id={} не удалена т.к. учавствует в operation_relations", op.getId());
                continue;
            }
            op.setDeletedDate(LocalDateTime.now());
            toDelete.add(op);
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

    private Interface createOrUpdateInterfaceObject(InterfaceDTO dto, Integer containerId, Map<String, Long> codesIdMap,
                                                    Map<String, Interface> existingInterfaces, List<Interface> toSave,
                                                    String method) {
        validateField(dto.getName(), method, "name");
        validateField(dto.getCode(), method, "code");
        if (dto.getCapabilityCode() == null) {
            throw new IllegalArgumentException("Capability code is empty");
        }
        Integer tcId = codesIdMap.get(dto.getCapabilityCode()) != null ? codesIdMap.get(dto.getCapabilityCode())
                .intValue() : null;
        log.info("TC save interface: code={}, capabilityCode={}, tcId={}, containerId={}",
                dto.getCode(), dto.getCapabilityCode(), tcId, containerId);
        Interface interfaceObj = existingInterfaces.get(dto.getCode());
        if (interfaceObj == null) {
            interfaceObj = InterfaceMapper.convertToInterface(dto, containerId, tcId);
            toSave.add(interfaceObj);
            log.info("TC save interface: создан новый, code={}, tcId={}", dto.getCode(), tcId);
        } else {
            if (interfaceObj.getDeletedDate() != null) {
                interfaceObj.setDeletedDate(null);
                interfaceObj.setUpdatedDate(LocalDateTime.now());
                toSave.add(interfaceObj);
            }
            if (!equalsInterfaces(interfaceObj, dto, tcId)) {
                Integer previousTcId = interfaceObj.getTcId();
                InterfaceMapper.updateInterface(interfaceObj, dto, containerId, tcId);
                toSave.add(interfaceObj);
                log.info("TC save interface: обновлён code={}, tcId {} -> {}", dto.getCode(), previousTcId, tcId);
            } else {
                log.info("TC save interface: без изменений code={}, tcId={}", dto.getCode(), interfaceObj.getTcId());
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

    private Operation createOrUpdateMethod(MethodDTO methodDTO, Integer interfaceId, Integer tcIdInterface,
                                           Operation existingOperation, List<Operation> operationsToSave,
                                           Map<String, Long> methodCodesIdMap) {
        Integer tcId = null;
        String tcSource = "none";
        if (methodDTO.getCapabilityCode() != null) {
            Long tcIdLong = methodCodesIdMap.get(methodDTO.getCapabilityCode());
            tcId = (tcIdLong != null) ? tcIdLong.intValue() : null;
            tcSource = tcId != null ? "method.capabilityCode" : "method.capabilityCode(not found)";
        }
        if (tcId == null) {
            tcId = tcIdInterface;
            tcSource = "interface.tcId";
        }
        log.debug("TC save method: name={}, type={}, capabilityCode={}, tcId={}, source={}, interfaceId={}",
                methodDTO.getName(), methodDTO.getType(), methodDTO.getCapabilityCode(), tcId, tcSource, interfaceId);
        if (existingOperation == null) {
            Operation newOperation = operationMapper.convertToOperation(methodDTO, interfaceId, tcId);
            operationsToSave.add(newOperation);
            log.debug("TC save method: создана новая операция name={}, type={}, tcId={}",
                    methodDTO.getName(), methodDTO.getType(), tcId);
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
                Integer previousTcId = existingOperation.getTcId();
                operationMapper.updateOperation(existingOperation, methodDTO, tcId, interfaceId);
                operationsToSave.add(existingOperation);
                log.debug("TC save method: обновлена операция name={}, type={}, tcId {} -> {}",
                        methodDTO.getName(), methodDTO.getType(), previousTcId, tcId);
            } else {
                log.debug("TC save method: без изменений name={}, type={}, tcId={}",
                        methodDTO.getName(), methodDTO.getType(), existingOperation.getTcId());
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
            throw new ValidationException(String.format("409 Ошибка валидации тела запроса: Отсутствует обязательное поле '%s': %s",
                    entityName,
                    fieldName));
        }
    }

    private Boolean equalsInterfaces(Interface getInterface, InterfaceDTO interfaceDTO, Integer tcId) {
        return Objects.equals(getInterface.getName(), interfaceDTO.getName())
                && Objects.equals(getInterface.getVersion(), interfaceDTO.getVersion())
                && Objects.equals(getInterface.getSpecLink(), interfaceDTO.getSpecLink())
                && Objects.equals(getInterface.getTcId(), tcId)
                && Objects.equals(getInterface.getProtocol(), interfaceDTO.getProtocol());
    }

    public List<GetProductTechDto> getAllProductsAndTechRelations() {
        try {
            List<TechProduct> techProducts = techProductRepository.findAllByDeletedDateIsNull();
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
            throw new ValidationException("409 Ошибка валидации тела запроса: " + errMsg.toString().trim());
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
                .filter(c -> Boolean.TRUE.equals(c.getIsCheck()))
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

    public List<ProductMapicInterfaceDTO> getInterfacesBySource(String cmdb, String sourceType, Boolean showHidden) {
        String decodedCmdb = URLDecoder.decode(cmdb, StandardCharsets.UTF_8);
        String decodedSourceType = URLDecoder.decode(sourceType, StandardCharsets.UTF_8);
        if (!StringUtils.hasText(decodedSourceType)) {
            throw new IllegalArgumentException("Отсутствует обязательный параметр source-type");
        }
        Product product = productRepository.findByAliasCaseInsensitive(decodedCmdb);
        if (product == null) {
            throw new EntityNotFoundException("Продукт с данным cmdb: " + decodedCmdb + " не найден.");
        }
        List<DiscoveredInterface> discoveredInterfaces = showHidden
                ? discoveredInterfaceRepository.findAllByProductAndSourceIgnoreCase(product, decodedSourceType)
                : discoveredInterfaceRepository.findAllByProductAndSourceIgnoreCaseAndDeletedDateIsNull(product, decodedSourceType);
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

    /** Same as {@link #getInterfacesBySource}, but externalId in the response is String, not Integer — see ProductMapicInterfaceV2DTO. */
    public List<ProductMapicInterfaceV2DTO> getInterfacesBySourceV2(String cmdb, String sourceType, Boolean showHidden) {
        String decodedCmdb = URLDecoder.decode(cmdb, StandardCharsets.UTF_8);
        String decodedSourceType = URLDecoder.decode(sourceType, StandardCharsets.UTF_8);
        if (!StringUtils.hasText(decodedSourceType)) {
            throw new IllegalArgumentException("Отсутствует обязательный параметр source-type");
        }
        Product product = productRepository.findByAliasCaseInsensitive(decodedCmdb);
        if (product == null) {
            throw new EntityNotFoundException("Продукт с данным cmdb: " + decodedCmdb + " не найден.");
        }
        List<DiscoveredInterface> discoveredInterfaces = showHidden
                ? discoveredInterfaceRepository.findAllByProductAndSourceIgnoreCase(product, decodedSourceType)
                : discoveredInterfaceRepository.findAllByProductAndSourceIgnoreCaseAndDeletedDateIsNull(product, decodedSourceType);
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
        return buildProductMapicInterfaceV2DTOs(discoveredInterfaces, discoveredOperationsByInterfaceId, operationsById);
    }

    private List<ProductMapicInterfaceV2DTO> buildProductMapicInterfaceV2DTOs(List<DiscoveredInterface> discoveredInterfaces,
                                                                              Map<Integer, List<DiscoveredOperation>> discoveredOperationsByInterfaceId,
                                                                              Map<Integer, Operation> operationsById) {
        return discoveredInterfaces.stream().map(discoveredInterface -> {
                    ProductMapicInterfaceV2DTO dto = InterfaceMapper.createProductMapicInterfaceV2(discoveredInterface);
                    List<DiscoveredOperation> interfaceDiscoveredOperations = discoveredOperationsByInterfaceId.getOrDefault(
                            discoveredInterface.getId(), Collections.emptyList());
                    if (interfaceDiscoveredOperations != null && !interfaceDiscoveredOperations.isEmpty()) {
                        dto.setContextProvider(interfaceDiscoveredOperations.get(0).getContext());
                    }
                    List<ConnectOperationDTO> operationDTOs = interfaceDiscoveredOperations.stream()
                            .map(discoveredOperation -> {
                                Operation operation = discoveredOperation.getConnectionOperationId() != null
                                        ? operationsById.get(discoveredOperation.getConnectionOperationId())
                                        : null;
                                return InterfaceMapper.createConnectOperationDTO(operation, discoveredOperation);
                            })
                            .sorted(Comparator.comparing(ConnectOperationDTO::getCreateDate).reversed())
                            .collect(Collectors.toList());
                    dto.setOperations(operationDTOs);
                    dto.setConnectInterface(InterfaceMapper.createMapicInterfaceDTO(discoveredInterface,
                            discoveredInterface.getConnectedInterface()));
                    return dto;
                }).sorted(Comparator.comparing(ProductMapicInterfaceV2DTO::getCreateDate).reversed())
                .collect(Collectors.toList());
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
                            .sorted(Comparator.comparing(ConnectOperationDTO::getCreateDate).reversed())
                            .collect(Collectors.toList());
                    dto.setOperations(operationDTOs);
                    dto.setConnectInterface(InterfaceMapper.createMapicInterfaceDTO(discoveredInterface,
                            discoveredInterface.getConnectedInterface()));
                    return dto;
                }).sorted(Comparator.comparing(ProductMapicInterfaceDTO::getCreateDate).reversed())
                .collect(Collectors.toList());
    }

    public List<ProductInterfaceDTO> getProductsFromStructurizr(String cmdb, String branch) {
        Product product = productRepository.findByAliasCaseInsensitive(cmdb);
        if (product == null) {
            throw new EntityNotFoundException("Продукт с данным cmdb: " + cmdb + " не найден.");
        }
        List<ProductInterfaceDTO> result = new ArrayList<>();
        Optional<ProductBranch> productBranch = findBranch(product.getAlias(), branch);
        if (productBranch.isEmpty()) {
            return result;
        }
        List<ContainerProduct> containerProducts = containerRepository.findAllByProductBranchIdAndDeletedDateIsNull(
                productBranch.get().getId());
        if (!containerProducts.isEmpty()) {
            List<Interface> interfaces = containerProducts.stream().flatMap(cp -> cp.getInterfaces().stream())
                    .filter(iface -> iface.getDeletedDate() == null)
                    .toList();
            if (!interfaces.isEmpty()) {
                List<Integer> interfaceIds = interfaces.stream().map(Interface::getId).toList();
                List<Interface> withDiscovered = interfaceRepository.findAllByIdIn(interfaceIds);
                List<DiscoveredInterface> allDdInterfaces = discoveredInterfaceRepository.findAllByConnectionInterfaceIdIn(
                        interfaceIds);
                Map<Integer, List<DiscoveredInterface>> discoveredInterfaceMap = getDiscoveredInterfaceMap(allDdInterfaces);
                List<Operation> allOperations = operationRepository.findAllByInterfaceIdInAndDeletedDateIsNull(
                        interfaceIds);
                Map<Integer, List<Operation>> operatioMap = getOperatioMap(allOperations);
                List<Integer> operationIds = allOperations.stream().map(Operation::getId).toList();
                List<DiscoveredOperation> discoveredOperations = discoveredOperationRepository.findAllByConnectionOperationIdIn(operationIds);
                Map<Integer, List<DiscoveredOperation>> discoveredOperationMap = getDiscoveredOperationMap(discoveredOperations);
                for (Interface interfaceObj : withDiscovered) {
                    ProductInterfaceDTO productInterfaceDTO = InterfaceMapper.createProductInterface(interfaceObj);
                    List<DiscoveredInterface> dInterfaces = discoveredInterfaceMap.get(interfaceObj.getId());
                    if (dInterfaces != null && !dInterfaces.isEmpty()) {
                        List<MapicInterfaceDTO> mapicInterfaceDTOS = new ArrayList<>();
                        dInterfaces.forEach(discoveredInterface -> mapicInterfaceDTOS.add(InterfaceMapper.createMapicInterfaceDTO(
                                discoveredInterface)));
                        productInterfaceDTO.setMapicInterfaces(mapicInterfaceDTOS);
                    }
                    List<Operation> operations = operatioMap.get(interfaceObj.getId());
                    if (operations != null && !operations.isEmpty()) {
                        List<OperationDTO> operationDTOS = new ArrayList<>();
                        for (Operation operation : operations) {
                            operationDTOS.add(operationMapper.createOperationDTO(operation,
                                    discoveredOperationMap.get(operation.getId())));
                        }
                        productInterfaceDTO.setOperations(operationDTOS);
                    }
                    result.add(productInterfaceDTO);
                }
            }
        }
        return result;
    }

    private Map<Integer, List<DiscoveredInterface>> getDiscoveredInterfaceMap(List<DiscoveredInterface> allDdInterfaces) {
        return allDdInterfaces.stream()
                .collect(Collectors.groupingBy(
                        DiscoveredInterface::getConnectionInterfaceId,
                        Collectors.toList()
                ));
    }

    private Map<Integer, List<DiscoveredOperation>> getDiscoveredOperationMap(List<DiscoveredOperation> discoveredOperations) {
        return discoveredOperations.stream()
                .collect(Collectors.groupingBy(
                        DiscoveredOperation::getConnectionOperationId,
                        Collectors.toList()
                ));
    }

    private Map<Integer, List<Operation>> getOperatioMap(List<Operation> allOperations) {
        return allOperations.stream()
                .collect(Collectors.groupingBy(
                        Operation::getInterfaceId,
                        Collectors.toList()
                ));
    }

    public List<ContainerInterfacesDTO> getContainersFromStructurizr(String cmdb, String branch, Boolean showHidden) {
        Product product = productRepository.findByAliasCaseInsensitive(cmdb);
        if (product == null) {
            throw new EntityNotFoundException("Продукт с данным cmdb: " + cmdb + " не найден.");
        }
        Optional<ProductBranch> productBranch = findBranch(product.getAlias(), branch);
        if (productBranch.isEmpty()) {
            return Collections.emptyList();
        }
        Integer productBranchId = productBranch.get().getId();
        List<ContainerProduct> containerProducts = showHidden ? containerRepository.findAllByProductBranchId(productBranchId)
                : containerRepository.findAllByProductBranchIdAndDeletedDateIsNull(productBranchId);
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
                        getOwner(product, userProfileShortDTOMap)))
                .sorted(Comparator.comparing(ProductInfoShortDTO::getAlias))
                .collect(Collectors.toList());
    }

    private String getOwner(Product product, Map<Integer, UserProfileShortDTO> userProfileShortDTOMap) {
        if (product.getOwnerID() != null) {
            UserProfileShortDTO profile = userProfileShortDTOMap.get(product.getOwnerID());
            if (profile != null) {
                return profile.getFullName();
            }
        }
        return null;
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
            throw new EntityNotFoundException("Продукт с данным cmdb: " + cmdb + " не найден.");
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

    public List<Integer> getTCIdsByProductId(Integer id, String branch) {
        Product product = productRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("not found"));
        Optional<ProductBranch> productBranch = findBranch(product.getAlias(), branch);
        if (productBranch.isEmpty()) {
            return new ArrayList<>();
        }
        List<ContainerProduct> containerProducts = containerRepository.findAllByProductBranchIdAndDeletedDateIsNull(
                productBranch.get().getId());
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
        long startedAt = System.currentTimeMillis();
        log.info("E2E по cmdb: начало, cmdb={}", cmdb);
        Product product = getProductByCode(cmdb);
        if (product == null) {
            log.warn("E2E по cmdb: продукт не найден, cmdb={}", cmdb);
            return List.of();
        }
        String alias = product.getAlias();
        List<E2eMethodUsagesDTO> methodUsages = dashboardClient.getE2eMethodUsages(alias);
        if (methodUsages.isEmpty()) {
            log.info("E2E по cmdb: пустой ответ Dashboard, cmdb={}, alias={}", cmdb, alias);
            return List.of();
        }
        List<ResultDTO> result = methodUsages.stream()
                .filter(item -> item.getMethod() != null && item.getUsages() != null)
                .flatMap(item -> item.getUsages().stream()
                        .map(usage -> ResultDTO.builder()
                                .e2e(usage.getE2e_name())
                                .operation(item.getMethod().getName())
                                .client(extractClientCodes(usage))
                                .build()))
                .toList();
        List<ResultDTO> aggregated = aggregateE2eResults(result);
        log.info("E2E по cmdb: завершено, cmdb={}, alias={}, методовВDashboard={}", cmdb, alias, methodUsages.size());
        return aggregated;
    }

    private static List<String> extractClientCodes(E2eMethodUsageDetailDTO usage) {
        if (usage.getClients() == null) {
            return List.of();
        }
        return usage.getClients().stream()
                .map(E2eMethodUsageClientDTO::getCode)
                .filter(Objects::nonNull)
                .toList();
    }

    private static List<ResultDTO> aggregateE2eResults(List<ResultDTO> result) {
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

    public ProductAvailableDTO getAvailableProductsByCode(String id) {
        Optional<ProductAvailability> productAvailability =
                productAvailabilityRepository.findFirstByProductIdOrderByCreatedDateDesc(Integer.parseInt(id));
        return ProductAvailableDTO.builder()
                .availability(productAvailability.isPresent() ? productAvailability.get().getAvailability() : true)
                .build();
    }

    public IsUniqAliasDTO getFreeAlias(String alias) {
        IsUniqAliasDTO result = new IsUniqAliasDTO();
        if (!alias.matches("^[a-zA-Z0-9]+$")) {
            throw new IllegalArgumentException("Параметр может содержать только латинские буквы и цифры");
        }
        Product product = productRepository.findByAliasCaseInsensitive(alias);
        result.setIsUniqAlias(product == null ? true : false);
        return result;
    }

    public List<GetUserProfileDTO> getEmployeeByAlias(String alias) {
        List<GetUserProfileDTO> result = new ArrayList<>();
        Product product = getProductByCode(alias);
        List<UserProduct> userProducts = userProductRepository.findAllByProductId(product.getId());
        if (!userProducts.isEmpty()) {
            List<UserProfileShortDTO> userProfileShortDTO =
                    userClient.findUserProfilesByIdIn(userProducts.stream().map(UserProduct::getUserId)
                            .filter(Objects::nonNull).toList());
            for (UserProfileShortDTO user : userProfileShortDTO) {
                result.add(GetUserProfileDTO.builder()
                        .login(user.getLogin())
                        .email(user.getEmail())
                        .fullName(user.getFullName())
                        .id(user.getId())
                        .build());
            }
        }
        return result;
    }

    public List<TcDTO> getTcByContainerProduct(String alias, List<String> containers, String branch) {
        List<TcDTO> result = new ArrayList<>();
        Product product = validateAliasContainers(alias, containers);
        Optional<ProductBranch> productBranch = findBranch(product.getAlias(), branch);
        if (productBranch.isEmpty()) {
            return result;
        }
        List<String> lowerCaseContainers = containers.stream()
                .map(String::toLowerCase)
                .toList();
        List<ContainerProduct> containerProducts =
                containerRepository.findAllByProductBranchIdAndNameInIgnoreCaseAndDeletedDateIsNull(
                        productBranch.get().getId(), lowerCaseContainers);
        if (containerProducts.isEmpty()) {
            return new ArrayList<>();
        }
        List<Integer> containerIds = containerProducts.stream()
                .map(ContainerProduct::getId)
                .toList();
        List<Interface> interfaces = interfaceRepository.findByContainerIdInWithOperationsNotDeleted(containerIds);
        if (interfaces != null && !interfaces.isEmpty()) {
            List<Operation> allOperations = interfaces.stream()
                    .map(Interface::getOperations)
                    .filter(Objects::nonNull)
                    .flatMap(List::stream)
                    .toList();
            List<Integer> tcIds = allOperations.stream()
                    .map(Operation::getTcId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();
            result = capabilityClient.getTcs(tcIds);
        }
        return result;
    }

    private Product validateAliasContainers(String alias, List<String> containers) {
        if (alias == null || alias.isEmpty()) {
            throw new IllegalArgumentException("Праметр alias не может быть пустым.");
        }
        if (containers == null || containers.isEmpty()) {
            throw new IllegalArgumentException("Список containers не может быть null и пустым.");
        }
        Product product = productRepository.findByAliasCaseInsensitive(alias);
        if (product == null) {
            throw new EntityNotFoundException("Продукт не найден.");
        }
        return product;
    }

    public void processOperation(Integer id, String changeType) {
        switch (changeType) {
            case "DELETE" -> operationRepository.markAsDeleted(id);
            case "UPDATE" -> operationRepository.markAsUpdated(id);
        }
    }

    public void deleteProduct(Integer id) {
        Product product = productRepository.findById(id).orElse(null);
        if (product == null) {
            log.error("Продукт с id {} не найден", id);
            throw new EntityNotFoundException("Запись в таблице Product с id= " + id + " не найдена.");
        }
        userProductRepository.deleteByProductId(id);
        log.info("Удалены user_product для продукта id: {}", id);
        List<ProductBranch> branches = productBranchRepository.findAllByAlias(product.getAlias());
        List<Integer> branchIds = branches.stream().map(ProductBranch::getId).toList();
        List<Integer> containerIds = branchIds.isEmpty()
                ? Collections.emptyList() : containerRepository.findIdsByProductBranchIdIn(branchIds);
        if (!containerIds.isEmpty()) {
            log.info("Удалено container_product: {} записей", containerIds.size());
            List<Integer> interfaceIds = interfaceRepository.findIdsByContainerIds(containerIds);
            if (!interfaceIds.isEmpty()) {
                log.info("Удаление discovered_interface для интерфейсов: {} шт", interfaceIds.size());
                List<Integer> discoveredInterfaceIds = discoveredInterfaceRepository.findIdsByInterfaceIds(interfaceIds);
                List<Integer> operationIds = operationRepository.findIdsByInterfaceIds(interfaceIds);
                List<Integer> discoveredOperationIds = new ArrayList<>();
                if (!discoveredInterfaceIds.isEmpty()) {
                    discoveredOperationIds.addAll(discoveredOperationRepository.findIdsByDiscoveredInterfaceIds(discoveredInterfaceIds));
                    discoveredOperationIds.addAll(discoveredOperationRepository.findIdsByOperationIds(operationIds));
                    if (!discoveredOperationIds.isEmpty()) {
                        discoveredParameterRepository.deleteByDiscoveredOperationIdIn(discoveredOperationIds);
                        discoveredOperationRepository.deleteByIdIn(discoveredOperationIds);
                        log.info("Удалено discovered_operation: {} записей", discoveredOperationIds.size());
                    }
                    discoveredInterfaceRepository.deleteByIdIn(discoveredInterfaceIds);
                    log.info("Удалено discovered_interface: {} записей", discoveredInterfaceIds.size());
                }

                if (!operationIds.isEmpty()) {
                    slaRepository.deleteByOperationIdIn(operationIds);
                    parameterRepository.deleteByOperationIdIn(operationIds);
                    operationRepository.deleteByIdIn(operationIds);
                }
                interfaceRepository.deleteByIdIn(interfaceIds);
            }
            containerRepository.deleteByIdIn(containerIds);
        }
        deleteLocalAssessmentsByProductId(id);
        techProductRepository.deleteByProductId(id);
        log.info("Удалено tech_product для продукта id: {}", id);
        if (!branchIds.isEmpty()) {
            productBranchRepository.deleteByAlias(product.getAlias());
            log.info("Удалено product_branch: {} записей для alias={}", branchIds.size(), product.getAlias());
        }
        productRepository.deleteById(id);
        log.info("Продукт id: {} успешно удален", id);
    }

    private void deleteLocalAssessmentsByProductId(Integer productId) {
        List<Integer> localAssessmentIds = localAssessmentRepository.findIdsByProductId(productId);
        if (localAssessmentIds.isEmpty()) {
            log.debug("local_assessment не найдены для продукта id: {}", productId);
            return;
        }
        List<Integer> localAssessmentCheckIds = localAssessmentCheckRepository.findByAssessmentIds(localAssessmentIds);
        List<Integer> localAcObjectIds = localAcObjectRepository.findAllByLocalAssessmentCheckIn(localAssessmentCheckIds);
        if (!localAcObjectIds.isEmpty()) {
            localAcObjectDetailRepository.deleteByLacoIdIn(localAcObjectIds);
            log.info("Удалено local_ac_object_detail: {} записей", localAcObjectIds.size());
        }
        if (!localAssessmentCheckIds.isEmpty()) {
            localAcObjectRepository.deleteByLacIdIn(localAssessmentCheckIds);
            log.info("Удалено local_ac_object: {} записей", localAssessmentCheckIds.size());
            localAssessmentCheckRepository.deleteByLocalAssessmentIdsIn(localAssessmentIds);
            log.info("Удалено local_assessment_check: {} записей", localAssessmentCheckIds.size());
        }
        localAssessmentRepository.deleteByProductId(productId);
    }

}