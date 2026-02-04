package org.example.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ProductTest {

    static Stream<Arguments> provideDescriptionSettersInputs() {
        return Stream.of(
            Arguments.of(List.of("First description", "Second description"), "First description", "Multiple descriptions"),
            Arguments.of(List.of("Single description"), "Single description", "Single description"),
            Arguments.of(List.of(""), "", "Empty description"),
            Arguments.of(null, null, "Null descriptions"),
            Arguments.of(List.of(), null, "Empty list")
        );
    }

    static Stream<Arguments> provideCategorySettersInputs() {
        return Stream.of(
            Arguments.of(
                List.of("Men", "Women", "Clothing", "Sports", "Outdoor", "Fashion", "Casual"),
                List.of("Men", "Women", "Clothing", "Sports", "Outdoor"),
                "More than 5 categories - should be limited to 5"
            ),
            Arguments.of(
                List.of("Men", "Women", "Clothing"),
                List.of("Men", "Women", "Clothing"),
                "Less than 5 categories - should remain unchanged"
            ),
            Arguments.of(
                List.of("Electronics", "Computers", "Phones", "Tablets", "Accessories"),
                List.of("Electronics", "Computers", "Phones", "Tablets", "Accessories"),
                "Exactly 5 categories - should remain unchanged"
            ),
            Arguments.of(
                List.of("Single Category"),
                List.of("Single Category"),
                "Single category - should remain unchanged"
            ),
            Arguments.of(null, null, "Null categories"),
            Arguments.of(List.of(), null, "Empty list")
        );
    }

    @ParameterizedTest(name = "{2}: setDescription({0}) should set description to '{1}'")
    @MethodSource("provideDescriptionSettersInputs")
    void testSetDescriptionLogic(List<String> descriptions, String expectedDescription, String testCase) {
        final Product product = new Product();
        product.setDescription(descriptions);
        
        assertEquals(expectedDescription, product.getDescription(), testCase);
    }

    @ParameterizedTest(name = "{2}: setCategory({0}) should set category to {1}")
    @MethodSource("provideCategorySettersInputs")
    void testSetCategoryLogic(List<String> inputCategories, List<String> expectedCategories, String testCase) {
        final Product product = new Product();
        product.setCategory(inputCategories);
        
        assertEquals(expectedCategories, product.getCategory(), testCase);
    }

    @Test
    void testProductCreation() {
        final Product product = new Product();
        
        // Test that we can create a product
        assertNotNull(product);
    }
}