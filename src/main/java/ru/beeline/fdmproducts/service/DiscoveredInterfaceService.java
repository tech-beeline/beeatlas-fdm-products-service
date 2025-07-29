package ru.beeline.fdmproducts.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.beeline.fdmproducts.domain.DiscoveredInterface;
import ru.beeline.fdmproducts.domain.DiscoveredOperation;
import ru.beeline.fdmproducts.domain.DiscoveredParameter;
import ru.beeline.fdmproducts.dto.DiscoveredInterfaceDTO;
import ru.beeline.fdmproducts.dto.DiscoveredInterfaceOperationDTO;
import ru.beeline.fdmproducts.dto.OperationParameterDTO;
import ru.beeline.fdmproducts.exception.ValidationException;
import ru.beeline.fdmproducts.mapper.DiscoveredInterfaceMapper;
import ru.beeline.fdmproducts.repository.DiscoveredInterfaceRepository;
import ru.beeline.fdmproducts.repository.DiscoveredOperationRepository;
import ru.beeline.fdmproducts.repository.DiscoveredParameterRepository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Transactional
@Service
@Slf4j
public class DiscoveredInterfaceService {

    private final DiscoveredInterfaceMapper discoveredInterfaceMapper;
    private final DiscoveredInterfaceRepository discoveredInterfaceRepository;
    private final DiscoveredOperationRepository discoveredOperationRepository;
    private final DiscoveredParameterRepository discoveredParameterRepository;

    public DiscoveredInterfaceService(DiscoveredInterfaceMapper discoveredInterfaceMapper,
                                      DiscoveredInterfaceRepository discoveredInterfaceRepository,
                                      DiscoveredOperationRepository discoveredOperationRepository,
                                      DiscoveredParameterRepository discoveredParameterRepository) {

        this.discoveredInterfaceMapper = discoveredInterfaceMapper;
        this.discoveredInterfaceRepository = discoveredInterfaceRepository;
        this.discoveredOperationRepository = discoveredOperationRepository;
        this.discoveredParameterRepository = discoveredParameterRepository;
    }

    public void createOrUpdateDiscoveredInterfaces(List<DiscoveredInterfaceDTO> dInterfacesDTOS) {
        dInterfacesDTOS.forEach(this::validateDiscoveredInterfaceDTO);
        List<Integer> externalIds = dInterfacesDTOS.stream().map(DiscoveredInterfaceDTO::getExternalId).toList();
        List<DiscoveredInterface> existingInterfaces = discoveredInterfaceRepository.findByExternalIdIn(externalIds);
        List<Integer> existingExternalIds = existingInterfaces.stream()
                .map(DiscoveredInterface::getExternalId)
                .toList();
        List<DiscoveredInterfaceDTO> missingInterfaces = dInterfacesDTOS.stream()
                .filter(dto -> !existingExternalIds.contains(dto.getExternalId()))
                .toList();
        saveNewInterfaces(missingInterfaces);
        updateInterfaces(existingInterfaces, dInterfacesDTOS);
    }

    private void saveNewInterfaces(List<DiscoveredInterfaceDTO> missingInterface) {
        List<DiscoveredInterface> saveList = missingInterface.stream()
                .map(discoveredInterfaceMapper::convertToEntity)
                .collect(Collectors.toList());
        discoveredInterfaceRepository.saveAll(saveList);
    }

    private void updateInterfaces(List<DiscoveredInterface> existingInterfaces,
                                  List<DiscoveredInterfaceDTO> incomingDtos) {
        Map<Integer, DiscoveredInterfaceDTO> dtoByExternalId = incomingDtos.stream()
                .collect(Collectors.toMap(DiscoveredInterfaceDTO::getExternalId, Function.identity()));
        for (DiscoveredInterface entity : existingInterfaces) {
            DiscoveredInterfaceDTO existingDto = discoveredInterfaceMapper.convertToDiscoveredInterfaceDto(entity);
            DiscoveredInterfaceDTO incomingDto = dtoByExternalId.get(entity.getExternalId());
            if (incomingDto != null && !incomingDto.equals(existingDto)) {
                discoveredInterfaceMapper.updateEntityFromDto(incomingDto, entity);
            }
        }
        discoveredInterfaceRepository.saveAll(existingInterfaces);
    }


