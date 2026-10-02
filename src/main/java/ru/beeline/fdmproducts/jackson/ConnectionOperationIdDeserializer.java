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
import java.math.BigInteger;

public class ConnectionOperationIdDeserializer extends JsonDeserializer<Integer> {

    public static final String NOT_INTEGER_MESSAGE = "Поле connectionOperationId должно быть целым числом";

    @Override
    public Integer deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        if (parser.currentToken() == JsonToken.VALUE_NUMBER_INT && fitsInInteger(parser)) {
            return parser.getIntValue();
        }
        throw JsonMappingException.from(parser, NOT_INTEGER_MESSAGE);
    }

    private boolean fitsInInteger(JsonParser parser) throws IOException {
        return switch (parser.getNumberType()) {
            case INT -> true;
            case LONG -> {
                long value = parser.getLongValue();
                yield value >= Integer.MIN_VALUE && value <= Integer.MAX_VALUE;
            }
            case BIG_INTEGER -> {
                BigInteger value = parser.getBigIntegerValue();
                yield value.compareTo(BigInteger.valueOf(Integer.MIN_VALUE)) >= 0
                        && value.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) <= 0;
            }
            default -> false;
        };
    }
}
