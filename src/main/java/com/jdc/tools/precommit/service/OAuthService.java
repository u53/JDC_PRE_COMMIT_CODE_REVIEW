package com.jdc.tools.precommit.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * OAuth授权服务
 * 实现通过浏览器打开门户网站进行授权登录
 */
@Service
public final class OAuthService {
    
    private static final Logger LOG = Logger.getInstance(OAuthService.class);
    private static final String PORTAL_URL = "https://www.jdctools.com.cn";
    private static final String AUTH_ENDPOINT = "/plugin-auth";
    private static final int CALLBACK_PORT = 18888; // 本地回调端口
    private static final int AUTH_TIMEOUT_SECONDS = 300; // 5分钟超时
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    private ServerSocket callbackServer;
    private volatile boolean isAuthInProgress = false;
    
    public static OAuthService getInstance() {
        return ApplicationManager.getApplication().getService(OAuthService.class);
    }
    
    /**
     * 启动OAuth授权流程
     * @return 授权结果的CompletableFuture
     */
    public CompletableFuture<AuthResult> startOAuthFlow() {
        if (isAuthInProgress) {
            return CompletableFuture.completedFuture(
                new AuthResult(false, "授权正在进行中，请稍候", null, null)
            );
        }
        
        CompletableFuture<AuthResult> future = new CompletableFuture<>();
        
        // 在后台任务中执行授权流程
        ProgressManager.getInstance().run(new Task.Backgroundable(null, "JDC Tools 授权登录", true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                try {
                    isAuthInProgress = true;
                    indicator.setText("正在启动授权流程...");
                    
                    // 启动本地回调服务器
                    if (!startCallbackServer()) {
                        future.complete(new AuthResult(false, "无法启动本地回调服务器", null, null));
                        return;
                    }
                    
                    // 构建授权URL
                    String authUrl = buildAuthUrl();
                    LOG.info("打开授权URL: " + authUrl);
                    
                    indicator.setText("正在打开浏览器进行授权...");
                    
                    // 在EDT中打开浏览器
                    ApplicationManager.getApplication().invokeLater(() -> {
                        BrowserUtil.browse(authUrl);
                        
                        // 显示用户提示
                        ApplicationManager.getApplication().executeOnPooledThread(() -> {
                            int result = Messages.showYesNoDialog(
                                "已在浏览器中打开JDC Tools授权页面。\n\n" +
                                "请在浏览器中完成登录授权，授权完成后会自动返回插件。\n\n" +
                                "如果浏览器没有自动打开，请手动访问：\n" + authUrl + "\n\n" +
                                "点击「是」继续等待授权，点击「否」取消授权。",
                                "JDC Tools 授权登录",
                                "继续等待",
                                "取消授权",
                                Messages.getInformationIcon()
                            );
                            
                            if (result != Messages.YES) {
                                future.complete(new AuthResult(false, "用户取消授权", null, null));
                                stopCallbackServer();
                                return;
                            }
                        });
                    });
                    
                    indicator.setText("等待授权回调...");
                    
                    // 等待授权回调
                    AuthResult authResult = waitForCallback(indicator);
                    future.complete(authResult);
                    
                } catch (Exception e) {
                    LOG.error("OAuth授权流程异常", e);
                    future.complete(new AuthResult(false, "授权流程异常: " + e.getMessage(), null, null));
                } finally {
                    isAuthInProgress = false;
                    stopCallbackServer();
                }
            }
        });
        
