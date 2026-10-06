/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;

import java.io.IOException;

public class BiStepCodesDeserializer extends JsonDeserializer<BiStepCodesRequest> {

    public static final String MISSING_BODY = "Отсутствует тело запроса";
    public static final String INVALID_CODE = "Код bi-step не может быть пустым";

    @Override
    public BiStepCodesRequest deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        if (parser.currentToken() != JsonToken.START_ARRAY) {
            throw JsonMappingException.from(parser, MISSING_BODY);
        }
        BiStepCodesRequest codes = new BiStepCodesRequest();
        while (parser.nextToken() != JsonToken.END_ARRAY) {
            JsonToken token = parser.currentToken();
            if (token == JsonToken.VALUE_NULL) {
                codes.add(null);
            } else if (token == JsonToken.VALUE_STRING) {
                codes.add(parser.getText());
            } else {
                throw JsonMappingException.from(parser, INVALID_CODE);
            }
        }
        return codes;
    }
}
