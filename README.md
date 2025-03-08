# Deepdesk
[![CI](https://github.com/moguyn/deepdesk/actions/workflows/ci.yml/badge.svg)](https://github.com/moguyn/deepdesk/actions/workflows/ci.yml)
[![codecov](https://codecov.io/gh/moguyn/deepdesk/graph/badge.svg?token=Q46OP0MTY5)](https://codecov.io/gh/moguyn/deepdesk)
[![AGPL v3](https://img.shields.io/badge/License-AGPL%20v3-blue.svg)](https://www.gnu.org/licenses/agpl-3.0)

## Overview

Deepdesk is an enterprise-grade platform for creating, managing, and deploying AI agents. It empowers organizations to leverage advanced AI capabilities while maintaining complete control over their data and infrastructure.

## Purpose

Deepdesk addresses several key challenges in enterprise AI adoption:

- **Custom Agent Creation**: Build and manage AI agents tailored to specific enterprise use cases
- **Integrated Interaction**: Access a unified chat interface for engaging with AI agents and enterprise data sources
- **Simplified Integration**: Deploy custom AI agents without the complexity of managing multiple data source integrations
- **Security-First Design**: Maintain data privacy and security within your company's infrastructure

## Key Features

- 💬 **Interactive Chat Interface**: Seamless communication with AI agents
- 🔒 **Private Data Integration**: Connect securely to enterprise data sources
- 🔄 **MCP Protocol Support**: Industry-standard Model Control Protocol for agent communication
- 🔌 **Flexible Model Integration**: Adapters for various LLM providers
- 🏢 **On-Premises LLM Support**: Run open-source LLMs within your company VPC
- 🔓 **Vendor Independence**: No vendor lock-in

## Architecture Highlights

- **Standardized Communication**: MCP (Model Control Protocol) for consistent agent interactions
- **Modular Design**: Adapter system for connecting to various LLM providers
- **Enterprise Security**: Infrastructure design that keeps business data within your control

## System Architecture

![System Architecture](images/archi.png)

This diagram illustrates the **enterprise application** ("MCP Host") that coordinates:

1. **User**  
   - Accesses the system through an intuitive **UI**  
   - Passes through **Authentication & Authorization** to ensure proper permissions

2. **MCP Host**  
   - **Permissions Manager**: Enforces role-based access controls
   - **Interaction Layer**: Handles communication between LLMs and enterprise resources
   - **MCP Client**: Establishes HTTP-based communication with backend services
   - **Configuration System**: Manages application settings and preferences

3. **LLM Engine**  
   - Performs language processing and generation tasks as needed

4. **Enterprise Services**  
   - **Document Service**: Processes PDFs, Word docs, TXT files, and more
   - **Database Service**: Connects to MySQL, PostgreSQL, and other datastores
   - **API Service**: Interfaces with internal enterprise APIs
   - **Additional Services**: Extends to other enterprise capabilities

**Data Flow**:  
User → UI → MCP Host (permissions verification) → MCP Client → Enterprise Services (via HTTP)  
The LLM is invoked through the MCP Host for all language model operations.

## Getting Started

### Prerequisites

- Java Development Kit (JDK) 21 or higher
- Maven (included via Maven Wrapper)
- Node.js with `npx` (required for MCP server)
- Python 3.13.x with uv package manager (required for MCP server)
- API keys configured as environment variables:

```shell
export OPENAI_API_KEY='your-openai-api-key'
export ANTHROPIC_API_KEY='your-anthropic-api-key'
export BRAVE_API_KEY="your-brave-api-key"
```

### Running the Application

Start the application locally:

```bash
./mvnw spring-boot:run
```

### Building the Executable JAR

Create an executable JAR file with SpringBoot:

```bash
./mvnw clean package
```

The executable JAR will be created in the `target` directory as `deepdesk-<version>.jar`.

To run the executable:

```bash
java -jar target/deepdesk-<version>.jar
```

### Running Tests

Execute the test suite:

```bash
./mvnw test
```

## Native Builds

Deepdesk supports native executable generation that can run without a JVM on Linux, macOS, and Windows.

### Building Native Executables Locally

#### Linux/macOS

```bash
# Make the script executable (first time only)
chmod +x build-native.sh

# Build the native executable
./build-native.sh
```

#### Windows

```batch
# Build the native executable
build-native.bat
```

The native executable will be created in the `target` directory as `deepdesk`.

### Automated Multi-Platform Builds

Native executables for all supported platforms are automatically built via GitHub Actions. Download the latest releases from the GitHub Releases section of this repository.

### Requirements for Native Builds

To build native executables locally:

- GraalVM installation with native-image utility
- Java 17 or newer (Java 23 recommended)
- Maven 3.8+

## Security & Privacy

Deepdesk is architected with enterprise security as a foundation:

- **Data Sovereignty**: Business data remains within your company's infrastructure
- **Private Deployment**: Support for local LLM hosting in private VPCs
- **Zero Data Leakage**: No external data transmission required when using local models

## License

This project is licensed under the GNU Affero General Public License v3.0 (AGPL-3.0). This license ensures:

- Free use, modification, and distribution of the software
- Any modifications must be made available under the same license
- Network use is considered distribution, requiring source code availability
- Complete license text can be found in the LICENSE file

For more information, visit: https://www.gnu.org/licenses/agpl-3.0.en.html