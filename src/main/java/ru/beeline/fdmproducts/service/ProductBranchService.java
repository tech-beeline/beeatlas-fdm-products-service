/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.beeline.fdmproducts.domain.ProductBranch;
import ru.beeline.fdmproducts.repository.ProductBranchRepository;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Разбор query-параметра {@code branch} и поиск ветки продукта — одинаково для всех методов,
 * где ветка встречается (SFDM-4043).
 * <p>
 * Раньше эти правила были продублированы в ProductService и ProductSequenceService, а часть кода
 * искала ветку по литералу «main» напрямую через репозиторий. Расхождения такого рода и приводят к
 * тому, что один метод ветку находит, а другой на тех же данных — нет.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ProductBranchService {

    /** Ветка, которая подразумевается, когда параметр не передан. */
    public static final String DEFAULT_BRANCH = "main";

    private final ProductBranchRepository productBranchRepository;

    /**
     * Имя ветки для запроса. Отсутствие параметра и пустое значение — разные вещи: {@code null}
     * означает «клиент про ветки не знает», это main, а {@code ?branch=} — заполненный параметр без
     * значения, то есть ошибка в запросе, а не согласие на main.
     * <p>
     * Имя приводится к нижнему регистру: поиск по правилам метода регистронезависимый, а
     * уникальный индекс в БД — нет, поэтому запись в одном регистре не даёт развести main и Main.
     */
    public String resolveBranchName(String branch) {
        if (branch == null) {
            return DEFAULT_BRANCH;
        }
        if (branch.isBlank()) {
            throw new IllegalArgumentException("Параметр branch не может быть пустым");
        }
        return branch.trim().toLowerCase(Locale.ROOT);
    }

    /** Ветка продукта по имени из запроса; пусто — ветки нет (для GET это 200 с пустым результатом). */
    public Optional<ProductBranch> find(String alias, String branch) {
        return findByName(alias, resolveBranchName(branch));
    }

    /**
     * Идентификатор ветки, создавая её при необходимости, — для PUT-методов, которые заводят ветку
     * на лету. Сначала регистронезависимый поиск: без него запрос с Main завёл бы вторую ветку
     * рядом с существующей main, так как ON CONFLICT опирается на регистрозависимый индекс.
     */
    public Integer getOrCreateId(String alias, String branch) {
        String branchName = resolveBranchName(branch);
        return findByName(alias, branchName)
                .map(ProductBranch::getId)
                .orElseGet(() -> {
                    Integer id = productBranchRepository.upsert(alias, branchName);
                    log.info("Создана ветка продукта: alias={}, branch={}, id={}", alias, branchName, id);
                    return id;
                });
    }

    private Optional<ProductBranch> findByName(String alias, String branchName) {
        List<ProductBranch> branches =
                productBranchRepository.findAllByAliasIgnoreCaseAndBranchNameIgnoreCaseOrderByIdAsc(alias, branchName);
        if (branches.size() > 1) {
            // Ветки, различающиеся только регистром, могли появиться мимо API. Берём совпадающую с
            // нормализованным именем, иначе самую раннюю, — чтобы разные методы выбирали одну и ту же.
            log.warn("Ветки различаются только регистром: alias={}, branch={}, найдено={}",
                    alias, branchName, branches.size());
            return branches.stream()
                    .filter(item -> branchName.equals(item.getBranchName()))
                    .findFirst()
                    .or(() -> Optional.of(branches.get(0)));
        }
        return branches.stream().findFirst();
    }
}
