# Deepdesk

## Overview
Deepdesk is a platform for creating and managing AI agents for enterprise use cases. It provides organizations with the ability to leverage AI capabilities while maintaining control over their data and infrastructure.

## Purpose
- Create and manage AI agents for enterprise-specific use cases
- Provide a chat interface for interacting with AI agents and enterprise private data sources
- Enable companies to create custom AI agents without the complexity of multiple data source integrations
- Maintain data privacy and security within company infrastructure

## Key Features
- Chat interface for AI agent interaction
- Integration with enterprise private data sources
- Industry-standard MCP protocol for agent communication
- Flexible model integration through adapters
- Support for local open-source LLMs within company VPC
- No vendor lock-in

## Architecture Highlights
- Uses MCP (Model Control Protocol) for standardized agent communication
- Modular adapter system for connecting to various LLM providers
- Secure infrastructure design keeping business data within company VPC

## System Architecture
![System Architecture](images/archi.png)

## Getting Started
### Prerequisites
- Java Development Kit (JDK) 23 or higher
- Maven (included via Maven Wrapper)
- Node.js with `npx` (required for MCP server)

### Running the Application
To start the application locally:
```bash
./mvnw spring-boot:run
```

### Running Tests
To run the test suite:
```bash
./mvnw test
```

## Security & Privacy
Deepdesk is designed with enterprise security in mind:
- Business data remains within company infrastructure
- Support for local LLM hosting in private VPCs
- No external data transmission required when using local models

## License
This project is licensed under the GNU Affero General Public License v3.0 (AGPL-3.0). This license ensures that:
- The software can be freely used, modified, and distributed
- Any modifications to the software must be made available under the same license
- Network use is considered distribution, requiring source code to be made available to users interacting with the software over a network
- Full license text can be found in the LICENSE file

For more information, visit: https://www.gnu.org/licenses/agpl-3.0.en.html