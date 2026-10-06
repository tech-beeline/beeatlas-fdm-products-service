/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.jackson;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.ArrayList;

@JsonDeserialize(using = BiStepCodesDeserializer.class)
public class BiStepCodesRequest extends ArrayList<String> {
}
