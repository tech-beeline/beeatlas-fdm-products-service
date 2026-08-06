/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.annotation;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static ru.beeline.fdmproducts.utils.Constant.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Parameters({
        @Parameter(name = USER_ROLES_HEADER, in = ParameterIn.HEADER, schema = @Schema(type = "string")),
        @Parameter(name = USER_ID_HEADER, in = ParameterIn.HEADER, schema = @Schema(type = "string")),
        @Parameter(name = USER_PERMISSION_HEADER, in = ParameterIn.HEADER, schema = @Schema(type = "string")),
        @Parameter(name = USER_PRODUCTS_IDS_HEADER, in = ParameterIn.HEADER, schema = @Schema(type = "string"))
})
public @interface CustomHeaders {
}
