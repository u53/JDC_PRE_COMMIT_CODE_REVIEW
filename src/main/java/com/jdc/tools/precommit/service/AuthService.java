package com.jdc.tools.precommit.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.credentialStore.CredentialAttributesKt;
import com.intellij.credentialStore.Credentials;
import com.intellij.ide.passwordSafe.PasswordSafe;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 认证服务
 * 负责用户登录、Token管理和认证状态维护
 */
@Service
public final class AuthService {

    private static final Logger LOG = Logger.getInstance(AuthService.class);
    private static final String CREDENTIAL_KEY = "JDC_PRE_COMMIT_REVIEW";
    private static final String TOKEN_KEY = "auth_token";
    private static final String USER_INFO_KEY = "user_info";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private String currentToken;
    private Map<String, Object> currentUserInfo;

    public static AuthService getInstance() {
        return ApplicationManager.getApplication().getService(AuthService.class);
    }

    /**
     * 用户登录
     */
    public CompletableFuture<Boolean> login(String username, String password) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                LOG.info("开始用户登录: " + username);

                ApiClient apiClient = ApiClient.getInstance();

                // 构建登录请求
                Map<String, String> loginRequest = Map.of(
                    "username", username,
                    "password", password
                );

                // 发送登录请求（登录接口不需要重试机制）
                ApiClient.ApiResponse response = apiClient.post("/auth/login", loginRequest).get();

