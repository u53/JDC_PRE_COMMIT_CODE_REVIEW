package com.jdc.tools.precommit.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * API客户端服务
 * 负责与后端API的HTTP通信，支持普通请求和SSE流式传输
 */
@Service
public final class ApiClient {

    private static final Logger LOG = Logger.getInstance(ApiClient.class);
    private static final String BASE_URL = "https://www.jdctools.com.cn/api";
//private static final String BASE_URL = "http://localhost:8089/api";
    private static final int TIMEOUT = 180000; // 60秒超时

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final CloseableHttpClient httpClient;

    public ApiClient() {
        this.httpClient = HttpClients.createDefault();
    }

    public static ApiClient getInstance() {
        return ApplicationManager.getApplication().getService(ApiClient.class);
    }

    /**
     * 处理认证错误
     */
    private CompletableFuture<Boolean> handleAuthError(int statusCode, String responseBody) {
        if (statusCode == 401 || statusCode == 403) {
            LOG.warn("认证失败，状态码: " + statusCode + ", 响应: " + responseBody);

            AuthService authService = AuthService.getInstance();

            // 显示通知
            String message = statusCode == 401 ?
                "登录已过期，正在尝试自动重新登录..." :
                "访问被拒绝，正在尝试重新认证...";

            ApplicationManager.getApplication().invokeLater(() -> {
                Notification notification = new Notification(
                    "JDC Tools",
                    "认证失败",
                    message,
                    NotificationType.WARNING
                );
                Notifications.Bus.notify(notification);
            });

            // 尝试自动重新登录
            return authService.handleAuthFailure().thenApply(success -> {
                if (success) {
                    ApplicationManager.getApplication().invokeLater(() -> {
                        Notification successNotification = new Notification(
                            "JDC Tools",
                            "重新登录成功",
                            "已自动重新登录，请重试您的操作",
                            NotificationType.INFORMATION
                        );
                        Notifications.Bus.notify(successNotification);
                    });
                } else {
                    ApplicationManager.getApplication().invokeLater(() -> {
                        Notification failNotification = new Notification(
                            "JDC Tools",
                            "重新登录失败",
                            "自动重新登录失败，请手动重新登录",
                            NotificationType.ERROR
                        );
                        Notifications.Bus.notify(failNotification);
                    });
                }
                return success;
            });
        }

        return CompletableFuture.completedFuture(false);
    }

    /**
     * 检查响应并处理认证错误
     */
    private ApiResponse processResponse(int statusCode, String responseBody) {
        boolean success = statusCode >= 200 && statusCode < 300;

        // 处理认证错误
        if (statusCode == 401 || statusCode == 403) {
            // 异步处理认证错误，不阻塞当前请求
            handleAuthError(statusCode, responseBody);

            // 尝试解析错误消息
            String errorMessage = "认证失败";
            try {
                Map<String, Object> errorResponse = objectMapper.readValue(responseBody, Map.class);
                Object message = errorResponse.get("message");
                if (message != null) {
                    errorMessage = message.toString();
                }
            } catch (Exception e) {
                // 忽略解析错误，使用默认消息
            }

            return new ApiResponse(statusCode, errorMessage, false);
        }

        return new ApiResponse(statusCode, responseBody, success);
    }

    /**
     * GET请求
     */
    public CompletableFuture<ApiResponse> get(String endpoint) {
        return get(endpoint, null);
    }

    /**
     * GET请求（带认证）
     */
    public CompletableFuture<ApiResponse> get(String endpoint, String token) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String url = BASE_URL + endpoint;
                HttpGet request = new HttpGet(url);

                // 设置请求头
                request.setHeader("Content-Type", "application/json");
                request.setHeader("Accept", "application/json");

                if (token != null && !token.trim().isEmpty()) {
                    request.setHeader("Authorization", "Bearer " + token);
                }

                LOG.info("发送GET请求: " + url);

                HttpResponse response = httpClient.execute(request);
                String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                int statusCode = response.getStatusLine().getStatusCode();

                LOG.info("GET请求响应: " + statusCode + " - " + (responseBody.length() > 200 ?
                    responseBody.substring(0, 200) + "..." : responseBody));

