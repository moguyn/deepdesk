# 深知

## 概述
深知是一个用于创建和管理企业级AI代理的平台。它使组织能够利用AI功能，同时保持对其数据和基础设施的完全控制。

## 目的
- 创建和管理企业特定用例的AI代理
- 提供与AI代理和企业私有数据源交互的聊天界面
- 使企业能够创建自定义AI代理，无需复杂的多数据源集成
- 维护数据隐私和安全性

## 主要特性
- AI代理交互的聊天界面
- 与企业私有数据源集成
- 行业标准MCP协议通信
- 灵活的模型集成适配器
- 支持在企业VPC中部署本地开源LLM
- 无供应商锁定

## 架构亮点
- 使用MCP（模型控制协议）进行标准化代理通信
- 用于连接各种LLM提供商的模块化适配器系统
- 安全的基础设施设计，确保业务数据安全

## 原生构建

深知支持生成可在Linux、macOS和Windows上不需要JVM即可运行的原生可执行文件。

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

生成的原生可执行文件将创建在`target`目录中，名称为：`deepdesk`

### 自动化多平台构建

所有支持平台的原生可执行文件都使用GitHub Actions自动构建。最新的构建可以在本仓库的GitHub Releases部分找到。

### 原生构建的要求

要在本地构建原生可执行文件：
- GraalVM安装，含native-image工具
- Java 17或更新版本（推荐Java 23）
- Maven 3.8+

## 系统架构
![系统架构图](images/archi.png)
该图展示了一个**企业应用程序**（"MCP宿主"）协调以下组件：
1. **用户**  
   - 通过**用户界面**访问系统。  
   - 经过**用户认证与授权**以确保适当的权限。

2. **MCP宿主**  
   - **权限**：管理基于角色的访问。  
   - **模型与资源交互**：处理LLM和企业资源之间的请求。  
   - **MCP客户端**：通过HTTP与后端MCP服务通信。  
   - **配置**：存储和管理应用设置。

3. **LLM**  
   - 根据需要执行语言处理和生成任务。

4. **企业服务**  
   - **文档服务**：处理PDF、Word、TXT等。  
   - **数据库服务**：连接MySQL、PostgreSQL等。  
   - **API服务**：提供内部企业API。  
   - **其他服务**：额外的企业功能。

**数据流**：  
用户 → 用户界面 → MCP宿主（检查权限）→ MCP客户端 → 企业服务（通过HTTP）。  
LLM也通过MCP宿主调用以执行语言模型任务。

## 快速开始
### 前置要求
- Java开发工具包 (JDK) 23或更高版本
- Maven（通过Maven Wrapper提供）
- Node.js和`npx`（MCP服务器所需）
- Python 3.13.x和uv包管理器（（MCP服务器所需））
- 设置以下环境变量
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
使用SpringBoot构建可执行JAR文件：
```bash
./mvnw clean package
```
生成的可执行JAR文件将创建在`target`目录中，名称为`deepdesk-<version>.jar`。

运行可执行JAR：
```bash
java -jar target/deepdesk-<version>.jar
```

### 运行测试
运行测试套件：
```bash
./mvnw test
```

## 安全与隐私
深知在设计时考虑了企业安全性：
- 业务数据保持在企业基础设施内
- 支持在私有VPC中托管本地LLM
- 使用本地模型时无需外部数据传输

## 许可证
本项目采用GNU Affero通用公共许可证v3.0（AGPL-3.0）授权。该许可证确保：
- 软件可以自由使用、修改和分发
- 对软件的任何修改都必须以相同的许可证提供
- 网络使用被视为分发，要求向通过网络与软件交互的用户提供源代码
- 完整许可证文本可在LICENSE文件中找到

更多信息，请访问：https://www.gnu.org/licenses/agpl-3.0.en.html
