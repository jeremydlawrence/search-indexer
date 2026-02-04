# SearchIndexer - Agent Development Guide

This document provides essential information for agentic coding agents working on the SearchIndexer Spring Boot project.

## Project Overview
- **Type**: Spring Boot 4.0.2 application with Java 25
- **Purpose**: Product indexing service for OpenSearch
- **Architecture**: MVC pattern with service layer and OpenSearch integration
- **Testing**: JUnit 5 with parameterized tests

## Build and Development Commands

### Core Commands
```bash
# Build the entire project
./gradlew build

# Clean and rebuild
./gradlew clean build

# Run the application locally
./gradlew bootRun

# Run all tests
./gradlew test

# Run tests with console output
./gradlew test --info

# Run a single test class
./gradlew test --tests "org.example.model.ProductTest"

# Run a specific test method
./gradlew test --tests "org.example.model.ProductTest.testSetDescriptionLogic"

# Run parameterized tests with specific test case
./gradlew test --tests "*ProductTest.testSetCategoryLogic*" --info
```

### Verification and Quality
```bash
# Run all checks (includes tests)
./gradlew check

# Generate Javadoc
./gradlew javadoc

# View project dependencies
./gradlew dependencies

# Check dependency updates
./gradlew dependencyUpdates
```

## Code Style and Conventions

### Package Structure
```
org.example/
├── SearchIndexerApplication.java     # Main application class
├── config/                           # Configuration classes
├── controller/                       # REST controllers
├── model/                           # Data models/entities
├── service/                         # Business logic services
├── indexer/                         # Data indexing components
└── deserializer/                    # JSON deserializers
```

### Import Organization
- Order: java.*, javax.*, org.*, com.*
- Group imports: static imports last
- No wildcard imports (`*`)
- Use lombok for getters/setters, avoid manual boilerplate

### Class and Method Conventions
- **Classes**: PascalCase, descriptive nouns (e.g., `ProductIndexer`, `OpenSearchService`)
- **Methods**: camelCase, descriptive verbs (e.g., `bulkIndex`, `clusterHealth`)
- **Constants**: UPPER_SNAKE_CASE
- **Fields**: camelCase, use `final` where possible

### Annotations Usage
- **Lombok**: `@Data` for models, `@RequiredArgsConstructor` for dependency injection
- **Spring**: `@Service`, `@RestController`, `@Configuration`, `@Autowired`
- **Swagger**: `@Tag`, `@Operation`, `@ApiResponses` for API documentation
- **Jackson**: `@JsonProperty`, `@JsonAlias`, `@JsonDeserialize`, `@JsonIgnoreProperties`

### Error Handling Patterns
- Use SLF4J logging: `private static final Logger logger = LoggerFactory.getLogger(ClassName.class);`
- Log levels: `logger.info()` for business events, `logger.error()` for exceptions, `logger.debug()` for details
- Exception handling: Catch specific exceptions, log with context, wrap in RuntimeException with descriptive message
- Validation: Check for null/empty inputs early, return early for void methods

### Spring Boot Specific Patterns
- Constructor injection preferred over field injection
- Use `@Autowired` on constructors when multiple parameters
- Return meaningful responses from REST endpoints
- Use `@RestController` with appropriate HTTP method annotations

### Testing Standards
- **Test naming**: `testMethodName_Scenario_ExpectedResult` or descriptive names
- **Parameterized tests**: Use `@MethodSource` for complex test data, include descriptive test names
- **Assertions**: Use JUnit 5 assertions, prefer `assertEquals` with message parameter
- **Test structure**: Arrange-Act-Assert pattern, keep tests focused and isolated

### JSON and Data Handling
- Use Jackson annotations for flexible JSON mapping
- Implement custom deserializers for complex data types (e.g., `PriceDeserializer`)
- Handle null/empty data gracefully in model setters
- Use `BigDecimal` for monetary values

### OpenSearch Integration
- Use OpenSearch Java client 3.5.0 with Apache HttpClient5 transport
- Implement bulk operations for performance
- Log cluster health status and operation results
- Handle index errors with detailed logging

### Configuration Management
- Externalize configuration using `@ConfigurationProperties`
- Use Spring Boot's configuration binding
- Provide sensible defaults for optional properties

## Development Workflow

### When Adding New Features
1. Create/update model classes in `org.example.model`
2. Implement business logic in `org.example.service`
3. Add REST endpoints in `org.example.controller` if needed
4. Write comprehensive tests in `src/test/java`
5. Update Swagger documentation for API changes

### When Modifying Existing Code
1. Follow existing patterns and conventions
2. Maintain test coverage - add tests for new functionality
3. Update documentation if API contracts change
4. Run full test suite before committing

### Performance Considerations
- Use bulk operations for OpenSearch indexing
- Implement proper logging levels to avoid performance overhead
- Consider async processing for large indexing operations
- Monitor memory usage with large document batches

## Environment Setup
- Java 25 toolchain configured via Gradle
- OpenSearch cluster required for integration testing
- Use `application.properties` for environment-specific configuration
- Gradle wrapper included for consistent builds

## Common Pitfalls to Avoid
- Don't use `@Autowired` on fields without constructor injection
- Avoid catching `Exception` broadly - be specific
- Don't ignore OpenSearch bulk operation errors
- Never commit sensitive configuration to version control
- Avoid hardcoded file paths - use resource loading

## Testing Best Practices
- Test edge cases: null inputs, empty collections, boundary conditions
- Use parameterized tests for multiple scenarios with clear descriptions
- Mock external dependencies (OpenSearch client) in unit tests
- Integration tests should use test OpenSearch cluster
- Keep test execution fast - avoid unnecessary I/O operations