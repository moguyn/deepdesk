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

## 架构原则

受[Anthropic构建高效代理方法](https://www.anthropic.com/engineering/building-effective-agents)的启发，深知遵循以下核心原则：

- **简洁性**：构建适当的系统，而非过度复杂的系统
- **透明性**：使代理规划步骤明确可见
- **良好设计的工具**：为代理和系统之间提供清晰、文档完善的接口
- **标准化通信**：使用MCP（模型控制协议）确保一致的代理交互
- **模块化**：适配器系统用于连接各种LLM提供商
- **企业级安全**：基础设施设计确保业务数据在您的控制之下

## 系统架构

![系统架构图](images/archi.png)

架构包括：

1. **用户界面层**：直观的UI，具备身份验证与授权功能
2. **MCP宿主**：核心组件，包含权限管理、交互层、MCP客户端和配置系统
3. **LLM引擎**：处理语言处理任务
4. **企业服务**：文档、数据库、API及其他企业服务集成

**数据流**：  
用户 → 用户界面 → MCP宿主 → MCP客户端 → 企业服务（通过HTTP）  
LLM通过MCP宿主调用执行。

## 快速开始

### 前置要求

- Java开发工具包 (JDK) 21+
- Maven（通过Maven Wrapper提供）
- Node.js和`npx`（MCP服务器所需）
- Python 3.13.x和uv包管理器（MCP服务器所需）
- 配置环境变量的API密钥：

```shell
export OPENAI_API_KEY='your-openai-api-key'
export ANTHROPIC_API_KEY='your-anthropic-api-key'
export BRAVE_API_KEY="your-brave-api-key"
```

### 快速命令

```bash
# 运行应用
./mvnw spring-boot:run

# 构建可执行JAR
./mvnw clean package

# 运行测试
./mvnw test
```

## 原生构建

深知支持为Linux、macOS和Windows生成原生可执行文件。

```bash
# Linux/macOS
chmod +x build-native.sh
./build-native.sh

# Windows
build-native.bat
```

所有平台的原生可执行文件也可在GitHub Releases中获取。

## 已知问题

- 流式模式尚不支持（由于[Spring问题#2341](https://github.com/spring-projects/spring-ai/issues/2341)）

## 安全与隐私

- **数据主权**：业务数据保持在您的基础设施内
- **私有部署**：支持在私有VPC中托管本地LLM
- **零数据泄漏**：使用本地模型时无需外部数据传输

## 许可证

本项目采用GNU Affero通用公共许可证v3.0（AGPL-3.0）授权。详见LICENSE文件或访问：https://www.gnu.org/licenses/agpl-3.0.en.html
