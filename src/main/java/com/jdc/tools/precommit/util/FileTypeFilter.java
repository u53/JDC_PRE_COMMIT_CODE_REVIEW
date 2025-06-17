package com.jdc.tools.precommit.util;

import com.intellij.openapi.vfs.VirtualFile;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 文件类型过滤器
 * 只允许代码文件进行审查，过滤掉二进制文件、配置文件等
 */
public class FileTypeFilter {

    /**
     * 支持的代码文件扩展名
     */
    private static final Set<String> SUPPORTED_EXTENSIONS = new HashSet<>(Arrays.asList(
        // Java生态
        "java", "kt", "kts", "scala", "groovy", "clj", "cljs",
        
        // JavaScript/TypeScript生态
        "js", "jsx", "ts", "tsx", "vue", "svelte", "mjs", "cjs",
        
        // Python生态
        "py", "pyx", "pyi", "pyw",
        
        // Web前端
        "html", "htm", "css", "scss", "sass", "less", "styl",
        
        // C/C++生态
        "c", "cpp", "cxx", "cc", "c++", "h", "hpp", "hxx", "hh", "h++",
        
        // C#/.NET生态
        "cs", "vb", "fs", "fsx", "fsi",
        
        // Go语言
        "go",
        
        // Rust语言
        "rs",
        
        // PHP语言
        "php", "php3", "php4", "php5", "phtml",
        
        // Ruby语言
        "rb", "rbw", "rake", "gemspec",
        
        // Swift语言
        "swift",
        
        // Objective-C
        "m", "mm",
        
        // Shell脚本
        "sh", "bash", "zsh", "fish", "csh", "tcsh", "ksh",
        
        // PowerShell
        "ps1", "psm1", "psd1",
        
        // 批处理
        "bat", "cmd",
        
        // SQL数据库
        "sql", "mysql", "pgsql", "plsql",
        
        // 配置和数据文件（代码相关）
        "xml", "json", "yaml", "yml", "toml", "ini", "cfg", "conf",
        "properties", "env", "dotenv",
        
        // 文档和标记语言
        "md", "markdown", "rst", "txt", "adoc", "asciidoc",
        
        // 模板文件
        "jsp", "jspx", "ftl", "vm", "tpl", "mustache", "hbs", "handlebars",
        "erb", "ejs", "pug", "jade",
        
        // 函数式语言
        "hs", "lhs", "elm", "ml", "mli", "ocaml", "f90", "f95", "f03", "f08",
        
        // 其他编程语言
        "lua", "pl", "pm", "r", "R", "matlab", "m", "jl", "dart", "nim",
        "crystal", "cr", "zig", "v", "d", "pas", "pp", "dpr",
        
        // 脚本和自动化
        "awk", "sed", "vim", "vimrc",
        
        // 构建和配置文件
        "gradle", "maven", "pom", "sbt", "build", "make", "makefile", "cmake",
        "dockerfile", "docker-compose", "vagrantfile",
        
        // 移动开发
        "kt", "java", "swift", "dart", "xaml",
        
        // 游戏开发
        "cs", "js", "lua", "gd", "gdscript",
        
        // 数据科学
        "ipynb", "rmd", "qmd",
        
        // 其他常见代码文件
        "asm", "s", "S", "nasm", "masm",
        "lisp", "scm", "ss", "rkt",
        "tcl", "tk", "exp",
        "pro", "pri", "prf", "qbs", "qml",
        "feature", "story", "spec"
    ));

    /**
     * 需要排除的文件扩展名（二进制文件、编译产物等）
     */
    private static final Set<String> EXCLUDED_EXTENSIONS = new HashSet<>(Arrays.asList(
        // 二进制文件
        "bin", "exe", "dll", "so", "dylib", "a", "lib", "obj", "o",
        
        // 压缩文件
        "zip", "rar", "7z", "tar", "gz", "bz2", "xz", "lzma",
        
        // 图片文件
        "jpg", "jpeg", "png", "gif", "bmp", "svg", "ico", "webp", "tiff", "tif",
        
        // 音视频文件
        "mp3", "mp4", "avi", "mov", "wmv", "flv", "mkv", "wav", "ogg", "flac",
        
        // 文档文件
        "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "odt", "ods", "odp",
        
        // 字体文件
        "ttf", "otf", "woff", "woff2", "eot",
        
        // 锁文件和缓存
        "lock", "cache", "tmp", "temp", "bak", "backup", "swp", "swo",
        
        // 编译产物
        "class", "jar", "war", "ear", "pyc", "pyo", "pyd", "__pycache__",
        
        // IDE文件
        "iml", "ipr", "iws", "idea", "vscode", "sublime-project", "sublime-workspace",
        
        // 系统文件
        "DS_Store", "Thumbs.db", "desktop.ini",
        
        // 日志文件
        "log", "logs", "out", "err"
    ));