                if (response.isSuccess()) {
                    Map<String, Object> result = objectMapper.readValue(response.getBody(), Map.class);
                    Object codeObj = result.get("code");
                    Integer code = null;
                    if (codeObj instanceof Number) {
                        code = ((Number) codeObj).intValue();
                    }

                    if (code != null && code == 200) {
                        Map<String, Object> data = (Map<String, Object>) result.get("data");
                        String token = (String) data.get("token");
                        Map<String, Object> userInfo = (Map<String, Object>) data.get("userInfo");

                        // 保存认证信息
                        saveCredentials(token, userInfo);

                        this.currentToken = token;
                        this.currentUserInfo = userInfo;

                        LOG.info("用户登录成功: " + username);
                        return true;
                    } else {
                        String message = (String) result.get("message");
                        LOG.warn("登录失败: " + message);
                        return false;
                    }
                } else {
                    LOG.warn("登录请求失败，状态码: " + response.getStatusCode());
                    return false;
                }

            } catch (Exception e) {
                LOG.error("登录过程中发生错误", e);
                return false;
            }
        });
    }

    /**
     * 使用OAuth授权登录
     */
    public CompletableFuture<Boolean> loginWithOAuth() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                LOG.info("开始OAuth授权登录");

                OAuthService oauthService = OAuthService.getInstance();
                OAuthService.AuthResult authResult = oauthService.startOAuthFlow().get();

                if (authResult.isSuccess()) {
                    String token = authResult.getToken();
                    Map<String, Object> userInfo = authResult.getUserInfo();

                    // 保存认证信息
                    saveCredentials(token, userInfo);

                    this.currentToken = token;
                    this.currentUserInfo = userInfo;

                    LOG.info("OAuth授权登录成功");
                    return true;
                } else {
                    LOG.warn("OAuth授权失败: " + authResult.getMessage());
                    return false;
                }

            } catch (Exception e) {
                LOG.error("OAuth授权登录异常", e);
                return false;
            }
        });
    }

    /**
     * 自动登录（使用保存的凭据）
     */
    public CompletableFuture<Boolean> autoLogin() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Credentials credentials = loadCredentials();
                if (credentials == null) {
                    return false;
                }

                String token = credentials.getPasswordAsString();
                if (token == null || token.trim().isEmpty()) {
                    return false;
                }

                // 验证Token有效性
                if (validateToken(token)) {
                    this.currentToken = token;
                    // 加载保存的用户信息
                    loadSavedUserInfo();
                    // 如果没有保存的用户信息，从服务器获取
                    if (this.currentUserInfo == null) {
                        loadUserInfo();
                    }
                    LOG.info("自动登录成功");
                    return true;
                } else {
                    // Token无效，清除保存的凭据
                    clearCredentials();
                    return false;
                }

            } catch (Exception e) {
                LOG.error("自动登录失败", e);
                return false;
            }
        });
    }

    /**
     * 用户登出
     */
    public CompletableFuture<Boolean> logout() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 如果有token，先调用后端退出接口
                if (currentToken != null) {
                    LOG.info("正在调用后端退出接口...");
                    ApiClient apiClient = ApiClient.getInstance();
                    ApiClient.ApiResponse response = apiClient.post("/auth/logout", null, currentToken).get();

                    if (response.isSuccess()) {
                        LOG.info("成功调用退出登录接口，token已失效");
                    } else {
                        LOG.warn("调用退出登录接口失败: " + response.getBody());
                        // 即使API调用失败，也要清除本地存储
                    }
                } else {
                    LOG.info("没有有效token，跳过后端退出接口调用");
                }
            } catch (Exception e) {
                LOG.error("调用退出登录接口失败", e);
                // 即使API调用失败，也要清除本地存储
            } finally {
                // 清除本地状态和凭据
                this.currentToken = null;
                this.currentUserInfo = null;
                clearCredentials();
                LOG.info("本地登录状态已清除");
            }

            return true;
        });
    }

    /**
     * 退出所有设备
     */
    public CompletableFuture<Boolean> logoutAll() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 如果有token，先调用后端退出所有设备接口
                if (currentToken != null) {
                    LOG.info("正在调用后端退出所有设备接口...");
                    ApiClient apiClient = ApiClient.getInstance();
                    ApiClient.ApiResponse response = apiClient.post("/auth/logout-all", null, currentToken).get();

                    if (response.isSuccess()) {
                        LOG.info("成功退出所有设备，所有token已失效");
                    } else {
                        LOG.warn("调用退出所有设备接口失败: " + response.getBody());
                        // 即使API调用失败，也要清除本地存储
                    }
                } else {
                    LOG.info("没有有效token，跳过后端退出所有设备接口调用");
                }
            } catch (Exception e) {
                LOG.error("调用退出所有设备接口失败", e);
                // 即使API调用失败，也要清除本地存储
            } finally {
                // 清除本地状态和凭据
                this.currentToken = null;
                this.currentUserInfo = null;
                clearCredentials();
                LOG.info("已退出所有设备，本地状态已清除");
            }

            return true;
        });
    }

    /**
     * 同步版本的登出（保持向后兼容）
     */
    public void logoutSync() {
        try {
            logout().get();
        } catch (Exception e) {
            LOG.error("同步登出失败", e);
        }
    }

    /**
     * 检查是否已认证
     */
    public boolean isAuthenticated() {
        return currentToken != null && !currentToken.trim().isEmpty();
    }

    /**
     * 获取当前Token
     */
    public String getCurrentToken() {
        return currentToken;
    }

    /**
     * 获取当前用户信息
     */
    public Map<String, Object> getCurrentUserInfo() {
        return currentUserInfo;
    }

    /**
     * 验证Token有效性
     */
    private boolean validateToken(String token) {
        try {
            ApiClient apiClient = ApiClient.getInstance();
            ApiClient.ApiResponse response = apiClient.get("/user/status", token).get();

            if (response.isSuccess()) {
                Map<String, Object> result = objectMapper.readValue(response.getBody(), Map.class);
                Object codeObj = result.get("code");
                if (codeObj instanceof Number) {
                    Integer code = ((Number) codeObj).intValue();
                    return code != null && code == 200;
                }
            }
            return false;

        } catch (Exception e) {
            LOG.warn("Token验证失败", e);
            return false;
        }
    }

    /**
     * 加载用户信息
     */
    private void loadUserInfo() {
        try {
            ApiClient apiClient = ApiClient.getInstance();
            // 获取用户信息使用带重试的方法
            ApiClient.ApiResponse response = apiClient.getWithRetry("/user/info").get();

            if (response.isSuccess()) {
                Map<String, Object> result = objectMapper.readValue(response.getBody(), Map.class);
                Object codeObj = result.get("code");
                if (codeObj instanceof Number) {
                    Integer code = ((Number) codeObj).intValue();
                    if (code != null && code == 200) {
                        this.currentUserInfo = (Map<String, Object>) result.get("data");
                    }
                }
            }

        } catch (Exception e) {
            LOG.warn("加载用户信息失败", e);
        }
    }

    /**
     * 保存认证凭据
     */
    private void saveCredentials(String token, Map<String, Object> userInfo) {
        try {
            CredentialAttributes attributes = createCredentialAttributes();
            Credentials credentials = new Credentials("token", token);
            PasswordSafe.getInstance().set(attributes, credentials);

            // 保存用户信息到应用程序设置中
            PropertiesComponent properties = PropertiesComponent.getInstance();
            if (userInfo != null) {
                String userInfoJson = objectMapper.writeValueAsString(userInfo);
                properties.setValue(USER_INFO_KEY, userInfoJson);
                LOG.info("用户信息已保存");
            }

        } catch (Exception e) {
            LOG.error("保存认证凭据失败", e);
        }
    }

    /**
     * 加载认证凭据
     */
    private Credentials loadCredentials() {
        try {
            CredentialAttributes attributes = createCredentialAttributes();
            return PasswordSafe.getInstance().get(attributes);
        } catch (Exception e) {
            LOG.error("加载认证凭据失败", e);
            return null;
        }
    }

    /**
     * 加载保存的用户信息
     */
    private void loadSavedUserInfo() {
        try {
            PropertiesComponent properties = PropertiesComponent.getInstance();
            String userInfoJson = properties.getValue(USER_INFO_KEY);
            if (userInfoJson != null && !userInfoJson.trim().isEmpty()) {
                this.currentUserInfo = objectMapper.readValue(userInfoJson, Map.class);
                LOG.info("已加载保存的用户信息");
            }
        } catch (Exception e) {
            LOG.warn("加载保存的用户信息失败", e);
        }
    }

    /**
     * 清除认证凭据
     */
    public void clearCredentials() {
        try {
            CredentialAttributes attributes = createCredentialAttributes();
            PasswordSafe.getInstance().set(attributes, null);

            // 清除保存的用户信息
            PropertiesComponent properties = PropertiesComponent.getInstance();
            properties.unsetValue(USER_INFO_KEY);

        } catch (Exception e) {
            LOG.error("清除认证凭据失败", e);
        }
    }

    /**
     * 处理认证失败，尝试自动重新登录
     */
    public CompletableFuture<Boolean> handleAuthFailure() {
        return CompletableFuture.supplyAsync(() -> {
            LOG.warn("检测到认证失败，尝试自动重新登录");

            // 清除当前无效的token
            this.currentToken = null;
            this.currentUserInfo = null;

            // 尝试使用OAuth重新登录
            try {
                return loginWithOAuth().get();
            } catch (Exception e) {
                LOG.error("自动重新登录失败", e);

                // 如果OAuth登录失败，清除所有凭据
                clearCredentials();
                return false;
            }
        });
    }

    /**
     * 创建凭据属性
     */
    private CredentialAttributes createCredentialAttributes() {
        return new CredentialAttributes(
            CredentialAttributesKt.generateServiceName("JDC Pre-Commit Review", CREDENTIAL_KEY)
        );
    }
}