    private void validateDiscoveredInterfaceDTO(DiscoveredInterfaceDTO dInterfaces) {
        StringBuilder errMsg = new StringBuilder();
        if (dInterfaces.getName() == null || dInterfaces.getName().trim().isEmpty()) {
            errMsg.append("Отсутствует обязательное поле name; ");
        }
        if (dInterfaces.getVersion() == null || dInterfaces.getVersion().trim().isEmpty()) {
            errMsg.append("Отсутствует обязательное поле Version; ");
        }
        if (dInterfaces.getApiLink() == null || dInterfaces.getApiLink().trim().isEmpty()) {
            errMsg.append("Отсутствует обязательное поле ApiLink; ");
        }
        if (dInterfaces.getDescription() == null || dInterfaces.getDescription().trim().isEmpty()) {
            errMsg.append("Отсутствует обязательное поле Description; ");
        }
        if (dInterfaces.getStatus() == null || dInterfaces.getStatus().trim().isEmpty()) {
            errMsg.append("Отсутствует обязательное поле Status; ");
        }
        if (dInterfaces.getContext() == null || dInterfaces.getContext().trim().isEmpty()) {
            errMsg.append("Отсутствует обязательное поле Context; ");
        }
        if (dInterfaces.getExternalId() == null) {
            errMsg.append("Отсутствует обязательное поле externalId; ");
        }
        if (dInterfaces.getApiId() == null) {
            errMsg.append("Отсутствует обязательное поле apiId; ");
        }
        if (dInterfaces.getProductId() == null) {
            errMsg.append("Отсутствует обязательное поле ProductId; ");
        }
        if (!errMsg.toString().isEmpty()) {
            throw new ValidationException(errMsg.toString().trim());
        }
    }

    public void createOrUpdateOperations(Integer interfaceId, List<DiscoveredInterfaceOperationDTO> operations) {
        LocalDateTime now = LocalDateTime.now();
        for (DiscoveredInterfaceOperationDTO operationDTO : operations) {
            Optional<DiscoveredOperation> existingOpOpt = discoveredOperationRepository.findByDiscoveredInterfaceIdAndNameAndTypeAndDeletedDateIsNull(
                    interfaceId,
                    operationDTO.getName(),
                    operationDTO.getType());

            DiscoveredOperation operation;

            operation = updateOperation(operationDTO, existingOpOpt, now);

            Map<String, DiscoveredParameter> existingParamsMap = operation.getParameters()
                    .stream()
                    .collect(Collectors.toMap(p -> p.getParameterName() + "::" + p.getParameterType(),
                                              Function.identity()));

            Set<String> inputParamsKeys = new HashSet<>();
            if (operationDTO.getParameters() != null) {
                for (OperationParameterDTO paramDTO : operationDTO.getParameters()) {
                    String key = paramDTO.getParameterName() + "::" + paramDTO.getParameterType();
                    inputParamsKeys.add(key);

                    if (!existingParamsMap.containsKey(key)) {
                        DiscoveredParameter param = DiscoveredParameter.builder()
                                .discoveredOperation(operation)
                                .parameterName(paramDTO.getParameterName())
                                .parameterType(paramDTO.getParameterType())
                                .createdDate(now)
                                .build();
                        discoveredParameterRepository.save(param);
                    }
                }
            }

            for (DiscoveredParameter existingParam : operation.getParameters()) {
                String key = existingParam.getParameterName() + "::" + existingParam.getParameterType();
                if (!inputParamsKeys.contains(key)) {
                    existingParam.setDeletedDate(now);
                    discoveredParameterRepository.saveAll(operation.getParameters());
                }
            }
        }
    }

    private DiscoveredOperation updateOperation(DiscoveredInterfaceOperationDTO operationDTO,
                                                Optional<DiscoveredOperation> existingOpOpt,
                                                LocalDateTime now) {
        if (existingOpOpt.isPresent()) {
            DiscoveredOperation operation;
            operation = existingOpOpt.get();

            boolean needUpdate = false;

            if (!equalsNullable(operation.getContext(), operationDTO.getContext())) {
                operation.setContext(operationDTO.getContext());
                needUpdate = true;
            }
            if (!equalsNullable(operation.getDescription(), operationDTO.getDescription())) {
                operation.setDescription(operationDTO.getDescription());
                needUpdate = true;
            }
            if (!equalsNullable(operation.getReturnType(), operationDTO.getReturnType())) {
                operation.setReturnType(operationDTO.getReturnType());
                needUpdate = true;
            }

            if (needUpdate) {
                operation.setUpdatedDate(now);
                discoveredOperationRepository.save(operation);
            }
            return operation;
        } else {
            return discoveredOperationRepository.save(DiscoveredOperation.builder()
                                                              .name(operationDTO.getName())
                                                              .description(operationDTO.getDescription())
                                                              .type(operationDTO.getType())
                                                              .createdDate(LocalDateTime.now())
                                                              .parameters(new ArrayList<>())
                                                              .build());
        }
    }

    private boolean equalsNullable(String a, String b) {
        return (a == null && b == null) || (a != null && a.equals(b));
    }

    public DiscoveredInterfaceDTO getOperationsByInterfaceId(Integer interfaceId) {
        return discoveredInterfaceMapper.convertToDiscoveredInterfaceDto(discoveredInterfaceRepository.findById(
                        interfaceId)
                                                                                 .orElseThrow(() -> new IllegalArgumentException(
                                                                                         "discoveredOperation с данным interfaceId не найден")));
    }
}