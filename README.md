# SearchIndexer

A Spring Boot application for indexing product data into OpenSearch.
This service provides REST APIs for bulk indexing products from JSON files and monitoring OpenSearch cluster health.
The goal of this project is to index documents into OpenSearch for various search practice projects.

## Features

- **Product Indexing**: Bulk index products from JSON files into OpenSearch
- **Cluster Health Monitoring**: Check OpenSearch cluster status
- **RESTful API**: Clean REST endpoints with Swagger documentation
- **Configurable Batch Processing**: Configurable batch sizes for optimal performance
- **Error Handling**: Comprehensive error handling and logging
- **JSON Flexibility**: Robust JSON parsing with aliases and custom deserializers

## Tech Stack

- **Java 25** with Spring Boot 4.0.2
- **OpenSearch 3.5.0** for search and indexing
- **Jackson** for JSON processing
- **Lombok** for reducing boilerplate code
- **JUnit 5** for testing
- **Swagger/OpenAPI 3** for API documentation

## Quick Start

### Prerequisites

- Java 25 or higher
- Gradle 9.0+
- OpenSearch cluster running locally or remotely

### Installation

1. Clone the repository:
```bash
git clone <repository-url>
cd SearchIndexer
```

2. Configure OpenSearch connection in `src/main/resources/application.yml`:
```yaml
open-search:
  protocol: http
  host: localhost
  port: 9200
```

3. Build the project:
```bash
./gradlew build
```

4. Run the application:
```bash
./gradlew bootRun
```

The application will start on `http://localhost:8080`

## Configuration

### OpenSearch Configuration

Configure your OpenSearch cluster connection in `application.yml`:

```yaml
open-search:
  protocol: http        # Protocol (http/https)
  host: localhost      # OpenSearch host
  port: 9200          # OpenSearch port

indexing:
  product:
    index-name: products    # OpenSearch index name
    batch-size: 133         # Batch size for bulk operations
```

### Logging Configuration

Configure logging levels and patterns:

```yaml
logging:
  level:
    org.example: DEBUG  # Set to INFO for production
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%level] - %msg%n"
```

## API Documentation

Once the application is running, you can access:

- **Swagger UI**: `http://localhost:8080/swagger-ui.html`
- **OpenAPI Docs**: `http://localhost:8080/api-docs`

### API Endpoints

#### Index Products
```http
GET /index-products
```
Triggers the indexing process for products from the configured JSON file.

**Response:**
```json
"Successfully indexed 1234 products"
```

#### Check OpenSearch Health
```http
GET /index-health
```
Checks the health status of the OpenSearch cluster.

**Response:**
```json
"OpenSearch status: GREEN"
```

#### Application Info
```http
GET /
```
Returns basic application information.

## Data Format

### Product JSON Structure

The indexer expects JSON lines (one JSON object per line) with the following structure:

```json
{
  "id": "B001234567",
  "asin": "B001234567",
  "title": "Product Name",
  "description": ["Product description line 1", "Product description line 2"],
  "category": ["Electronics", "Computers", "Laptops"],
  "price": 999.99,
  "image": ["http://example.com/image1.jpg", "http://example.com/image2.jpg"]
}
```

### Field Mappings

- `id` / `asin`: Product identifier
- `title`: Product title
- `description`: Array of descriptions (first one used)
- `category`: Array of categories (limited to 5)
- `price`: Product price (supports decimal values)
- `image` / `imageURLHighRes`: Array of image URLs

## Development

### Build Commands

```bash
# Build the project
./gradlew build

# Run tests
./gradlew test

# Run tests with output
./gradlew test --info

# Run specific test class
./gradlew test --tests "org.example.model.ProductTest"

# Clean and rebuild
./gradlew clean build
```

### Running Locally

```bash
# Start the application
./gradlew bootRun

# Or build and run the JAR
./gradlew build
java -jar build/libs/SearchIndexer-1.0-SNAPSHOT.jar
```

### Project Structure

```
src/
├── main/
│   ├── java/org/example/
│   │   ├── SearchIndexerApplication.java    # Main application
│   │   ├── config/                          # Configuration classes
│   │   ├── controller/                      # REST controllers
│   │   ├── model/                          # Data models
│   │   ├── service/                        # Business logic
│   │   ├── indexer/                        # Indexing components
│   │   └── deserializer/                   # Custom JSON deserializers
│   └── resources/
│       └── application.yml                 # Configuration
└── test/
    └── java/org/example/                   # Test classes
```

## Testing

The project uses JUnit 5 with parameterized tests. Run all tests:

```bash
./gradlew test
```

Key test areas:
- Model validation and data transformation
- JSON deserialization
- Business logic edge cases

## Performance Considerations

- **Batch Processing**: Products are indexed in configurable batches (default: 133)
- **Memory Management**: Streaming JSON parser to handle large files
- **Error Resilience**: Failed bulk operations are logged but don't stop the process
- **Monitoring**: Detailed logging for debugging and monitoring

## Monitoring and Logging

- Application logs include indexing progress, error details, and performance metrics
- OpenSearch operation results are logged with timing information
- Debug logging can be enabled for detailed troubleshooting

## Error Handling

The application includes comprehensive error handling:

- **File I/O Errors**: Graceful handling of missing or corrupted files
- **JSON Parsing**: Invalid JSON lines are skipped and logged
- **OpenSearch Errors**: Bulk operation errors are logged with details
- **Network Issues**: Connection problems to OpenSearch are handled