        return future;
    }
    
    /**
     * 构建授权URL
     */
    private String buildAuthUrl() {
        return String.format("%s%s?callback_port=%d&plugin_version=1.0.0&client_type=intellij_plugin&auto_auth=true",
            PORTAL_URL, AUTH_ENDPOINT, CALLBACK_PORT);
    }
    
    /**
     * 启动本地回调服务器
     */
    private boolean startCallbackServer() {
        try {
            callbackServer = new ServerSocket(CALLBACK_PORT);
            LOG.info("本地回调服务器启动成功，端口: " + CALLBACK_PORT);
            return true;
        } catch (IOException e) {
            LOG.error("启动本地回调服务器失败", e);
            return false;
        }
    }
    
    /**
     * 停止本地回调服务器
     */
    private void stopCallbackServer() {
        if (callbackServer != null && !callbackServer.isClosed()) {
            try {
                callbackServer.close();
                LOG.info("本地回调服务器已关闭");
            } catch (IOException e) {
                LOG.warn("关闭本地回调服务器失败", e);
            }
        }
    }
    
    /**
     * 等待授权回调
     */
    private AuthResult waitForCallback(ProgressIndicator indicator) {
        try {
            callbackServer.setSoTimeout(AUTH_TIMEOUT_SECONDS * 1000);
            
            while (!indicator.isCanceled()) {
                try {
                    indicator.setText("等待授权回调... (超时时间: " + AUTH_TIMEOUT_SECONDS + "秒)");
                    
                    Socket clientSocket = callbackServer.accept();
                    LOG.info("收到回调连接");
                    
                    // 读取HTTP请求
                    Scanner scanner = new Scanner(clientSocket.getInputStream());
                    String requestLine = scanner.nextLine();
                    LOG.info("收到请求: " + requestLine);
                    
                    // 解析请求参数
                    AuthResult result = parseCallbackRequest(requestLine);
                    
                    // 发送响应
                    sendCallbackResponse(clientSocket, result.isSuccess());
                    
                    clientSocket.close();
                    return result;
                    
                } catch (java.net.SocketTimeoutException e) {
                    return new AuthResult(false, "授权超时，请重试", null, null);
                }
            }
            
            return new AuthResult(false, "用户取消授权", null, null);
            
        } catch (Exception e) {
            LOG.error("等待授权回调失败", e);
            return new AuthResult(false, "等待授权回调失败: " + e.getMessage(), null, null);
        }
    }
    
    /**
     * 解析回调请求参数
     */
    private AuthResult parseCallbackRequest(String requestLine) {
        try {
            // 解析URL参数
            String[] parts = requestLine.split(" ");
            if (parts.length < 2) {
                return new AuthResult(false, "无效的回调请求", null, null);
            }

            String path = parts[1];

            // 处理根路径的情况
            String queryString = "";
            if (path.contains("?")) {
                queryString = path.substring(path.indexOf("?") + 1);
            } else if (path.equals("/")) {
                // 根路径但没有参数，可能是简单的健康检查
                return new AuthResult(false, "回调请求缺少参数", null, null);
            }

            if (queryString.isEmpty()) {
                return new AuthResult(false, "回调请求缺少参数", null, null);
            }

            Map<String, String> params = parseQueryString(queryString);
            
            String success = params.get("success");
            String token = params.get("token");
            String userInfo = params.get("user_info");
            String error = params.get("error");
            
            if ("true".equals(success) && token != null && !token.trim().isEmpty()) {
                // 解码用户信息
                Map<String, Object> userInfoMap = null;
                if (userInfo != null && !userInfo.trim().isEmpty()) {
                    try {
                        String decodedUserInfo = URLDecoder.decode(userInfo, StandardCharsets.UTF_8);
                        userInfoMap = objectMapper.readValue(decodedUserInfo, Map.class);
                    } catch (Exception e) {
                        LOG.warn("解析用户信息失败", e);
                    }
                }
                
                return new AuthResult(true, "授权成功", token, userInfoMap);
            } else {
                String errorMsg = error != null ? URLDecoder.decode(error, StandardCharsets.UTF_8) : "授权失败";
                return new AuthResult(false, errorMsg, null, null);
            }
            
        } catch (Exception e) {
            LOG.error("解析回调请求失败", e);
            return new AuthResult(false, "解析回调请求失败: " + e.getMessage(), null, null);
        }
    }
    
    /**
     * 解析查询字符串
     */
    private Map<String, String> parseQueryString(String queryString) {
        Map<String, String> params = new HashMap<>();
        String[] pairs = queryString.split("&");
        
        for (String pair : pairs) {
            String[] keyValue = pair.split("=", 2);
            if (keyValue.length == 2) {
                try {
                    String key = URLDecoder.decode(keyValue[0], StandardCharsets.UTF_8);
                    String value = URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8);
                    params.put(key, value);
                } catch (Exception e) {
                    LOG.warn("解析参数失败: " + pair, e);
                }
            }
        }
        
        return params;
    }
    
    /**
     * 发送回调响应
     */
    private void sendCallbackResponse(Socket clientSocket, boolean success) {
        try {
            String responseBody = success ? 
                "<html><body><h2>✅ 授权成功</h2><p>您已成功授权JDC Tools插件，可以关闭此页面。</p></body></html>" :
                "<html><body><h2>❌ 授权失败</h2><p>授权失败，请重试。</p></body></html>";
            
            String response = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: text/html; charset=UTF-8\r\n" +
                "Content-Length: " + responseBody.getBytes(StandardCharsets.UTF_8).length + "\r\n" +
                "Connection: close\r\n" +
                "\r\n" +
                responseBody;
            
            clientSocket.getOutputStream().write(response.getBytes(StandardCharsets.UTF_8));
            clientSocket.getOutputStream().flush();
            
        } catch (IOException e) {
            LOG.warn("发送回调响应失败", e);
        }
    }
    
    /**
     * 授权结果类
     */
    public static class AuthResult {
        private final boolean success;
        private final String message;
        private final String token;
        private final Map<String, Object> userInfo;
        
        public AuthResult(boolean success, String message, String token, Map<String, Object> userInfo) {
            this.success = success;
            this.message = message;
            this.token = token;
            this.userInfo = userInfo;
        }
        
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public String getToken() { return token; }
        public Map<String, Object> getUserInfo() { return userInfo; }
    }
}
