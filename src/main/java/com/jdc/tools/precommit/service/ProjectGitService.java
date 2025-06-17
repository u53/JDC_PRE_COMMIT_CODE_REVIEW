package com.jdc.tools.precommit.service;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryManager;

import java.util.Collection;

/**
 * 项目Git服务
 * 负责项目级别的Git操作和状态管理
 */
@Service(Service.Level.PROJECT)
public final class ProjectGitService {
    
    private static final Logger LOG = Logger.getInstance(ProjectGitService.class);
    private final Project project;
    
    public ProjectGitService(Project project) {
        this.project = project;
    }
    
    public static ProjectGitService getInstance(Project project) {
        return project.getService(ProjectGitService.class);
    }
    
    /**
     * 检查项目是否为Git仓库
     */
    public boolean isGitRepository() {
        try {
            GitRepositoryManager repositoryManager = GitRepositoryManager.getInstance(project);
            Collection<GitRepository> repositories = repositoryManager.getRepositories();
            return !repositories.isEmpty();
        } catch (Exception e) {
            LOG.error("检查Git仓库状态失败", e);
            return false;
        }
    }
    
    /**
     * 获取Git仓库信息
     */
    public String getRepositoryInfo() {
        try {
            if (!isGitRepository()) {
                return "当前项目不是Git仓库";
            }
            
            GitRepositoryManager repositoryManager = GitRepositoryManager.getInstance(project);
            Collection<GitRepository> repositories = repositoryManager.getRepositories();
            
            if (repositories.isEmpty()) {
                return "未找到Git仓库";
            }
            
            GitRepository repository = repositories.iterator().next();
            String currentBranch = repository.getCurrentBranchName();
            String rootPath = repository.getRoot().getPath();
            
            return String.format("仓库路径: %s\n当前分支: %s", rootPath, currentBranch);
            
        } catch (Exception e) {
            LOG.error("获取Git仓库信息失败", e);
            return "获取仓库信息失败: " + e.getMessage();
        }
    }
    
    /**
     * 获取当前分支名称
     */
    public String getCurrentBranch() {
        try {
            if (!isGitRepository()) {
                return null;
            }
            
            GitRepositoryManager repositoryManager = GitRepositoryManager.getInstance(project);
            Collection<GitRepository> repositories = repositoryManager.getRepositories();
            
            if (!repositories.isEmpty()) {
                GitRepository repository = repositories.iterator().next();
                return repository.getCurrentBranchName();
            }
            
            return null;
            
        } catch (Exception e) {
            LOG.error("获取当前分支失败", e);
            return null;
        }
    }
    
    /**
     * 获取项目名称
     */
    public String getProjectName() {
        return project.getName();
    }
    
    /**
     * 获取项目路径
     */
    public String getProjectPath() {
        return project.getBasePath();
    }
}
