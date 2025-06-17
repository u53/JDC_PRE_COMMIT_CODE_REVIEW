# JDC Code Review Assistant - IntelliJ IDEA 插件

一个强大的IntelliJ IDEA插件，提供AI驱动的代码审查和分析功能。

## 功能特性

### 🔍 **智能代码审查**
- 使用AI技术对代码进行全面分析
- 支持多种编程语言（Java、Kotlin、Python、JavaScript等）
- 提供代码质量评分和详细分析报告
- 识别潜在的安全漏洞、性能问题和代码风格问题

### 🔗 **链式代码分析**
- 自动追踪方法调用链和依赖关系
- 分析代码架构和模块间的耦合度
- 识别循环依赖和关键路径
- 提供架构优化建议

### 💬 **交互式AI对话**
- 支持与AI进行代码相关的问答
- 可以针对特定代码片段进行深入讨论
- 获得个性化的编程建议和最佳实践指导

### ⚡ **流式输出**
- 实时显示AI分析结果
- 提升用户体验，无需等待完整分析完成
- 支持中断和重新开始分析

### 🔐 **安全认证**
- 集成JDC Tools账号系统
- 安全的令牌认证机制
- 自动保存登录状态

## 安装要求

- IntelliJ IDEA 2023.1 或更高版本
- JDK 8 或更高版本（兼容JDK 8-21）
- Gradle 8.0+ （推荐）或使用包含的Gradle Wrapper
- 有效的JDC Tools账号

## 构建插件

### 1. 克隆项目
```bash
git clone <repository-url>
cd intellij-plugin
```

### 2. 构建插件
```bash
# 使用系统Gradle（如果已安装）
gradle clean buildPlugin

# 或使用Gradle Wrapper（推荐）
./gradlew clean buildPlugin

# 或使用构建脚本
./build.sh
```

构建完成后，插件文件将生成在 `build/distributions/` 目录下。

### 3. 运行开发环境
```bash
# 启动带有插件的IntelliJ IDEA实例进行测试
./gradlew runIde

# 或使用系统Gradle
gradle runIde
```

### 4. 验证插件
```bash
# 验证插件配置
./gradlew verifyPlugin
```

## 安装插件

### 方法一：从文件安装
1. 打开IntelliJ IDEA
2. 进入 `File` → `Settings` → `Plugins`
3. 点击齿轮图标，选择 `Install Plugin from Disk...`
4. 选择构建生成的插件文件（.zip格式）
5. 重启IDE

### 方法二：从插件市场安装（待发布）
1. 打开IntelliJ IDEA
2. 进入 `File` → `Settings` → `Plugins`
3. 搜索 "JDC Code Review Assistant"
4. 点击安装并重启IDE

## 使用方法

### 1. 登录账号
- 首次使用时，插件会提示登录JDC Tools账号
- 在工具窗口的登录页面输入用户名和密码
- 登录成功后，凭据会安全保存，下次自动登录

### 2. 分析单个文件
- 右键点击文件，选择 `JDC Code Review` → `分析当前文件`
- 或使用快捷键 `Ctrl+Shift+A`
- 分析结果将显示在工具窗口中

### 3. 分析选中代码
- 在编辑器中选中要分析的代码
- 右键选择 `JDC Code Review` → `分析选中代码`
- 或使用快捷键 `Ctrl+Shift+S`

### 4. 链式代码分析
- 右键点击Java/Kotlin文件，选择 `JDC Code Review` → `链式分析`
- 或使用快捷键 `Ctrl+Shift+C`
- 插件会自动分析调用链和依赖关系

### 5. AI对话
- 点击工具窗口的"AI对话"标签页
- 输入问题并发送，与AI进行交互式对话
- 或使用快捷键 `Ctrl+Shift+Q` 快速打开对话

## 快捷键

| 功能 | Windows/Linux | macOS |
|------|---------------|-------|
| 分析当前文件 | `Ctrl+Shift+A` | `Cmd+Shift+A` |
| 分析选中代码 | `Ctrl+Shift+S` | `Cmd+Shift+S` |
| 链式分析 | `Ctrl+Shift+C` | `Cmd+Shift+C` |
| 打开AI对话 | `Ctrl+Shift+Q` | `Cmd+Shift+Q` |

## 配置

