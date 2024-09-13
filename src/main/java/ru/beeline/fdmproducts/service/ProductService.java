package ru.beeline.fdmproducts.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.beeline.fdmlib.dto.product.ProductPutDto;
import ru.beeline.fdmproducts.domain.Product;
import ru.beeline.fdmproducts.domain.UserProduct;
import ru.beeline.fdmproducts.exception.EntityNotFoundException;
import ru.beeline.fdmproducts.exception.ValidationException;
import ru.beeline.fdmproducts.repository.ProductRepository;
import ru.beeline.fdmproducts.repository.UserProductRepository;

import java.util.List;
import java.util.stream.Collectors;

@Transactional
@Service
@Slf4j
public class ProductService {
    private final UserProductRepository userProductRepository;
    private final ProductRepository productRepository;

    public ProductService(UserProductRepository userProductRepository, ProductRepository productRepository) {
        this.userProductRepository = userProductRepository;
        this.productRepository = productRepository;
    }

    public List<Product> getProductsByUser(Integer userId) {
        return userProductRepository.findAllByUserId(userId).stream().map(UserProduct::getProduct).collect(Collectors.toList());
    }

    public Product getProductByCode(String code) {
        Product product = productRepository.findByAlias(code);
        if (product == null) {
            throw new EntityNotFoundException((String.format("404 Пользователь c alias '%s' не найден", code)));
        }
        return product;
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
        if (productPutDto.getName() != null && !product.getName().equals(productPutDto.getName())) {
            product.setName(productPutDto.getName());
        }
        if (productPutDto.getDescription() != null && !product.getDescription().equals(productPutDto.getDescription())) {
            product.setDescription(productPutDto.getDescription());
        }
        if (productPutDto.getGitUrl() != null && !product.getGitUrl().equals(productPutDto.getGitUrl())) {
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

    public void validateProductPutDto(ProductPutDto productPutDto) {
        StringBuilder errMsg = new StringBuilder();
        if (productPutDto.getName() == null) {
            errMsg.append("Отсутствует обязательное поле name");
        }
        if (!errMsg.toString().isEmpty()) {
            throw new ValidationException(errMsg.toString());
        }
    }

    public void validatePatchProductPutDto(ProductPutDto productPutDto) {
        StringBuilder errMsg = new StringBuilder();
        if (productPutDto.getStructurizrWorkspaceName() == null) {
            errMsg.append("Отсутствует обязательное поле structurizrWorkspaceName");
        }
        if (productPutDto.getStructurizrApiKey() == null) {
            errMsg.append("Отсутствует обязательное поле structurizrApiKey");
        }
        if (productPutDto.getStructurizrApiSecret() == null) {
            errMsg.append("Отсутствует обязательное поле structurizrApiSecret");
        }
        if (productPutDto.getStructurizrApiUrl() == null) {
            errMsg.append("Отсутствует обязательное поле structurizrApiUrl");
        }
        if (!errMsg.toString().isEmpty()) {
            throw new ValidationException(errMsg.toString());
        }
    }
}