package ru.beeline.fdmproducts.domain;

import java.util.List;

public record ProductInfraProjection(String name, List<String> parentSystems) {}
