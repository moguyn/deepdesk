# 深知
[![CI](https://github.com/moguyn/deepdesk/actions/workflows/ci.yml/badge.svg)](https://github.com/moguyn/deepdesk/actions/workflows/ci.yml)
[![codecov](https://codecov.io/gh/moguyn/deepdesk/graph/badge.svg?token=Q46OP0MTY5)](https://codecov.io/gh/moguyn/deepdesk)
[![AGPL v3](https://img.shields.io/badge/License-AGPL%20v3-blue.svg)](https://www.gnu.org/licenses/agpl-3.0)

## 概述

深知是一个企业级平台，用于创建、管理和部署AI代理。它使组织能够利用先进的AI功能，同时保持对数据和基础设施的完全控制。

## 目的

深知解决了企业AI采用过程中的几个关键挑战：

- **定制代理创建**：构建和管理针对特定企业用例的AI代理
- **集成交互**：通过统一的聊天界面与AI代理和企业数据源交互
- **简化集成**：部署自定义AI代理，无需管理多个数据源集成的复杂性
- **安全优先设计**：在企业基础设施内维护数据隐私和安全

## 主要特性

- 💬 **交互式聊天界面**：与AI代理的无缝沟通
- 🔒 **私有数据集成**：安全连接到企业数据源
- 🔄 **MCP协议支持**：行业标准模型控制协议用于代理通信
- 🔌 **灵活模型集成**：适配各种LLM提供商的适配器
- 🏢 **本地LLM支持**：在企业VPC内运行开源LLM
- 🔓 **供应商独立性**：无供应商锁定

## 架构亮点

- **标准化通信**：MCP（模型控制协议）确保一致的代理交互
- **模块化设计**：适配器系统用于连接各种LLM提供商
- **企业级安全**：基础设施设计确保业务数据在您的控制之下

## 系统架构

![系统架构图](images/archi.png)

该图说明了**企业应用程序**（"MCP宿主"）协调以下组件：

1. **用户**  
   - 通过直观的**用户界面**访问系统
   - 通过**身份验证与授权**确保适当的权限

2. **MCP宿主**  
   - **权限管理器**：执行基于角色的访问控制
   - **交互层**：处理LLM和企业资源之间的通信
   - **MCP客户端**：建立与后端服务的HTTP通信
   - **配置系统**：管理应用程序设置和偏好

3. **LLM引擎**  
   - 根据需要执行语言处理和生成任务

4. **企业服务**  
   - **文档服务**：处理PDF、Word文档、TXT文件等
   - **数据库服务**：连接MySQL、PostgreSQL等数据存储
   - **API服务**：对接内部企业API
   - **附加服务**：扩展到其他企业功能

**数据流**：  
用户 → 用户界面 → MCP宿主（权限验证）→ MCP客户端 → 企业服务（通过HTTP）  
LLM通过MCP宿主调用以执行所有语言模型操作。

## 快速开始

### 前置要求

- Java开发工具包 (JDK) 21或更高版本
- Maven（通过Maven Wrapper提供）
- Node.js和`npx`（MCP服务器所需）
- Python 3.13.x和uv包管理器（MCP服务器所需）
- 配置为环境变量的API密钥：

```shell
export OPENAI_API_KEY='your-openai-api-key'
export ANTHROPIC_API_KEY='your-anthropic-api-key'
export BRAVE_API_KEY="your-brave-api-key"
```

### 运行应用

在本地启动应用：

```bash
./mvnw spring-boot:run
```

### 构建可执行JAR

使用SpringBoot创建可执行JAR文件：

```bash
./mvnw clean package
```

可执行JAR将在`target`目录中创建为`deepdesk-<version>.jar`。

运行可执行文件：

```bash
java -jar target/deepdesk-<version>.jar
```

### 运行测试

执行测试套件：

```bash
./mvnw test
```

## 原生构建

深知支持生成可在Linux、macOS和Windows上不需要JVM运行的原生可执行文件。

### 本地构建原生可执行文件

#### Linux/macOS

```bash
# 使脚本可执行（仅首次）
chmod +x build-native.sh

# 构建原生可执行文件
./build-native.sh
```

#### Windows

```batch
# 构建原生可执行文件
build-native.bat
```

原生可执行文件将在`target`目录中创建为`deepdesk`。

### 自动化多平台构建

所有支持平台的原生可执行文件都通过GitHub Actions自动构建。从本仓库的GitHub Releases部分下载最新版本。

### 原生构建的要求

要在本地构建原生可执行文件：

- GraalVM安装，含native-image工具
- Java 17或更新版本（推荐Java 23）
- Maven 3.8+

## 已知问题

- 流式模式尚不支持（由于[Spring中的这个问题](https://github.com/spring-projects/spring-ai/issues/2341)）
   - 可通过[src/test/resources/chat.http]复现

## 安全与隐私

深知以企业安全为基础进行架构设计：

- **数据主权**：业务数据保持在企业基础设施内
- **私有部署**：支持在私有VPC中托管本地LLM
- **零数据泄漏**：使用本地模型时无需外部数据传输

## 许可证

本项目采用GNU Affero通用公共许可证v3.0（AGPL-3.0）授权。该许可证确保：

- 软件可以自由使用、修改和分发
- 任何修改都必须以相同的许可证提供
- 网络使用被视为分发，要求提供源代码
- 完整许可证文本可在LICENSE文件中找到

更多信息，请访问：https://www.gnu.org/licenses/agpl-3.0.en.html
