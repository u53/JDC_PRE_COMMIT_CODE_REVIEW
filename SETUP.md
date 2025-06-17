# JDC Code Review Plugin 设置指南

## 🎯 项目概述

这是一个为IntelliJ IDEA开发的AI代码审查插件，提供智能代码分析、链式调用分析和交互式AI对话功能。

## 📋 完成的工作

### ✅ 后端扩展
1. **新增代码审查服务**
   - `CodeReviewService` 接口
   - `CodeReviewServiceImpl` 实现类
   - `CodeReviewController` 控制器

2. **新增数据模型**
   - `CodeReviewRequest` - 代码审查请求
   - `CodeReviewResponse` - 代码审查响应

3. **流式API支持**
   - SSE (Server-Sent Events) 流式输出
   - 交互式对话流
   - 实时分析结果推送

### ✅ IntelliJ IDEA 插件
1. **完整的插件架构**
   - 基于Gradle构建系统
   - Java 8兼容性
   - IntelliJ Platform SDK集成

2. **核心功能组件**
   - `ApiClient` - HTTP客户端和SSE支持
   - `AuthService` - 认证服务
   - `CodeReviewService` - 代码审查服务
   - `CodeReviewToolWindow` - 主UI界面

3. **用户交互功能**
   - 右键菜单集成
   - 快捷键支持
   - 工具窗口界面
   - 登录认证

4. **代码分析功能**
   - 单文件分析
   - 多文件分析
   - 选中代码分析
   - 链式调用分析

## 🔧 技术栈

### 后端
- Spring Boot
- Java 8
- MyBatis-Plus
- MySQL + Redis

### 插件
- IntelliJ Platform SDK
- Java 8
- Gradle 8.4
- Jackson (JSON处理)
- Apache HttpClient

## 🚀 快速开始

### 1. 安装要求
- IntelliJ IDEA 2023.1+
- JDK 8+
- Gradle 8.0+ (可选，有Gradle Wrapper)

### 2. 构建插件
```bash
cd intellij-plugin

# 使用Gradle Wrapper (推荐)
./gradlew clean buildPlugin

# 或使用构建脚本
./build.sh
```

### 3. 安装插件
1. 构建完成后，在 `build/distributions/` 目录找到插件ZIP文件
2. 在IntelliJ IDEA中：`File` → `Settings` → `Plugins` → `Install Plugin from Disk`
3. 选择ZIP文件并重启IDE

### 4. 使用插件
1. 右键点击Java文件 → `JDC Code Review` → `分析当前文件`
2. 或使用快捷键：`Ctrl+Shift+A`
3. 首次使用需要登录JDC Tools账号

## 📁 项目结构

```
intellij-plugin/
├── build.gradle                 # Gradle构建配置
├── src/main/
│   ├── java/com/jdc/tools/codereview/
│   │   ├── action/              # 菜单动作
│   │   ├── model/               # 数据模型
│   │   ├── service/             # 服务层
│   │   └── ui/                  # 用户界面
│   └── resources/META-INF/
│       └── plugin.xml           # 插件配置
└── build/                       # 构建输出
```

## 🔑 主要功能

### 代码审查
- **智能分析**: AI驱动的代码质量评估
- **多维度检查**: 安全性、性能、风格、逻辑
- **问题定位**: 精确到行号的问题标识
- **修复建议**: 具体的代码改进建议

### 链式分析
- **调用链追踪**: 自动分析方法调用关系
- **依赖关系**: 识别类和模块间的依赖
- **循环依赖检测**: 发现潜在的架构问题
- **关键路径分析**: 识别重要的执行路径

### 交互式对话
- **AI问答**: 针对代码的智能问答
- **上下文理解**: 基于当前代码的对话
- **流式输出**: 实时显示AI响应
- **会话管理**: 支持多轮对话

## 🛠️ 开发指南

### 添加新功能
1. 在相应包中创建新类
2. 在 `plugin.xml` 中注册Action或服务
3. 更新UI组件
4. 在 `build.gradle` 中添加依赖

### 调试插件
```bash
# 启动开发环境
./gradlew runIde

# 这会启动一个带有插件的IntelliJ IDEA实例
```

### 常用Gradle任务
- `./gradlew clean` - 清理构建
- `./gradlew compileJava` - 编译代码
- `./gradlew buildPlugin` - 构建插件
- `./gradlew verifyPlugin` - 验证插件
- `./gradlew runIde` - 运行开发环境

## 🔗 API集成

插件通过以下API与后端通信：
- `POST /code-review/analyze` - 代码审查
- `POST /code-review/analyze-stream` - 流式代码审查
- `POST /code-review/chat-stream` - 交互式对话
- `POST /code-review/chain-analysis` - 链式分析
- `POST /auth/login` - 用户登录

## 📝 注意事项

1. **Java 8兼容性**: 所有代码都兼容Java 8
2. **认证集成**: 使用现有的JDC Tools认证系统
3. **流式输出**: 支持SSE实时数据传输
4. **错误处理**: 完善的异常处理和用户提示
5. **安全性**: 凭据安全存储和传输

## 🐛 故障排除

### 常见问题
1. **编译错误**: 确保使用Java 8兼容语法
2. **依赖问题**: 检查网络连接和仓库配置
3. **插件加载失败**: 验证plugin.xml配置
4. **认证失败**: 检查后端服务可用性

### 获取帮助
- 查看构建日志：`./gradlew buildPlugin --info`
- 查看IDE日志：`Help` → `Show Log in Explorer`
- 检查网络连接到 `https://www.jdctools.com.cn`

## 🎉 下一步

1. 安装Gradle (可选)
2. 构建并测试插件
3. 根据需要调整配置
4. 部署到生产环境

插件现在已经准备好进行构建和测试了！
