#!/bin/bash

# OAuth2 access token from the staging userpool.
#
# The script provides a simple way to fetch tokens for testing or local development environments where
# access to AWS Cognito staging user pool is needed.
#
# Authorization Bearer {access_token}
#
# Requirements:
# - AWS CLI must be installed and configured with an appropriate user profile.

check_aws_cli() {
  if ! command -v aws &> /dev/null
  then
    echo "Error: AWS CLI is not installed. Please install AWS CLI and try again."
    exit 1
  fi
}

configure_cognito_client_credentials() {
  SECRET_NAME="kogo_stg_cognito_client_secrets"  # Replace with your secret name
  AWS_REGION="us-west-2"  # Replace with the AWS region where the secret is stored

  echo "Retrieving client credentials from AWS Secrets Manager..."

  # Retrieve the secret from AWS Secrets Manager
  secret=$(aws secretsmanager get-secret-value --secret-id "$SECRET_NAME" --region "$AWS_REGION" --query 'SecretString' --output text)

  # Extract client_id and client_secret from the retrieved secret
  CLIENT_ID=$(echo "$secret" | sed -n 's/.*"client_id":"\([^"]*\)".*/\1/p')
  CLIENT_SECRET=$(echo "$secret" | sed -n 's/.*"client_secret":"\([^"]*\)".*/\1/p')

  if [[ -z "$CLIENT_ID" || -z "$CLIENT_SECRET" ]]; then
    echo "Error: Failed to retrieve client credentials."
    exit 1
  fi

  echo "Client credentials retrieved successfully."
}

check_aws_cli
configure_cognito_client_credentials

# OAuth2 Authentication
AUTH_URL="https://kogo-dev.auth.us-west-2.amazoncognito.com/oauth2/authorize"
TOKEN_URL="https://kogo-dev.auth.us-west-2.amazoncognito.com/oauth2/token"
CALLBACK_URL="https://kogocampus.com"

echo "Opening the authorization page in your browser..."
sleep 3

echo "Please enter the authorization code from the callback URL: "
open "${AUTH_URL}?response_type=code&client_id=${CLIENT_ID}&redirect_uri=${CALLBACK_URL}"

read -p "Authorization Code: " AUTH_CODE

echo "Requesting access token..."

response=$(curl -X POST "${TOKEN_URL}" \
  -H "Authorization: Basic $(echo -n "${CLIENT_ID}:${CLIENT_SECRET}" | base64)" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=authorization_code" \
  -d "code=${AUTH_CODE}" \
  -d "redirect_uri=${CALLBACK_URL}")

json_extract() {
  echo "$1" | sed -n "s/.*\"$2\":\"\([^\"]*\)\".*/\1/p"
}

# Extract tokens and relevant fields using regex
ACCESS_TOKEN=$(json_extract "$response" "access_token")
ID_TOKEN=$(json_extract "$response" "id_token")
REFRESH_TOKEN=$(json_extract "$response" "refresh_token")
EXPIRES_IN=$(json_extract "$response" "expires_in")
TOKEN_TYPE=$(json_extract "$response" "token_type")

# Prompt user to print access token alone or all details
echo "Access Token: $ACCESS_TOKEN"

read -p "Do you want to see all the token information? (y/n): " show_all_info
if [[ "$show_all_info" == "y" ]]; then
  echo "ID Token: $ID_TOKEN"
  echo "Refresh Token: $REFRESH_TOKEN"
  echo "Expires In: $EXPIRES_IN seconds"
  echo "Token Type: $TOKEN_TYPE"
fi



