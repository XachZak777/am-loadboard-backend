#!/bin/bash

# Load environment variables from .env file
if [ -f .env ]; then
    echo "📝 Loading environment variables from .env file..."
    set -a
    source .env
    set +a
    echo "✅ Environment variables loaded"
else
    echo "⚠️  .env file not found. Using system environment variables or defaults."
fi

# Run the application
echo "🚀 Starting Load Board Backend..."
./mvnw spring-boot:run
