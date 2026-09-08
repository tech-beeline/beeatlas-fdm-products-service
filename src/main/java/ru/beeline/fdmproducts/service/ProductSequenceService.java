/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.beeline.fdmproducts.domain.Product;
import ru.beeline.fdmproducts.domain.ProductBranch;
import ru.beeline.fdmproducts.domain.ProductSequence;
import ru.beeline.fdmproducts.domain.SeqProductStep;
import ru.beeline.fdmproducts.domain.SequenceStepOperation;
import ru.beeline.fdmproducts.dto.sequence.SequenceCallDTO;
import ru.beeline.fdmproducts.dto.sequence.SequenceInfoDTO;
import ru.beeline.fdmproducts.dto.sequence.SequenceOperationDTO;
import ru.beeline.fdmproducts.dto.sequence.SequenceUpsertRequestDTO;
import ru.beeline.fdmproducts.dto.sequence.SequenceUpsertResponseDTO;
import ru.beeline.fdmproducts.exception.EntityNotFoundException;
import ru.beeline.fdmproducts.repository.OperationRepository;
import ru.beeline.fdmproducts.repository.ProductBranchRepository;
import ru.beeline.fdmproducts.repository.ProductRepository;
import ru.beeline.fdmproducts.repository.ProductSequenceRepository;
import ru.beeline.fdmproducts.repository.SeqProductStepRepository;
import ru.beeline.fdmproducts.repository.SequenceStepOperationRepository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProductSequenceService {

    private static final String DEFAULT_BRANCH = "main";
    private static final String UNKNOWN_GUID_MESSAGE = "sequenceCall ссылается на неизвестный guid операции";

    private final ProductRepository productRepository;
    private final ProductBranchRepository productBranchRepository;
    private final ProductSequenceRepository productSequenceRepository;
    private final SeqProductStepRepository seqProductStepRepository;
    private final SequenceStepOperationRepository sequenceStepOperationRepository;
    private final OperationRepository operationRepository;

    @Transactional
    public SequenceUpsertResponseDTO upsert(String alias, String branch, SequenceUpsertRequestDTO request) {
        String branchName = normalizeBranch(branch);
        log.info("Sequence upsert: обработка, alias={}, branch={}, sequence.uid={}", alias, branchName,
                request != null && request.getSequence() != null ? request.getSequence().getUid() : null);

        validateRequest(request);
        ProductBranch productBranch = resolveBranch(alias, branchName);

        Map<String, SequenceOperationDTO> operationsByGuid = indexOperations(request.getOperations());
        List<SequenceCallDTO> calls = request.getSequenceCall();
        validateCalls(calls, operationsByGuid);

        Map<String, Integer> stepOperationIdByGuid = createStepOperations(
                referencedGuids(calls), operationsByGuid, branchName);

        ProductSequence sequence = upsertCard(productBranch.getId(), request.getSequence());
        rebuildSteps(sequence.getId(), calls, stepOperationIdByGuid);

        log.info("Sequence upsert: завершён, id={}, code={}, productBranchId={}, шагов={}",
                sequence.getId(), sequence.getCode(), sequence.getProductBranchId(), calls.size());
        return SequenceUpsertResponseDTO.builder()
                .id(sequence.getId())
                .code(sequence.getCode())
                .productBranchId(sequence.getProductBranchId())
                .build();
    }

    private String normalizeBranch(String branch) {
        return (branch == null || branch.isBlank()) ? DEFAULT_BRANCH : branch.trim().toLowerCase(Locale.ROOT);
    }

    private ProductBranch resolveBranch(String alias, String branchName) {
        if (alias == null || alias.isBlank()) {
            throw new IllegalArgumentException("Не указан alias продукта");
        }
        Product product = productRepository.findByAliasCaseInsensitive(alias.trim());
        if (product == null) {
            throw new EntityNotFoundException("Продукт с указанным alias не найден");
        }
        return productBranchRepository.findByAliasAndBranchName(product.getAlias(), branchName)
                .orElseThrow(() -> new EntityNotFoundException("Ветка продукта не найдена: " + branchName));
    }

    private void validateRequest(SequenceUpsertRequestDTO request) {
        if (request == null) {
            throw new IllegalArgumentException("Отсутствует тело запроса");
        }
        if (request.getSequence() == null) {
            throw new IllegalArgumentException("Отсутствует обязательное поле sequence");
        }
        requireNonBlank(request.getSequence().getUid(), "sequence.uid");
        requireNonBlank(request.getSequence().getName(), "sequence.name");
        if (request.getSequenceCall() == null) {
            throw new IllegalArgumentException("Отсутствует обязательное поле sequenceCall");
        }
        if (request.getOperations() == null || request.getOperations().isEmpty()) {
            throw new IllegalArgumentException("Отсутствует обязательное непустое поле operations");
        }
    }

    private Map<String, SequenceOperationDTO> indexOperations(List<SequenceOperationDTO> operations) {
        Map<String, SequenceOperationDTO> byGuid = new LinkedHashMap<>();
        for (SequenceOperationDTO operation : operations) {
            if (operation == null) {
                throw new IllegalArgumentException("Пустой элемент в operations");
            }
            requireNonBlank(operation.getGuid(), "operations.guid");
            requireNonBlank(operation.getProductAlias(), "operations.productAlias");
            requireNonBlank(operation.getName(), "operations.name");
            requireNonBlank(operation.getType(), "operations.type");
            String guid = operation.getGuid().trim();
            if (byGuid.put(guid, operation) != null) {
                throw new IllegalArgumentException("Дубликат guid в operations: " + guid);
            }
        }
        return byGuid;
    }

    private void validateCalls(List<SequenceCallDTO> calls, Map<String, SequenceOperationDTO> operationsByGuid) {
        for (SequenceCallDTO call : calls) {
            if (call == null) {
                throw new IllegalArgumentException("Пустой элемент в sequenceCall");
            }
            requireNonBlank(call.getRelatedOperationGuid(), "sequenceCall.relatedOperationGuid");
            if (call.getOrder() == null) {
                throw new IllegalArgumentException("Отсутствует обязательное поле sequenceCall.order");
            }
            if (!operationsByGuid.containsKey(call.getRelatedOperationGuid().trim())) {
                throw new IllegalArgumentException(UNKNOWN_GUID_MESSAGE);
            }
            String operationGuid = trimToNull(call.getOperationGuid());
            if (operationGuid != null && !operationsByGuid.containsKey(operationGuid)) {
                throw new IllegalArgumentException(UNKNOWN_GUID_MESSAGE);
            }
        }
    }

    private Set<String> referencedGuids(List<SequenceCallDTO> calls) {
        Set<String> guids = new LinkedHashSet<>();
        for (SequenceCallDTO call : calls) {
            String operationGuid = trimToNull(call.getOperationGuid());
            if (operationGuid != null) {
                guids.add(operationGuid);
            }
            guids.add(call.getRelatedOperationGuid().trim());
        }
        return guids;
    }

    private Map<String, Integer> createStepOperations(Set<String> guids,
                                                      Map<String, SequenceOperationDTO> operationsByGuid,
                                                      String branchName) {
        Map<String, Integer> stepOperationIdByGuid = new LinkedHashMap<>();
        for (String guid : guids) {
            SequenceOperationDTO dto = operationsByGuid.get(guid);
            SequenceStepOperation stepOperation = sequenceStepOperationRepository.save(SequenceStepOperation.builder()
                    .operationProductAlias(dto.getProductAlias())
                    .operationContainerCode(dto.getContainerCode())
                    .operationInterfaceCode(dto.getInterfaceCode())
                    .operationName(dto.getName())
                    .operationType(dto.getType())
                    .operationId(resolveOperationId(dto, branchName))
                    .build());
            stepOperationIdByGuid.put(guid, stepOperation.getId());
        }
        return stepOperationIdByGuid;
    }

    private Integer resolveOperationId(SequenceOperationDTO dto, String branchName) {
        List<Integer> ids = operationRepository.findIdsForSequenceStep(
                dto.getProductAlias().trim(),
                branchName,
                trimToNull(dto.getContainerCode()),
                trimToNull(dto.getInterfaceCode()),
                dto.getName().trim(),
                dto.getType().trim());
        if (ids.size() == 1) {
            return ids.get(0);
        }
        log.info("Sequence upsert: операция не разрешена (найдено {}), guid={}, productAlias={}, "
                        + "containerCode={}, interfaceCode={}, name={}, type={}, branch={}",
                ids.size(), dto.getGuid(), dto.getProductAlias(), dto.getContainerCode(),
                dto.getInterfaceCode(), dto.getName(), dto.getType(), branchName);
        return null;
    }

    private ProductSequence upsertCard(Integer productBranchId, SequenceInfoDTO info) {
        String code = info.getUid().trim();
        ProductSequence sequence = productSequenceRepository
                .findByProductBranchIdAndCodeIgnoreCase(productBranchId, code)
                .orElseGet(() -> ProductSequence.builder()
                        .productBranchId(productBranchId)
                        .build());
        sequence.setCode(code);
        sequence.setName(info.getName().trim());
        sequence.setDescription(info.getDescription());
        sequence.setTcCode(info.getTcCode());
        return productSequenceRepository.save(sequence);
    }

    private void rebuildSteps(Integer sequenceId, List<SequenceCallDTO> calls,
                              Map<String, Integer> stepOperationIdByGuid) {
        List<SeqProductStep> existingSteps = seqProductStepRepository.findAllBySeqId(sequenceId);
        if (!existingSteps.isEmpty()) {
            Set<Integer> obsoleteStepOperationIds = new LinkedHashSet<>();
            for (SeqProductStep step : existingSteps) {
                if (step.getSeqStepOperationId() != null) {
                    obsoleteStepOperationIds.add(step.getSeqStepOperationId());
                }
                obsoleteStepOperationIds.add(step.getSeqStepRelatedOperationId());
            }
            seqProductStepRepository.deleteAllBySeqId(sequenceId);
            sequenceStepOperationRepository.deleteOrphansByIdIn(obsoleteStepOperationIds);
            log.info("Sequence upsert: удалён прежний состав, seqId={}, шагов={}, снимков методов={}",
                    sequenceId, existingSteps.size(), obsoleteStepOperationIds.size());
        }

        List<SeqProductStep> steps = new ArrayList<>();
        for (SequenceCallDTO call : calls) {
            String operationGuid = trimToNull(call.getOperationGuid());
            steps.add(SeqProductStep.builder()
                    .seqId(sequenceId)
                    .order(call.getOrder())
                    .rawDescription(call.getDescription())
                    .seqStepOperationId(operationGuid == null ? null : stepOperationIdByGuid.get(operationGuid))
                    .seqStepRelatedOperationId(stepOperationIdByGuid.get(call.getRelatedOperationGuid().trim()))
                    .build());
        }
        seqProductStepRepository.saveAll(steps);
    }

    private void requireNonBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Отсутствует обязательное поле " + field);
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
