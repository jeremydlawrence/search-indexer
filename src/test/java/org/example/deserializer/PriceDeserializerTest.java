package org.example.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PriceDeserializerTest {

    @Mock
    private JsonParser jsonParser;

    @Mock
    private DeserializationContext deserializationContext;

    private PriceDeserializer priceDeserializer;

    @BeforeEach
    void setUp() {
        priceDeserializer = new PriceDeserializer();
    }

    static Stream<Arguments> provideValidPriceInputs() {
        return Stream.of(
            Arguments.of("$12.99", new BigDecimal("12.99"), "Price with dollar sign"),
            Arguments.of("€25.50", new BigDecimal("25.50"), "Price with euro symbol"),
            Arguments.of("£15.75", new BigDecimal("15.75"), "Price with pound symbol"),
            Arguments.of("15.75", new BigDecimal("15.75"), "Price without currency symbol"),
            Arguments.of("  $  18.99  ", new BigDecimal("18.99"), "Price with whitespace"),
            Arguments.of("¥1000.00", new BigDecimal("1000.00"), "Price with yen symbol"),
            Arguments.of("$0.99", new BigDecimal("0.99"), "Price under one dollar"),
            Arguments.of("$1000000.00", new BigDecimal("1000000.00"), "Large price amount"),
            Arguments.of("$9.39 - $49.33", new BigDecimal("9.39"), "Price range - first value")
        );
    }

    static Stream<Arguments> provideEmptyPriceInputs() {
        return Stream.of(
            Arguments.of("", "Empty string"),
            Arguments.of(null, "Null string"),
            Arguments.of("   ", "Whitespace only")
        );
    }

    static Stream<Arguments> provideInvalidPriceInputs() {
        return Stream.of(
            Arguments.of("$abc.def", "Non-numeric characters after currency"),
            Arguments.of("not-a-price", "Completely invalid format"),
            Arguments.of("$", "Currency symbol only"),
            Arguments.of("12.34.56", "Multiple decimal points"),
            Arguments.of("$12.34.56", "Multiple decimal points with currency")
        );
    }

    @ParameterizedTest(name = "{2}: should parse {0} to {1}")
    @MethodSource("provideValidPriceInputs")
    void testDeserializeValidPrices(String input, BigDecimal expected, String description) throws IOException {
        when(jsonParser.getValueAsString()).thenReturn(input);
        
        final BigDecimal result = priceDeserializer.deserialize(jsonParser, deserializationContext);
        
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "{1}: should return null for input: {0}")
    @MethodSource("provideEmptyPriceInputs")
    void testDeserializeEmptyInputs(String input, String description) throws IOException {
        when(jsonParser.getValueAsString()).thenReturn(input);
        
        final BigDecimal result = priceDeserializer.deserialize(jsonParser, deserializationContext);
        
        assertNull(result, description);
    }

    @ParameterizedTest(name = "{1}: should throw IOException for input: {0}")
    @MethodSource("provideInvalidPriceInputs")
    void testDeserializeInvalidPrices(String input, String description) throws IOException {
        when(jsonParser.getValueAsString()).thenReturn(input);
        
        final IOException exception = assertThrows(IOException.class, () ->
                priceDeserializer.deserialize(jsonParser, deserializationContext), description);
        
        assertTrue(exception.getMessage().contains("Unable to parse price"));
    }
}