/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.domain;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Ветки архитектуры, у которых есть свои атрибуты связи discovered → arch (SFDM-4318): main пишет в
 * connection_*_id, develop — в connection_*_dev_id, design — в connection_*_design_id. Связи веток живут
 * независимо: запись или очистка одной ветки не трогает атрибуты двух других.
 * <p>
 * Ветка вне этих трёх атрибутов связи не имеет — с discovered она не сопоставляется.
 */
public enum DiscoveredConnectionBranch {

    MAIN("main",
            DiscoveredOperation::getConnectionOperationId, DiscoveredOperation::setConnectionOperationId,
            DiscoveredInterface::getConnectionInterfaceId, DiscoveredInterface::setConnectionInterfaceId),
    DEVELOP("develop",
            DiscoveredOperation::getConnectionOperationDevId, DiscoveredOperation::setConnectionOperationDevId,
            DiscoveredInterface::getConnectionInterfaceDevId, DiscoveredInterface::setConnectionInterfaceDevId),
    DESIGN("design",
            DiscoveredOperation::getConnectionOperationDesignId, DiscoveredOperation::setConnectionOperationDesignId,
            DiscoveredInterface::getConnectionInterfaceDesignId, DiscoveredInterface::setConnectionInterfaceDesignId);

    /** Каноническое имя ветки; его же получают native-запросы, выбирающие колонку ветки. */
    private final String branchName;
    private final Function<DiscoveredOperation, Integer> operationGetter;
    private final BiConsumer<DiscoveredOperation, Integer> operationSetter;
    private final Function<DiscoveredInterface, Integer> interfaceGetter;
    private final BiConsumer<DiscoveredInterface, Integer> interfaceSetter;

    DiscoveredConnectionBranch(String branchName,
                               Function<DiscoveredOperation, Integer> operationGetter,
                               BiConsumer<DiscoveredOperation, Integer> operationSetter,
                               Function<DiscoveredInterface, Integer> interfaceGetter,
                               BiConsumer<DiscoveredInterface, Integer> interfaceSetter) {
        this.branchName = branchName;
        this.operationGetter = operationGetter;
        this.operationSetter = operationSetter;
        this.interfaceGetter = interfaceGetter;
        this.interfaceSetter = interfaceSetter;
    }

    public String getBranchName() {
        return branchName;
    }

    /** Ветка по имени без учёта регистра, как сравнивается main; пусто — у ветки нет своих атрибутов. */
    public static Optional<DiscoveredConnectionBranch> fromBranchName(String branchName) {
        return Arrays.stream(values())
                .filter(branch -> branch.branchName.equalsIgnoreCase(branchName))
                .findFirst();
    }

    public Integer getConnectionOperationId(DiscoveredOperation discoveredOperation) {
        return operationGetter.apply(discoveredOperation);
    }

    public void setConnectionOperationId(DiscoveredOperation discoveredOperation, Integer operationId) {
        operationSetter.accept(discoveredOperation, operationId);
    }

    public Integer getConnectionInterfaceId(DiscoveredInterface discoveredInterface) {
        return interfaceGetter.apply(discoveredInterface);
    }

    public void setConnectionInterfaceId(DiscoveredInterface discoveredInterface, Integer interfaceId) {
        interfaceSetter.accept(discoveredInterface, interfaceId);
    }
}
