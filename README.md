# Kogo Content Backend API

This is a Kotlin Spring Boot application serving as a backend API service for the Kogo content platform.

## Edit Spring configuration for the remote environments

Decrypt configuration:
```bash
sops --config=src/main/resources/sops.yml -d -i src/main/resources/application-{env}.yml
```

Encrypt configuration before push:
```bash
sops --config=src/main/resources/sops.yml -e -i src/main/resources/application-{env}.yml
```

## Development

### Create docker-compose test environment (Only required for integration tests)  
```bash
# Start test MongoDB
docker compose -f docker-compose.test.yml up
```

if you want to run the full test cases  
```bash
# Run all tests (including integration)
./gradlew test
```

### Run application  
```bash
# Development
./gradlew bootRun

# Specific profile
./gradlew bootRun --args='--spring.profiles.active=stg'
```

###Build
```bash
./gradlew clean build -x test  # Skip tests
./gradlew clean build          # With tests
```