    /**
     * 需要排除的文件名（不区分扩展名）
     */
    private static final Set<String> EXCLUDED_FILENAMES = new HashSet<>(Arrays.asList(
        // 依赖管理
        "package-lock.json", "yarn.lock", "pnpm-lock.yaml", "composer.lock",
        "Pipfile.lock", "poetry.lock", "Cargo.lock", "go.sum",
        
        // 构建产物
        "node_modules", "target", "build", "dist", "out", ".gradle", ".mvn",
        
        // 版本控制
        ".gitignore", ".gitattributes", ".gitmodules", ".gitkeep",
        ".hgignore", ".svnignore",
        
        // IDE配置
        ".vscode", ".idea", ".eclipse", ".settings",
        
        // 其他配置
        ".env.local", ".env.production", ".env.development",
        "thumbs.db", ".ds_store"
    ));

    /**
     * 判断文件是否为可审查的代码文件
     */
    public static boolean isCodeFile(VirtualFile file) {
        if (file == null || file.isDirectory()) {
            return false;
        }

        String fileName = file.getName().toLowerCase();
        String extension = file.getExtension();
        
        // 检查是否在排除的文件名列表中
        if (EXCLUDED_FILENAMES.contains(fileName)) {
            return false;
        }
        
        // 检查扩展名
        if (extension != null) {
            extension = extension.toLowerCase();
            
            // 如果在排除列表中，直接排除
            if (EXCLUDED_EXTENSIONS.contains(extension)) {
                return false;
            }
            
            // 如果在支持列表中，允许
            if (SUPPORTED_EXTENSIONS.contains(extension)) {
                return true;
            }
        }
        
        // 没有扩展名的文件，检查是否为常见的代码文件
        if (extension == null || extension.isEmpty()) {
            return isCommonCodeFileWithoutExtension(fileName);
        }
        
        // 默认不支持未知扩展名的文件
        return false;
    }

    /**
     * 检查没有扩展名的文件是否为常见的代码文件
     */
    private static boolean isCommonCodeFileWithoutExtension(String fileName) {
        Set<String> commonCodeFiles = new HashSet<>(Arrays.asList(
            "makefile", "dockerfile", "vagrantfile", "rakefile", "gemfile",
            "podfile", "fastfile", "appfile", "deliverfile", "matchfile",
            "scanfile", "snapfile", "gymfile", "procfile", "buildfile"
        ));
        
        return commonCodeFiles.contains(fileName);
    }

    /**
     * 获取文件类型描述
     */
    public static String getFileTypeDescription(VirtualFile file) {
        if (file == null) {
            return "未知";
        }
        
        String extension = file.getExtension();
        if (extension == null || extension.isEmpty()) {
            return "无扩展名文件";
        }
        
        extension = extension.toLowerCase();
        
        // 根据扩展名返回文件类型描述
        switch (extension) {
            case "java": return "Java源码";
            case "kt": case "kts": return "Kotlin源码";
            case "js": case "jsx": return "JavaScript源码";
            case "ts": case "tsx": return "TypeScript源码";
            case "vue": return "Vue组件";
            case "py": return "Python源码";
            case "html": case "htm": return "HTML文件";
            case "css": case "scss": case "sass": case "less": return "样式文件";
            case "xml": return "XML文件";
            case "json": return "JSON文件";
            case "yaml": case "yml": return "YAML文件";
            case "md": case "markdown": return "Markdown文档";
            case "sql": return "SQL脚本";
            case "sh": case "bash": return "Shell脚本";
            case "go": return "Go源码";
            case "rs": return "Rust源码";
            case "php": return "PHP源码";
            case "rb": return "Ruby源码";
            case "cs": return "C#源码";
            case "cpp": case "cxx": case "cc": return "C++源码";
            case "c": return "C源码";
            case "h": case "hpp": return "头文件";
            default: return extension.toUpperCase() + "文件";
        }
    }
}
