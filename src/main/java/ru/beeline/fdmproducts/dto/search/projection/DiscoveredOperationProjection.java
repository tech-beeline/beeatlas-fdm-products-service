package ru.beeline.fdmproducts.dto.search.projection;

public interface DiscoveredOperationProjection {

    Integer getOpId();
    String getOpName();
    String getOpType();
    String getInterfaceCode();
    String getProductAlias();
    String getSource();
    Double getRps();
    Double getLatency();
    Double getErrorRate();
}