                return processResponse(statusCode, responseBody);

            } catch (Exception e) {
                LOG.error("GET请求失败: " + endpoint, e);
                return new ApiResponse(500, "请求失败: " + e.getMessage(), false);
            }
        });
    }

    /**
     * POST请求
     */
    public CompletableFuture<ApiResponse> post(String endpoint, Object requestBody) {
        AuthService authService = AuthService.getInstance();
        String token = authService.getCurrentToken();
        return post(endpoint, requestBody, token);
    }

    /**
     * 带自动重试的POST请求
     */
    public CompletableFuture<ApiResponse> postWithRetry(String endpoint, Object requestBody) {
        return post(endpoint, requestBody).thenCompose(response -> {
            // 如果是认证错误，尝试重新登录后重试
            if (response.getStatusCode() == 401 || response.getStatusCode() == 403) {
                AuthService authService = AuthService.getInstance();
                return authService.handleAuthFailure().thenCompose(loginSuccess -> {
                    if (loginSuccess) {
                        LOG.info("重新登录成功，重试请求: " + endpoint);
                        // 重新获取token并重试请求
                        String newToken = authService.getCurrentToken();
                        return post(endpoint, requestBody, newToken);
                    } else {
                        LOG.warn("重新登录失败，返回原始错误响应");
                        return CompletableFuture.completedFuture(response);
                    }
                });
            }
            return CompletableFuture.completedFuture(response);
        });
    }

    /**
     * 带自动重试的GET请求
     */
    public CompletableFuture<ApiResponse> getWithRetry(String endpoint) {
        AuthService authService = AuthService.getInstance();
        String token = authService.getCurrentToken();
        return get(endpoint, token).thenCompose(response -> {
            // 如果是认证错误，尝试重新登录后重试
            if (response.getStatusCode() == 401 || response.getStatusCode() == 403) {
                return authService.handleAuthFailure().thenCompose(loginSuccess -> {
                    if (loginSuccess) {
                        LOG.info("重新登录成功，重试请求: " + endpoint);
                        // 重新获取token并重试请求
                        String newToken = authService.getCurrentToken();
                        return get(endpoint, newToken);
                    } else {
                        LOG.warn("重新登录失败，返回原始错误响应");
                        return CompletableFuture.completedFuture(response);
                    }
                });
            }
            return CompletableFuture.completedFuture(response);
        });
    }

    /**
     * POST请求（带认证）
     */
    public CompletableFuture<ApiResponse> post(String endpoint, Object requestBody, String token) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String url = BASE_URL + endpoint;
                HttpPost request = new HttpPost(url);

                // 设置请求头
                request.setHeader("Content-Type", "application/json");
                request.setHeader("Accept", "application/json");

                if (token != null && !token.trim().isEmpty()) {
                    request.setHeader("Authorization", "Bearer " + token);
                }

                // 设置请求体
                if (requestBody != null) {
                    String jsonBody = objectMapper.writeValueAsString(requestBody);
                    request.setEntity(new StringEntity(jsonBody, StandardCharsets.UTF_8));

                    LOG.info("发送POST请求: " + url + " - Body: " +
                        (jsonBody.length() > 500 ? jsonBody.substring(0, 500) + "..." : jsonBody));
                } else {
                    LOG.info("发送POST请求: " + url);
                }

                HttpResponse response = httpClient.execute(request);
                String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                int statusCode = response.getStatusLine().getStatusCode();

                LOG.info("POST请求响应: " + statusCode + " - " + (responseBody.length() > 200 ?
                    responseBody.substring(0, 200) + "..." : responseBody));

                return processResponse(statusCode, responseBody);

            } catch (Exception e) {
                LOG.error("POST请求失败: " + endpoint, e);
                return new ApiResponse(500, "请求失败: " + e.getMessage(), false);
            }
        });
    }

    /**
     * SSE流式POST请求
     */
    public void postStream(String endpoint, Object requestBody,
                          Consumer<String> onMessage, Runnable onComplete, Consumer<Exception> onError) {

        CompletableFuture.runAsync(() -> {
            try {
                AuthService authService = AuthService.getInstance();
                String token = authService.getCurrentToken();

                String url = BASE_URL + endpoint;
                HttpPost request = new HttpPost(url);

                // 设置请求头
                request.setHeader("Content-Type", "application/json");
                request.setHeader("Accept", "text/event-stream");
                request.setHeader("Cache-Control", "no-cache");

                if (token != null && !token.trim().isEmpty()) {
                    request.setHeader("Authorization", "Bearer " + token);
                }

                // 设置请求体
                if (requestBody != null) {
                    String jsonBody = objectMapper.writeValueAsString(requestBody);
                    request.setEntity(new StringEntity(jsonBody, StandardCharsets.UTF_8));
                }

                LOG.info("发送SSE流式请求: " + url);

                HttpResponse response = httpClient.execute(request);
                int statusCode = response.getStatusLine().getStatusCode();

                // 检查认证错误
                if (statusCode == 401 || statusCode == 403) {
                    String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                    handleAuthError(statusCode, responseBody);
                    onError.accept(new IOException("认证失败: " + statusCode));
                    return;
                }

                HttpEntity entity = response.getEntity();

                if (entity != null) {
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(entity.getContent(), StandardCharsets.UTF_8))) {

                        String line;
                        while ((line = reader.readLine()) != null) {
                            if (line.startsWith("data: ")) {
                                String data = line.substring(6); // 移除 "data: " 前缀
                                if (!"[DONE]".equals(data.trim())) {
                                    onMessage.accept(data);
                                }
                            }
                        }

                        onComplete.run();
                        LOG.info("SSE流式请求完成: " + url);

                    }
                } else {
                    onError.accept(new IOException("响应实体为空"));
                }

            } catch (Exception e) {
                LOG.error("SSE流式请求失败: " + endpoint, e);
                onError.accept(e);
            }
        });
    }

    /**
     * 关闭HTTP客户端
     */
    public void close() {
        try {
            if (httpClient != null) {
                httpClient.close();
            }
        } catch (IOException e) {
            LOG.error("关闭HTTP客户端失败", e);
        }
    }

    /**
     * API响应封装类
     */
    public static class ApiResponse {
        private final int statusCode;
        private final String body;
        private final boolean success;

        public ApiResponse(int statusCode, String body, boolean success) {
            this.statusCode = statusCode;
            this.body = body;
            this.success = success;
        }

        public int getStatusCode() {
            return statusCode;
        }

        public String getBody() {
            return body;
        }

        public boolean isSuccess() {
            return success;
        }

        @Override
        public String toString() {
            return String.format("ApiResponse{statusCode=%d, success=%s, bodyLength=%d}",
                statusCode, success, body != null ? body.length() : 0);
        }
    }
}
