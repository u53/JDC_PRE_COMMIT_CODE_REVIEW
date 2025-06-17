#!/bin/bash

# JDC Code Review Plugin Build Script (Gradle版本)

set -e

echo "🚀 开始构建 JDC Code Review Plugin..."

# 检查Java版本
if ! command -v java &> /dev/null; then
    echo "❌ 错误: 未找到Java，请安装JDK 8或更高版本"
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1-2)
echo "✅ 检测到Java版本: $JAVA_VERSION"

# 检查Gradle
if command -v gradle &> /dev/null; then
    GRADLE_CMD="gradle"
    GRADLE_VERSION=$(gradle -version | grep "Gradle" | cut -d' ' -f2)
    echo "✅ 检测到Gradle版本: $GRADLE_VERSION"
elif [ -f "./gradlew" ]; then
    GRADLE_CMD="./gradlew"
    echo "✅ 使用Gradle Wrapper"
else
    echo "❌ 错误: 未找到Gradle，请安装Gradle或确保gradlew可执行"
    echo "   可以从 https://gradle.org/install/ 下载Gradle"
    exit 1
fi

# 清理之前的构建
echo "🧹 清理之前的构建..."
$GRADLE_CMD clean

# 编译项目
echo "🔨 编译项目..."
$GRADLE_CMD compileJava

# 运行测试
echo "🧪 运行测试..."
$GRADLE_CMD test

# 构建插件
echo "📦 构建插件包..."
$GRADLE_CMD buildPlugin

# 验证插件
echo "✅ 验证插件..."
$GRADLE_CMD verifyPlugin

echo ""
echo "🎉 构建完成！"
echo ""
echo "📁 插件文件位置:"
find build/distributions -name "*.zip" -type f 2>/dev/null || echo "   build/distributions/ 目录下的 .zip 文件"

echo ""
echo "🔧 其他有用的命令:"
echo "   运行开发环境: $GRADLE_CMD runIde"
echo "   只构建不测试: $GRADLE_CMD buildPlugin -x test"
echo "   发布到本地:   $GRADLE_CMD publishToMavenLocal"
echo ""
echo "📖 更多信息请查看 README.md"
