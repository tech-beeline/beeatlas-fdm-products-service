/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.beeline.fdmproducts.client.ProjectClient;
import ru.beeline.fdmproducts.domain.DiscoveredInterface;
import ru.beeline.fdmproducts.domain.DiscoveredOperation;
import ru.beeline.fdmproducts.domain.Product;
import ru.beeline.fdmproducts.dto.discovered.UseCaseDiscoveredInterfaceDTO;
import ru.beeline.fdmproducts.dto.discovered.UseCaseDiscoveredInterfaceResultDTO;
import ru.beeline.fdmproducts.dto.discovered.UseCaseDiscoveredOperationDTO;
import ru.beeline.fdmproducts.dto.discovered.UseCaseDiscoveredOperationResultDTO;
import ru.beeline.fdmproducts.dto.project.OperationUseCaseDTO;
import ru.beeline.fdmproducts.dto.project.OperationUseCasesResponseDTO;
import ru.beeline.fdmproducts.repository.DiscoveredInterfaceRepository;
import ru.beeline.fdmproducts.repository.DiscoveredOperationRepository;
import ru.beeline.fdmproducts.repository.ProductRepository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class UseCaseDiscoveredInterfaceService {

    public static final String SOURCE = "UseCase";
    private static final String MISSING_USE_CASE_ID = "Не передан обязательный параметр use-case-id";
    private static final char KEY_SEPARATOR = 0;
    private final ProductRepository productRepository;
    private final DiscoveredInterfaceRepository discoveredInterfaceRepository;
    private final DiscoveredOperationRepository discoveredOperationRepository;
    private final ProjectClient projectClient;

    private record RequestedInterface(UseCaseDiscoveredInterfaceDTO dto, String interfaceCode, String productAlias,
                                      Product product, String productPlanned) {
    }

    private record ResolvedInterface(DiscoveredInterface entity, boolean created) {
    }

    @Transactional
    public List<UseCaseDiscoveredInterfaceResultDTO> syncUseCaseOperations(String useCaseIdParam,
                                                                           List<UseCaseDiscoveredInterfaceDTO> interfaces) {
        Integer useCaseId = parseUseCaseId(useCaseIdParam);
        if (interfaces == null) {
            throw new IllegalArgumentException("Отсутствует тело запроса");
        }
        List<RequestedInterface> requested = resolveRequest(interfaces);
        log.info("Публикация состава use case: useCaseId={}, интерфейсов в теле={}", useCaseId, requested.size());
        LocalDateTime now = LocalDateTime.now();

        Map<String, List<DiscoveredInterface>> storedByCode = preloadInterfaces(requested);
        Map<Integer, List<DiscoveredOperation>> operationsByInterfaceId = preloadOperations(storedByCode);

        List<DiscoveredOperation> deletionCandidates = new ArrayList<>();
        List<UseCaseDiscoveredInterfaceResultDTO> result = new ArrayList<>(requested.size());
        for (RequestedInterface item : requested) {
            ResolvedInterface resolved = findOrCreateInterface(item, now, storedByCode);
            result.add(UseCaseDiscoveredInterfaceResultDTO.builder()
                    .interfaceCode(item.dto().getInterfaceCode())
                    .product(item.productAlias())
                    .productPlanned(item.productPlanned())
                    .operations(syncOperations(resolved, item, now, deletionCandidates, operationsByInterfaceId))
                    .build());
        }

        softDeleteCandidates(deletionCandidates, useCaseId, now);
        log.info("Публикация состава use case завершена: useCaseId={}, интерфейсов={}, методов={}, кандидатов={}",
                useCaseId, result.size(), result.stream().mapToInt(item -> item.getOperations().size()).sum(),
                deletionCandidates.size());
        return result;
    }

    private List<RequestedInterface> resolveRequest(List<UseCaseDiscoveredInterfaceDTO> interfaces) {
        Map<String, Product> productByAlias = resolveProducts(interfaces);
        Set<String> seenInterfaces = new HashSet<>();
        List<RequestedInterface> requested = new ArrayList<>(interfaces.size());
        for (UseCaseDiscoveredInterfaceDTO dto : interfaces) {
            if (dto == null) {
                throw new IllegalArgumentException("Пустой элемент в теле запроса");
            }
            String interfaceCode = requireNonBlank(dto.getInterfaceCode(), "interfaceCode");
            String productAlias = trimToNull(dto.getProduct());
            String productPlanned = trimToNull(dto.getProductPlanned());
            validateProductProductPlanned(dto);
            if (dto.getOperations() == null) {
                throw new IllegalArgumentException(
                        "Отсутствует обязательное поле operations, interfaceCode=" + interfaceCode);
            }
            validateOperations(dto.getOperations(), interfaceCode);

            String identityPart = productAlias != null ? lower(productAlias) : lower(productPlanned);
            String key = lower(interfaceCode) + KEY_SEPARATOR + identityPart;
            if (!seenInterfaces.add(key)) {
                throw new IllegalArgumentException("Интерфейс встречается в теле запроса дважды: interfaceCode="
                        + interfaceCode + ", product=" + productAlias + ", productPlanned=" + productPlanned);
            }
            Product product = null;
            if (productAlias != null) {
                product = productByAlias.get(lower(productAlias));
                if (product == null) {
                    throw new IllegalArgumentException("Продукт не найден: product=" + productAlias);
                }
            }
            requested.add(new RequestedInterface(dto, interfaceCode, productAlias, product, productPlanned));
        }
        return requested;
    }

    private void validateProductProductPlanned(UseCaseDiscoveredInterfaceDTO dto) {
        boolean productFilled =
                dto.getProduct() != null && !dto.getProduct().isBlank();
        boolean productPlannedFilled =
                dto.getProductPlanned() != null && !dto.getProductPlanned().isBlank();
        boolean exactlyOneFilled = productFilled ^ productPlannedFilled;
        if (!exactlyOneFilled) {
            throw new IllegalArgumentException(
                    "Для интерфейса обязательно должен быть указан существующий product либо планируемый productPlanned"
            );
        }
    }

    private Map<String, Product> resolveProducts(List<UseCaseDiscoveredInterfaceDTO> interfaces) {
        Set<String> aliases = new LinkedHashSet<>();
        for (UseCaseDiscoveredInterfaceDTO dto : interfaces) {
            if (dto == null || dto.getProduct() == null || dto.getProduct().isBlank()) {
                continue;
            }
            aliases.add(lower(dto.getProduct()));
        }
        Map<String, Product> productByAlias = new HashMap<>();
        if (aliases.isEmpty()) {
            return productByAlias;
        }
        for (Product product : productRepository.findByAliasInIgnoreCase(List.copyOf(aliases))) {
            if (product.getAlias() != null) {
                productByAlias.put(lower(product.getAlias()), product);
            }
        }
        return productByAlias;
    }

    private Map<String, List<DiscoveredInterface>> preloadInterfaces(List<RequestedInterface> requested) {
        if (requested.isEmpty()) {
            return Map.of();
        }
        Set<String> codes = requested.stream()
                .map(item -> lower(item.interfaceCode()))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<String, List<DiscoveredInterface>> byCode = new HashMap<>();
        for (DiscoveredInterface existing : discoveredInterfaceRepository
                .findAllBySourceAndExternalIdInIgnoreCase(SOURCE, codes)) {
            byCode.computeIfAbsent(lower(existing.getExternalId()), key -> new ArrayList<>()).add(existing);
        }
        return byCode;
    }

    private Map<Integer, List<DiscoveredOperation>> preloadOperations(
            Map<String, List<DiscoveredInterface>> storedByCode) {
        List<Integer> interfaceIds = storedByCode.values().stream()
                .flatMap(List::stream)
                .map(DiscoveredInterface::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (interfaceIds.isEmpty()) {
            return new HashMap<>();
        }
        Map<Integer, List<DiscoveredOperation>> byInterfaceId = new HashMap<>();
        for (DiscoveredOperation operation : discoveredOperationRepository.findAllByInterfaceIdIn(interfaceIds)) {
            Integer interfaceId = operation.getInterfaceId() != null
                    ? operation.getInterfaceId()
                    : (operation.getDiscoveredInterface() == null ? null : operation.getDiscoveredInterface().getId());
            if (interfaceId == null) {
                continue;
            }
            byInterfaceId.computeIfAbsent(interfaceId, key -> new ArrayList<>()).add(operation);
        }
        return byInterfaceId;
    }

    private void validateOperations(List<UseCaseDiscoveredOperationDTO> operations, String interfaceCode) {
        Set<String> seenKeys = new HashSet<>();
        for (UseCaseDiscoveredOperationDTO dto : operations) {
            if (dto == null) {
                throw new IllegalArgumentException("Пустой элемент в operations, interfaceCode=" + interfaceCode);
            }
            String name = requireNonBlank(dto.getName(), "operations.name");
            String type = requireNonBlank(dto.getType(), "operations.type");
            if (!seenKeys.add(operationKey(name, type))) {
                throw new IllegalArgumentException("Операция встречается в operations дважды: name=" + name
                        + ", type=" + type + ", interfaceCode=" + interfaceCode);
            }
        }
    }

    private ResolvedInterface findOrCreateInterface(RequestedInterface item, LocalDateTime now,
                                                    Map<String, List<DiscoveredInterface>> storedByCode) {
        Integer productId = item.product() == null ? null : item.product().getId();
        List<DiscoveredInterface> matches = storedByCode
                .getOrDefault(lower(item.interfaceCode()), List.of())
                .stream()
                .filter(existing -> matchesKey(existing, productId, item.productPlanned()))
                .toList();
        List<DiscoveredInterface> live = matches.stream()
                .filter(existing -> existing.getDeletedDate() == null)
                .toList();
        if (live.size() > 1) {
            throw new IllegalArgumentException("Найдено несколько интерфейсов по ключу: interfaceCode="
                    + item.interfaceCode() + ", product=" + item.productAlias());
        }

        DiscoveredInterface existing = live.isEmpty()
                ? (matches.isEmpty() ? null : matches.get(0))
                : live.get(0);
        if (existing == null) {
            DiscoveredInterface created = discoveredInterfaceRepository.save(DiscoveredInterface.builder()
                    .name(item.interfaceCode())
                    .externalId(item.interfaceCode())
                    .product(item.product())
                    .productPlanned(item.productPlanned())
                    .source(SOURCE)
                    .createdDate(now)
                    .build());
            storedByCode.computeIfAbsent(lower(item.interfaceCode()), key -> new ArrayList<>()).add(created);
            log.info("Создан discovered_interface: id={}, code={}, productId={}, productPlanned={}",
                    created.getId(), item.interfaceCode(), productId, item.productPlanned());
            return new ResolvedInterface(created, true);
        }
        existing.setDeletedDate(null);
        existing.setProductPlanned(item.productPlanned());
        existing.setUpdatedDate(now);
        return new ResolvedInterface(discoveredInterfaceRepository.save(existing), false);
    }

    private List<UseCaseDiscoveredOperationResultDTO> syncOperations(ResolvedInterface resolved,
                                                                     RequestedInterface item, LocalDateTime now,
                                                                     List<DiscoveredOperation> deletionCandidates,
                                                                     Map<Integer, List<DiscoveredOperation>> operationsByInterfaceId) {
        List<DiscoveredOperation> stored = resolved.created()
                ? List.of()
                : operationsByInterfaceId.getOrDefault(resolved.entity().getId(), List.of());
        Map<String, DiscoveredOperation> storedByKey = indexOperations(stored);

        Set<String> requestKeys = new HashSet<>();
        List<UseCaseDiscoveredOperationResultDTO> result = new ArrayList<>(item.dto().getOperations().size());
        for (UseCaseDiscoveredOperationDTO dto : item.dto().getOperations()) {
            String name = dto.getName().trim();
            String type = dto.getType().trim();
            boolean isPlanned = Boolean.TRUE.equals(dto.getIsPlanned());
            requestKeys.add(operationKey(name, type));

            DiscoveredOperation operation = storedByKey.get(operationKey(name, type));
            if (operation == null) {
                operation = discoveredOperationRepository.save(DiscoveredOperation.builder()
                        .discoveredInterface(resolved.entity())
                        .name(name)
                        .type(type)
                        .description(dto.getDescriptionOperation())
                        .tcCode(dto.getTcCode())
                        .tcDescription(dto.getDescriptionTc())
                        .isPlanned(isPlanned)
                        .createdDate(now)
                        .build());
            } else {
                refreshOperation(operation, dto, isPlanned, now);
            }
            result.add(UseCaseDiscoveredOperationResultDTO.builder()
                    .discoveredOperationId(operation.getId())
                    .name(dto.getName())
                    .type(dto.getType())
                    .isPlanned(isPlanned)
                    .build());
        }

        for (DiscoveredOperation storedOperation : stored) {
            if (storedOperation.getDeletedDate() != null
                    || requestKeys.contains(operationKey(storedOperation.getName(), storedOperation.getType()))) {
                continue;
            }
            deletionCandidates.add(storedOperation);
        }
        return result;
    }

    private Map<String, DiscoveredOperation> indexOperations(List<DiscoveredOperation> stored) {
        Map<String, DiscoveredOperation> byKey = new HashMap<>();
        for (DiscoveredOperation item : stored) {
            String key = operationKey(item.getName(), item.getType());
            DiscoveredOperation kept = byKey.get(key);
            if (kept == null || (kept.getDeletedDate() != null && item.getDeletedDate() == null)) {
                byKey.put(key, item);
            }
        }
        return byKey;
    }

    private void refreshOperation(DiscoveredOperation operation, UseCaseDiscoveredOperationDTO dto,
                                  boolean isPlanned, LocalDateTime now) {
        boolean changed = false;
        if (!Objects.equals(operation.getDescription(), dto.getDescriptionOperation())) {
            operation.setDescription(dto.getDescriptionOperation());
            changed = true;
        }
        if (!Objects.equals(operation.getTcCode(), dto.getTcCode())) {
            operation.setTcCode(dto.getTcCode());
            changed = true;
        }
        if (!Objects.equals(operation.getTcDescription(), dto.getDescriptionTc())) {
            operation.setTcDescription(dto.getDescriptionTc());
            changed = true;
        }
        if (!Objects.equals(operation.getIsPlanned(), isPlanned)) {
            operation.setIsPlanned(isPlanned);
            changed = true;
        }
        if (operation.getDeletedDate() != null) {
            operation.setDeletedDate(null);
            changed = true;
        }
        if (changed) {
            operation.setUpdatedDate(now);
            discoveredOperationRepository.save(operation);
        }
    }

    private void softDeleteCandidates(List<DiscoveredOperation> candidates, Integer useCaseId, LocalDateTime now) {
        if (candidates.isEmpty()) {
            return;
        }
        List<Integer> ids = candidates.stream().map(DiscoveredOperation::getId).toList();
        List<OperationUseCasesResponseDTO> response = projectClient.findUseCasesByOperations(ids);
        Map<Integer, List<OperationUseCaseDTO>> useCasesByOperation = new HashMap<>();
        for (OperationUseCasesResponseDTO item : response) {
            if (item == null || item.getOperationId() == null) {
                continue;
            }
            useCasesByOperation.put(item.getOperationId(),
                    item.getUseCases() == null ? List.of() : item.getUseCases());
        }

        for (DiscoveredOperation candidate : candidates) {
            List<OperationUseCaseDTO> useCases = useCasesByOperation.get(candidate.getId());
            if (useCases == null || useCases.size() != 1) {
                continue;
            }
            OperationUseCaseDTO onlyUseCase = useCases.get(0);
            if (onlyUseCase == null || !useCaseId.equals(onlyUseCase.getId())) {
                continue;
            }
            candidate.setDeletedDate(now);
            candidate.setUpdatedDate(now);
            discoveredOperationRepository.save(candidate);
            log.info("Метод удалён: id={}, name={}, type={}", candidate.getId(), candidate.getName(),
                    candidate.getType());
        }
    }

    private Integer parseUseCaseId(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(MISSING_USE_CASE_ID);
        }
        try {
            int parsed = Integer.parseInt(value.trim());
            if (parsed <= 0) {
                throw new IllegalArgumentException(MISSING_USE_CASE_ID);
            }
            return parsed;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(MISSING_USE_CASE_ID);
        }
    }

    private boolean matchesKey(DiscoveredInterface existing, Integer productId, String productPlanned) {
        if (productId != null) {
            Integer existingProductId = existing.getProduct() == null ? null : existing.getProduct().getId();
            return productId.equals(existingProductId);
        }
        return equalIgnoreCase(existing.getProductPlanned(), productPlanned);
    }

    private boolean equalIgnoreCase(String first, String second) {
        if (first == null || second == null) {
            return first == second;
        }
        return first.trim().equalsIgnoreCase(second.trim());
    }

    private String operationKey(String name, String type) {
        return lower(name) + KEY_SEPARATOR + lower(type);
    }

    private String lower(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String requireNonBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Отсутствует обязательное поле " + field);
        }
        return value.trim();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
