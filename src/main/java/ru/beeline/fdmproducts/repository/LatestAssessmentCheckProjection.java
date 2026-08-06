package ru.beeline.fdmproducts.repository;

public interface LatestAssessmentCheckProjection {
    Integer getProductId();
    Integer getLacId();
    Boolean getIsCheck();
    Integer getFitnessFunctionId();
}
