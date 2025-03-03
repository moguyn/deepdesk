#!/bin/bash
set -e

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${YELLOW}Building DeepDesk native executable...${NC}"

# Check if java is installed
if ! command -v java &> /dev/null; then
    echo -e "${RED}Java is not installed. Please install Java 17 or higher.${NC}"
    exit 1
fi

# Get Java version
JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | awk -F '.' '{print $1}')
if [ -z "$JAVA_VERSION" ] || [ "$JAVA_VERSION" -lt 17 ]; then
    echo -e "${RED}Java 17 or higher is required. Current version: $JAVA_VERSION${NC}"
    exit 1
fi

echo -e "${GREEN}Using Java version: $(java -version 2>&1 | head -n 1)${NC}"

# Check operating system
OS=$(uname -s | tr '[:upper:]' '[:lower:]')
if [[ "$OS" == "darwin"* ]]; then
    PLATFORM="macos"
elif [[ "$OS" == "linux"* ]]; then
    PLATFORM="linux"
else
    echo -e "${RED}Unsupported OS: $OS. Please use Linux or macOS.${NC}"
    exit 1
fi

# Build the native executable
echo -e "${YELLOW}Building native executable for $PLATFORM...${NC}"
./mvnw clean package -Pnative

# Check if build succeeded
if [ $? -ne 0 ]; then
    echo -e "${RED}Build failed.${NC}"
    exit 1
fi

echo -e "${GREEN}Native executable for $PLATFORM created: ./target/deepdesk${NC}"
echo -e "${YELLOW}Run it with: ./target/deepdesk${NC}" 
