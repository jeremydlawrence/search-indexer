package org.example.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.math.BigDecimal;

public class PriceDeserializer extends JsonDeserializer<BigDecimal> {

    @Override
    public BigDecimal deserialize(final JsonParser p, final DeserializationContext ctxt) throws IOException {
        String priceString = p.getValueAsString();
        
        if (priceString == null || priceString.trim().isEmpty()) {
            return null;
        }

        // If range exists, just use the first value
        if (priceString.indexOf("$") != priceString.lastIndexOf("$")) {
            priceString = priceString.substring(0, priceString.lastIndexOf("$"));
        }
        
        // Remove currency symbols and whitespace
        String cleanPrice = priceString.replaceAll("[^\\d.]", "");
        
        try {
            return new BigDecimal(cleanPrice);
        } catch (NumberFormatException e) {
            throw new IOException("Unable to parse price: " + priceString, e);
        }
    }
}