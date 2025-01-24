# Kogo Content Backend API

This is a Kotlin Spring Boot application serving as a backend API service for the Kogo content platform.

## Development

### Create docker-compose test environment (Only required for integration tests)
```bash
# Start test MongoDB
docker compose -f docker-compose.test.yml up
```

### Manaul methods to run application and tests (Optional if using IDE)
if you want to run the full test cases
```bash
# Run all tests (including integration)
./gradlew test
```

### Run application
```bash
# Development
./gradlew bootRun
```

### Build
```bash
./gradlew clean build -x test  # Skip tests
./gradlew clean build          # With tests
```

## Configuration for the remote environments

### Install SOPS (if not installed)
```bash
# macOS
brew install sops

# Linux
curl -L https://github.com/mozilla/sops/releases/download/v3.9.1/sops-v3.9.1.linux.amd64 -o sops
chmod +x sops
sudo mv sops /usr/local/bin/
```

### Manage configurations
Decrypt configuration:
```bash
sops --config=src/main/resources/sops.yml -d -i src/main/resources/application-{env}.yml
```

Encrypt configuration before push:
```bash
sops --config=src/main/resources/sops.yml -e -i src/main/resources/application-{env}.yml
```
