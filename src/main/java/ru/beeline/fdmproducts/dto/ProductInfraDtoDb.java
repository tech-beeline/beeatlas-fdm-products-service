/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.dto;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ProductInfraDtoDb {
    private final String name;
    private final List<String> parentSystems;

    public ProductInfraDtoDb(String name, String parentSystemsStr) {
        this.name = name;
        this.parentSystems = parentSystemsStr.isEmpty()
                ? Collections.emptyList()
                : Arrays.asList(parentSystemsStr.split(","));
    }

    public String getName() {
        return name;
    }

    public List<String> getParentSystems() {
        return parentSystems;
    }
}