### 后端服务器地址
默认连接到 `https://www.jdctools.com.cn/api`，如需修改可在设置中配置。

### AI模型选择
插件默认使用 `claude-sonnet-4-20250514` 模型，可根据需要调整。

## 开发说明

### 项目结构
```
intellij-plugin/
├── build.gradle                              # Gradle构建文件
├── gradle/                                   # Gradle Wrapper文件
├── gradlew                                   # Gradle Wrapper脚本 (Unix)
├── gradlew.bat                              # Gradle Wrapper脚本 (Windows)
├── build.sh                                 # 构建脚本
├── src/main/
│   ├── java/com/jdc/tools/codereview/
│   │   ├── action/                           # Action类（菜单项和快捷键）
│   │   │   ├── AnalyzeFileAction.java
│   │   │   ├── AnalyzeSelectionAction.java
│   │   │   ├── ChainAnalysisAction.java
│   │   │   ├── OpenChatAction.java
│   │   │   └── SettingsAction.java
│   │   ├── model/                            # 数据模型
│   │   │   ├── CodeReviewRequest.java
│   │   │   └── CodeReviewResponse.java
│   │   ├── service/                          # 服务层
│   │   │   ├── ApiClient.java               # HTTP客户端
│   │   │   ├── AuthService.java             # 认证服务
│   │   │   └── CodeReviewService.java       # 代码审查服务
│   │   ├── ui/                              # 用户界面组件
│   │   │   ├── CodeReviewToolWindow.java
│   │   │   └── CodeReviewToolWindowFactory.java
│   │   └── util/                            # 工具类
│   └── resources/
│       └── META-INF/
│           └── plugin.xml                   # 插件配置文件
└── build/                                   # Gradle构建输出目录
```

### Gradle命令说明

#### 开发常用命令
```bash
# 清理项目
./gradlew clean

# 编译项目
./gradlew compileJava

# 运行测试
./gradlew test

# 构建插件
./gradlew buildPlugin

# 运行IDE进行测试
./gradlew runIde

# 验证插件
./gradlew verifyPlugin

# 发布到本地
./gradlew publishToMavenLocal

# 查看所有任务
./gradlew tasks
```

#### 常用任务说明
- `clean`: 清理构建目录
- `compileJava`: 编译Java源代码
- `buildPlugin`: 构建插件ZIP包
- `runIde`: 启动带插件的IDE实例
- `verifyPlugin`: 验证插件兼容性

### 主要组件
- `ApiClient`: HTTP客户端，处理与后端的通信，支持SSE流式传输
- `AuthService`: 认证服务，管理用户登录状态和凭据保存
- `CodeReviewService`: 代码审查服务，核心业务逻辑
- `CodeReviewToolWindow`: 主要的UI组件，包含登录、结果显示、对话功能

### 扩展开发
如需添加新功能：
1. 在相应的包中创建新类
2. 在 `plugin.xml` 中注册新的Action或服务
3. 更新UI组件以支持新功能
4. 在 `build.gradle` 中添加必要的依赖

### 依赖管理
项目使用Gradle管理依赖，主要依赖包括：
- IntelliJ Platform SDK (由intellij插件自动提供)
- Jackson (JSON处理)
- Apache HttpClient (HTTP通信)
- JUnit 5 (测试框架)

#### 添加新依赖
在 `build.gradle` 的 `dependencies` 块中添加：
```gradle
dependencies {
    implementation 'group:artifact:version'
    testImplementation 'test-group:test-artifact:version'
}
```

## 故障排除

### 常见问题

**Q: 插件无法连接到服务器**
A: 检查网络连接和服务器地址配置，确保能访问 `https://www.jdctools.com.cn`

**Q: 登录失败**
A: 确认用户名和密码正确，检查账号是否已激活

**Q: 分析结果显示异常**
A: 检查代码文件是否为空，网络是否稳定

**Q: 快捷键不生效**
A: 检查是否与其他插件的快捷键冲突，可在设置中重新配置

### 日志查看
在 `Help` → `Show Log in Explorer/Finder` 中查看详细的错误日志。

## 贡献

欢迎提交Issue和Pull Request来改进这个插件！

## 许可证

本项目采用MIT许可证。

## 联系我们

- 官网：https://www.jdctools.com.cn
- 邮箱：support@jdctools.com
- 技术支持：请在GitHub上提交Issue
