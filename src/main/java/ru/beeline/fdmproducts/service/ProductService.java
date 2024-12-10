package ru.beeline.fdmproducts.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.beeline.fdmlib.dto.product.ProductPutDto;
//import ru.beeline.fdmproducts.client.CapabilityClient;
import ru.beeline.fdmproducts.domain.*;
import ru.beeline.fdmproducts.dto.*;
import ru.beeline.fdmproducts.exception.EntityNotFoundException;
import ru.beeline.fdmproducts.exception.ValidationException;
import ru.beeline.fdmproducts.repository.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Transactional
@Service
@Slf4j
public class ProductService {

//    @Autowired
//    CapabilityClient capabilityClient;
    private final UserProductRepository userProductRepository;
    private final ServiceEntityRepository serviceEntityRepository;
    private final ProductRepository productRepository;
    private final ContainerRepository containerRepository;
    private final InterfaceRepository interfaceRepository;
    private final OperationRepository operationRepository;
    private final SlaRepository slaRepository;


    public ProductService(UserProductRepository userProductRepository, ProductRepository productRepository,
                          ServiceEntityRepository serviceEntityRepository, ContainerRepository containerRepository,
                          InterfaceRepository interfaceRepository, OperationRepository operationRepository,
                          SlaRepository slaRepository) {
        this.userProductRepository = userProductRepository;
        this.productRepository = productRepository;
        this.serviceEntityRepository = serviceEntityRepository;
        this.containerRepository = containerRepository;
        this.interfaceRepository = interfaceRepository;
        this.operationRepository = operationRepository;
        this.slaRepository = slaRepository;
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

    public void createOrUpdate1(List<ContainerDTO> containerDTOList, String code) {
        Product product = getProductByCode(code);
        for (ContainerDTO containerDTO : containerDTOList) {
            Optional<ContainerProduct> optionalContainer = containerRepository.findByCode(containerDTO.getCode());
            Integer containerId = null;
            if (optionalContainer.isEmpty()) {
                ContainerProduct containerProduct = ContainerProduct.builder()
                        .name(containerDTO.getName())
                        .version(containerDTO.getVersion())
                        .createdDate(new Date())
                        .build();
                containerRepository.save(containerProduct);    // запомнить id контейнера - родит
                containerId = containerProduct.getId();
            } else {
                ContainerProduct container = optionalContainer.get();
                if (!container.getName().equals(containerDTO.getName()) ||
                        !container.getVersion().equals(containerDTO.getVersion())) {
                    container.setName(containerDTO.getName());
                    container.setVersion(containerDTO.getVersion());
                    container.setUpdatedDate(new Date());
                    containerRepository.save(container);
                    containerId = container.getId();
                }

            }
            // для каждого интерфейса контейнера:
            for (InterfaceDTO interfaceDTO : containerDTO.getInterfaces()) {
                Optional<Interface> optionalInterface = interfaceRepository.findByCode(interfaceDTO.getCode());
                Integer interfaceId = null;
                if (optionalInterface.isEmpty()) {
//                    SearchCapabilityDTO searchCapabilityDTO = capabilityClient.getCapabilities(interfaceDTO.getCode());
                    Interface newInterface = Interface.builder()
                            .name(interfaceDTO.getName())
                            .code(interfaceDTO.getCode())
                            .version(interfaceDTO.getVersion())
                            .specLink(interfaceDTO.getSpecLink())
                            .protocol(interfaceDTO.getProtocol())
//                            .tcId(searchCapabilityDTO.getId())
                            .containerId(containerId)
                            .createdDate(new Date())
                            .build();
                    interfaceRepository.save(newInterface);
                    interfaceId = newInterface.getId();
                } else {
                    Interface getInterface = optionalInterface.get();
                    getInterface.setName(interfaceDTO.getName());
                    getInterface.setCode(interfaceDTO.getCode());
                    getInterface.setVersion(interfaceDTO.getVersion());
                    getInterface.setSpecLink(interfaceDTO.getSpecLink());
                    getInterface.setProtocol(interfaceDTO.getProtocol());
                    getInterface.setUpdatedDate(new Date());
                    getInterface.setDeletedDate(null);
                    interfaceId = getInterface.getId();
                    interfaceRepository.save(getInterface);
                }
                for (MethodDTO methodDTO : interfaceDTO.getMethods()) {
                    Optional<Operation> optionalOperation = operationRepository.findByName(methodDTO.getName());
                    Integer operationId=null;
                    if (optionalOperation.isEmpty()){
                        Operation operation = Operation.builder()
                                .name(methodDTO.getName())
                                .description(methodDTO.getDescription())
                                .returnType(methodDTO.getReturnType())
                                .interfaceId(interfaceId)
                                .createdDate(new Date())
                                .build();
                        operationRepository.save(operation);
                        operationId=operation.getId();
                    }else{
                        Operation operation = optionalOperation.get();
                        operation.setName(methodDTO.getName());
                        operation.setDescription(methodDTO.getDescription());
                        operation.setReturnType(methodDTO.getReturnType());
                        operation.setUpdatedDate(new Date());
                        operation.setDeleteDate(null);
                        operationRepository.save(operation);
                        operationId = operation.getId();
                    }

                    Optional<Sla> optionalSla = slaRepository.findByOperationId(operationId);
                    if(optionalSla.isEmpty()){
                        Sla sla = Sla.builder()
                                .operationId(operationId)
                                .rps(methodDTO.getSla().getRps())
                                .latency(methodDTO.getSla().getLatency())
                                .errorRate(methodDTO.getSla().getErrorRate())
                                .build();
                        slaRepository.save(sla);
                    }else{
                        Sla sla= optionalSla.get();
                        sla.setRps(methodDTO.getSla().getRps());
                        sla.setLatency(methodDTO.getSla().getLatency());
                        sla.setErrorRate(methodDTO.getSla().getErrorRate());
                        slaRepository.save(sla);
                    }
                    List<ParameterDTO> parameters= methodDTO.getParameters();
                    for(ParameterDTO parameterDTO: parameters){



                    }

                }
            }

        }
    }

}
