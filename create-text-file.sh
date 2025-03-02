#!/bin/bash

# Get the directory where the script is located
SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)

# Define the file path in the script's directory
FILE_PATH="$SCRIPT_DIR/target/deepdesk.txt"

echo "Creating a text file in $FILE_PATH..."

# Create the file
cat << 'EOF' > "$FILE_PATH"

Deepdesk is a platform for creating and managing AI agents for enterprise use cases.
It provides a chat interface to interact with the AI agents and enterprise private data sources.
It allows companies to create custom AI agents for their use cases without the heavy lifting of integrating with multiple data sources.
It avoids "lock-in" by using industry standard MCP protocol for agent communication and the ability to connect to any LLM or model through adapters.
For instance, by using a local open source LLM hosted in the company's VPC, the business data will never leave the company's infrastructure.


EOF

echo "The file has been created in $FILE_PATH."
