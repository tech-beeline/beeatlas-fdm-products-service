package ru.beeline.fdmproducts.dto.search.projection;

public interface ArchOperationProjection {

    Integer getOpId();
    String getOpName();
    String getOpType();
    Integer getInterfaceId();
    String getInterfaceName();
    String getInterfaceCode();
    Integer getContainerId();
    String getContainerName();
    String getContainerCode();
    Integer getProductId();
    String getProductName();
    String getProductAlias();
}
