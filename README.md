# JDC Pre-Commit Code Review - IntelliJ IDEA Plugin

A professional IntelliJ IDEA plugin that automatically performs AI-driven code reviews before Git commits, helping developers improve code quality.

🎉 **Now Available on JetBrains Plugin Marketplace!**

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![JetBrains Plugin](https://img.shields.io/badge/JetBrains-Plugin-orange.svg)](https://plugins.jetbrains.com/)
[![Java](https://img.shields.io/badge/Java-8%2B-red.svg)](https://www.oracle.com/java/)

## ✨ Core Features

### 🔍 **Smart Code Review**
- Multi-dimensional code quality analysis powered by Claude Sonnet 4
- Supports multiple programming languages (Java, Kotlin, Python, JavaScript, TypeScript, etc.)
- Provides 0-100 code quality scores with detailed improvement suggestions
- Identifies potential security vulnerabilities, performance issues, and code style problems

### 🎯 **Selective File Review**
- Supports selective review with customizable file selection
- Checkbox interface with select all/none operations
- Intelligently filters non-code files, only reviews actual code files

### 🔗 **Deep Git Integration**
- Automatically detects Git staged files
- Analyzes file changes and diff comparisons
- Only reviews code files in the staging area

### 🛡️ **Secure OAuth Authentication**
- Browser-based secure login, no need to enter passwords in the plugin
- Supports automatic re-authentication, auto-refresh when login status expires
- Smart error handling and retry mechanisms

### 🎨 **Modern Interface**
- Beautiful interface compliant with IntelliJ design guidelines
- Real-time display of analysis results
- Professional file selection dialog

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

### 方法二：从插件市场安装（推荐）
1. 打开IntelliJ IDEA
2. 进入 `File` → `Settings` → `Plugins`
3. 搜索 "JDC Pre-Commit Code Review"
4. 点击安装并重启IDE

**或者直接访问**: [JetBrains Plugin Marketplace](https://plugins.jetbrains.com/plugin/search?search=JDC%20Pre-Commit%20Code%20Review)

## 使用方法

### 1. First Login
- Click the "Login" button in the tool window, browser will automatically open the authorization page
- Complete secure login in the browser without entering passwords in the plugin
- Authentication credentials are securely saved for automatic login next time

### 2. Review Staged Files
- Use `git add` to add files to the staging area
- In the plugin panel, check the files you want to review
- Click "Review Selected Files" button to start AI code review
- Or use keyboard shortcut `Ctrl+Shift+R`

### 3. Review Current File
- Right-click on a file and select `Tools → JDC Pre-Commit Review → Review Current File`
- Or use keyboard shortcut `Ctrl+Shift+F`
- Analysis results will be displayed in the tool window

### 4. Open Tool Window
- Use keyboard shortcut `Ctrl+Shift+P` to open the review tool window
- Or find "JDC Pre-Commit Review" in the right-side tool windows

## Keyboard Shortcuts

| Function | Windows/Linux | macOS |
|----------|---------------|-------|
| Review Staged Files | `Ctrl+Shift+R` | `Cmd+Shift+R` |
| Review Current File | `Ctrl+Shift+F` | `Cmd+Shift+F` |
| Open Tool Window | `Ctrl+Shift+P` | `Cmd+Shift+P` |

## Configuration

### Backend Server
Default connection to `https://www.jdctools.com.cn/api`. Configuration can be modified in settings if needed.

### AI Model
Plugin uses `claude-sonnet-4-20250514` model by default, which can be adjusted as needed.

## Development Guide

### Project Structure
```
intellij-plugin/
├── build.gradle                              # Gradle build file
├── gradle/                                   # Gradle Wrapper files
├── gradlew                                   # Gradle Wrapper script (Unix)
├── gradlew.bat                              # Gradle Wrapper script (Windows)
├── src/main/
│   ├── java/com/jdc/tools/precommit/
│   │   ├── action/                           # Action classes (menu items and shortcuts)
│   │   │   ├── ReviewStagedFilesAction.java
│   │   │   ├── ReviewCurrentFileAction.java
│   │   │   ├── OpenToolWindowAction.java
│   │   │   └── SettingsAction.java
│   │   ├── service/                          # Service layer
│   │   │   ├── ApiClient.java               # HTTP client
│   │   │   ├── AuthService.java             # Authentication service
│   │   │   ├── GitAnalysisService.java      # Git analysis service
│   │   │   └── PreCommitReviewService.java  # Pre-commit review service
│   │   ├── ui/                              # User interface components
│   │   │   ├── PreCommitReviewToolWindow.java
│   │   │   ├── FileSelectionDialog.java
│   │   │   └── panels/                      # UI panels
│   │   └── util/                            # Utility classes
│   │       └── FileTypeFilter.java          # File type filtering
│   └── resources/
│       └── META-INF/
│           └── plugin.xml                   # Plugin configuration file
└── build/                                   # Gradle build output directory
```

### Gradle Commands

#### Common Development Commands
```bash
# Clean project
./gradlew clean

# Compile project
./gradlew compileJava

# Run tests
./gradlew test

# Build plugin
./gradlew buildPlugin

# Run IDE for testing
./gradlew runIde

# Verify plugin
./gradlew verifyPlugin

# Publish to local
./gradlew publishToMavenLocal

# View all tasks
./gradlew tasks
```

#### Task Descriptions
- `clean`: Clean build directory
- `compileJava`: Compile Java source code
- `buildPlugin`: Build plugin ZIP package
- `runIde`: Start IDE instance with plugin
- `verifyPlugin`: Verify plugin compatibility

### Main Components
- `ApiClient`: HTTP client for backend communication with streaming support
- `AuthService`: Authentication service for managing login state and credentials
- `GitAnalysisService`: Git analysis service for staged file detection
- `PreCommitReviewService`: Pre-commit review service with core business logic
- `PreCommitReviewToolWindow`: Main UI component with login, results display, and file selection
- `FileTypeFilter`: Intelligent file type filtering for code files

### Extension Development
To add new features:
1. Create new classes in appropriate packages
2. Register new Actions or services in `plugin.xml`
3. Update UI components to support new functionality
4. Add necessary dependencies in `build.gradle`

### Dependency Management
Project uses Gradle for dependency management, main dependencies include:
- IntelliJ Platform SDK (automatically provided by intellij plugin)
- Jackson (JSON processing)
- Apache HttpClient (HTTP communication)
- JUnit 5 (testing framework)

#### Adding New Dependencies
Add to the `dependencies` block in `build.gradle`:
```gradle
dependencies {
    implementation 'group:artifact:version'
    testImplementation 'test-group:test-artifact:version'
}
```

## Troubleshooting

### Common Issues

**Q: Plugin cannot connect to server**
A: Check network connection and server address configuration, ensure access to `https://www.jdctools.com.cn`

**Q: Login failed**
A: Confirm browser authorization is completed, check if account is activated

**Q: Analysis results display abnormally**
A: Check if code files are empty, ensure network is stable

**Q: Keyboard shortcuts not working**
A: Check for conflicts with other plugin shortcuts, can be reconfigured in settings

**Q: No files to review**
A: Use `git add` to stage files first, plugin only reviews staged files

### Log Viewing
View detailed error logs in `Help` → `Show Log in Explorer/Finder`.

## Contributing

Welcome to submit Issues and Pull Requests to improve this plugin!

## License

This project is licensed under the Apache License 2.0.

## Contact Us

- Official Website: https://www.jdctools.com.cn
- Email: jdctools@163.com
- Technical Support: Please submit Issues on GitHub
- Plugin Marketplace: [JDC Pre-Commit Code Review](https://plugins.jetbrains.com/plugin/search?search=JDC%20Pre-Commit%20Code%20Review)
