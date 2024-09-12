# Kogo Content Backend API

This is a Kotlin Spring Boot application serving as a backend API service for the Kogo content platform.

**Prerequisites:**
- JDK 21 (or higher) is required.
- AWS CLI v2



```
# Build and Run:
./gradlew bootRun

# Running Tests:
./gradlew test

# Generate OpenAPI spec
./gradlew generateOpenApiDocs
```
코드를 사용할 때는 주의가 필요합니다.

The API documentation is hosted at
https://kogocampus.github.io/kogo-content-backend/.

### Retrieve AWS Cognito Oauth2 Access Token

**Install AWS CLI (if not already installed):**

Use the official AWS CLI v2 installation instructions: https://docs.aws.amazon.com/cli/latest/userguide/install-cliv2-linux.html
```
aws configure // enter your access id and secret
./bin/cognito-accesskey.sh
```

### Dependencies

**Install MongoDB Locally (if not already installed):**  
Follow the official MongoDB installation guide: https://www.mongodb.com/try/download/community  
Start the MongoDB Server in your local machine.

**Run Meilisearch Locally:**
```
./bin/run-local-meilisearch.sh
```
This script starts a local instance of Meilisearch for development purposes.
