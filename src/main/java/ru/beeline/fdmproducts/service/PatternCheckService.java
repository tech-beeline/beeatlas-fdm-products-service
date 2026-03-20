/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.beeline.fdmproducts.domain.PatternCheck;
import ru.beeline.fdmproducts.domain.Product;
import ru.beeline.fdmproducts.exception.EntityNotFoundException;
import ru.beeline.fdmproducts.repository.PatternCheckRepository;
import ru.beeline.fdmproducts.repository.ProductRepository;

import java.time.LocalDateTime;
import java.util.List;

@Transactional
@Service
@Slf4j
public class PatternCheckService {

    @Autowired
    private PatternCheckRepository patternCheckRepository;
    @Autowired
    private ProductRepository productRepository;

    public PatternCheck create(Integer productId, String patternCode, Boolean isCheck, String resultDetails) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Продукт не найден"));

        PatternCheck patternCheck = PatternCheck.builder()
                .product(product)
                .patternCode(patternCode)
                .isCheck(isCheck != null ? isCheck : false)
                .resultDetails(resultDetails)
                .createDate(LocalDateTime.now())
                .build();
        return patternCheckRepository.save(patternCheck);
    }

    public List<PatternCheck> findByProductId(Integer productId) {
        return patternCheckRepository.findByProductId(productId);
    }

    public List<PatternCheck> findByPatternCode(String patternCode) {
        return patternCheckRepository.findByPatternCode(patternCode);
    }
}
