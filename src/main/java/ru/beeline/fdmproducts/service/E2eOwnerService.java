/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.beeline.fdmproducts.domain.E2e;
import ru.beeline.fdmproducts.domain.Product;
import ru.beeline.fdmproducts.exception.EntityNotFoundException;
import ru.beeline.fdmproducts.repository.ProductRepository;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Продукт-владелец карточки e2e (SFDM-4244): product.e2e.product_id, в теле и ответе — productAlias.
 * Владелец не создаётся: неизвестный alias — 404. Владельца может не быть.
 */
@Service
@RequiredArgsConstructor
public class E2eOwnerService {

    public static final String OWNER_NOT_FOUND = "Продукт с указанным alias не найден";
    public static final String OWNER_BLANK = "productAlias не может быть пустым";

    private final ProductRepository productRepository;

    /** Что сделать с владельцем по полю productAlias тела запроса. */
    public sealed interface OwnerChange permits Keep, Clear, Assign {
    }

    /** Поле не передано или JSON null: при создании владельца нет, при обновлении он не меняется. */
    public record Keep() implements OwnerChange {
    }

    /** Пустая строка: владелец снимается. */
    public record Clear() implements OwnerChange {
    }

    public record Assign(Product product) implements OwnerChange {
    }

    /**
     * Разбирает productAlias до любой записи: пустая строка снимает владельца, строка из пробелов — 400,
     * alias ищется без учёта регистра после trim, не найден — 404, продукт не создаётся.
     */
    public OwnerChange resolve(String productAlias) {
        if (productAlias == null) {
            return new Keep();
        }
        if (productAlias.isEmpty()) {
            return new Clear();
        }
        String trimmed = productAlias.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(OWNER_BLANK);
        }
        return productRepository.findFirstByAliasIgnoreCase(trimmed)
                .<OwnerChange>map(Assign::new)
                .orElseThrow(() -> new EntityNotFoundException(OWNER_NOT_FOUND));
    }

    /** Применяет изменение владельца к карточке; Keep ничего не меняет. */
    public void apply(E2e e2e, OwnerChange change) {
        if (change instanceof Clear) {
            e2e.setProductId(null);
        } else if (change instanceof Assign assign) {
            e2e.setProductId(assign.product().getId());
        }
    }

    /** alias владельцев карточек одним запросом: productId → product.alias как записан в таблице. */
    public Map<Integer, String> aliasesOf(Collection<E2e> e2es) {
        List<Integer> productIds = e2es.stream()
                .map(E2e::getProductId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (productIds.isEmpty()) {
            return Map.of();
        }
        Map<Integer, String> aliases = new HashMap<>();
        for (Product product : productRepository.findAllById(productIds)) {
            aliases.put(product.getId(), product.getAlias());
        }
        return aliases;
    }

    public String aliasOf(E2e e2e) {
        return e2e.getProductId() == null ? null : aliasesOf(List.of(e2e)).get(e2e.getProductId());
    }
}
