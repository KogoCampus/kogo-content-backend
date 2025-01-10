#!/bin/bash

# App Runner service information
SERVICE_ARN="arn:aws:apprunner:us-west-2:992382730467:service/production-kogo-content-service/c89157809c5d4fcbbbc8f3842a13f761"
REGION="us-west-2"

# Check if auto deploy is enabled (i.e., skipping confirmation)
AUTO_DEPLOY=false
if [[ "$1" == "--auto" ]]; then
    AUTO_DEPLOY=true
fi

# Function to check App Runner service status
check_service_status() {
    echo "Checking App Runner service status for: $SERVICE_ARN"

    STATUS=$(aws apprunner describe-service \
        --service-arn "$SERVICE_ARN" \
        --region "$REGION" \
        --query 'Service.Status' \
        --output text)

    echo "Current App Runner service status: $STATUS"

    # Check if the service is in a deployable state
    if [[ "$STATUS" != "RUNNING" && "$STATUS" != "OPERATION_IN_PROGRESS" ]]; then
        echo "Service is not in a deployable state. Current status: $STATUS"
        exit 1
    elif [[ "$STATUS" == "OPERATION_IN_PROGRESS" ]]; then
        echo "A deployment is already in progress. Exiting."
        exit 1
    fi
}

# Function to trigger the App Runner deployment
trigger_deploy() {
    echo "Triggering App Runner deployment for service: $SERVICE_ARN"

    aws apprunner start-deployment \
        --service-arn "$SERVICE_ARN" \
        --region "$REGION"

    if [ $? -eq 0 ]; then
        echo "Deployment successfully triggered."
    else
        echo "Failed to trigger deployment."
        exit 1
    fi
}

# Check service status first
check_service_status

# If auto deploy is enabled, skip confirmation
if [ "$AUTO_DEPLOY" = true ]; then
    echo "Auto deploy enabled. Proceeding with deployment..."
    trigger_deploy
    exit 0
fi

# Prompt user for confirmation to trigger deployment
read -p "Do you want to trigger the App Runner deployment? (yes/no): " answer

# Check user input and trigger deployment if 'yes'
case "$answer" in
    yes|Yes|y|Y)
        trigger_deploy
        ;;
    no|No|n|N)
        echo "Deployment aborted."
        ;;
    *)
        echo "Invalid input. Please enter 'yes' or 'no'."
        exit 1
        ;;
esac
