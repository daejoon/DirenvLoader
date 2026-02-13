package com.github.direnvloader;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.configurations.RunConfigurationBase;
import com.intellij.execution.configurations.RunnerSettings;
import com.intellij.execution.configuration.RunConfigurationExtensionBase;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

// Run Configuration에 direnv 환경변수 주입 기능을 추가하는 Extension
public class DirenvRunConfigurationExtension
        extends RunConfigurationExtensionBase<RunConfigurationBase<?>> {

    private static final String SERIALIZATION_ID = "com.github.direnvloader";
    private static final String ATTR_ENABLED = "direnv-enabled";
    private static final String ATTR_TRUST = "direnv-trust";

    @NotNull
    @Override
    protected String getSerializationId() {
        return SERIALIZATION_ID;
    }

    @Override
    protected void readExternal(@NotNull RunConfigurationBase<?> runConfiguration,
                                @NotNull Element element) {
        boolean enabled = Boolean.parseBoolean(element.getAttributeValue(ATTR_ENABLED, "false"));
        boolean trust = Boolean.parseBoolean(element.getAttributeValue(ATTR_TRUST, "false"));
        DirenvSettings settings = new DirenvSettings(enabled, trust);
        runConfiguration.putCopyableUserData(DirenvSettings.KEY, settings);
    }

    @Override
    protected void writeExternal(@NotNull RunConfigurationBase<?> runConfiguration,
                                 @NotNull Element element) {
        DirenvSettings settings = runConfiguration.getCopyableUserData(DirenvSettings.KEY);
        if (settings != null) {
            element.setAttribute(ATTR_ENABLED, String.valueOf(settings.isEnabled()));
            element.setAttribute(ATTR_TRUST, String.valueOf(settings.isTrust()));
        }
    }

    @Nullable
    @Override
    protected String getEditorTitle() {
        return "Direnv";
    }

    @Override
    public boolean isApplicableFor(@NotNull RunConfigurationBase<?> configuration) {
        return true;
    }

    @Override
    public boolean isEnabledFor(@NotNull RunConfigurationBase<?> applicableConfiguration,
                                @Nullable RunnerSettings runnerSettings) {
        DirenvSettings settings = applicableConfiguration.getCopyableUserData(DirenvSettings.KEY);
        return settings != null && settings.isEnabled();
    }

    @Nullable
    @Override
    protected <T extends RunConfigurationBase<?>> SettingsEditor<T> createEditor(@NotNull T configuration) {
        return new DirenvSettingsEditor<>();
    }

    @Override
    protected void patchCommandLine(@NotNull RunConfigurationBase<?> configuration,
                                    @Nullable RunnerSettings runnerSettings,
                                    @NotNull GeneralCommandLine cmdLine,
                                    @NotNull String runnerId) throws ExecutionException {
        DirenvSettings settings = configuration.getCopyableUserData(DirenvSettings.KEY);
        if (settings == null || !settings.isEnabled()) {
            return;
        }

        Project project = configuration.getProject();
        File workDir = resolveWorkDir(project);

        // direnv 설치 확인
        if (!DirenvCommandExecutor.isDirenvInstalled()) {
            DirenvNotifier.notifyNotInstalled(project);
            throw new ExecutionException("direnv가 설치되어 있지 않습니다.");
        }

        try {
            // Trust 옵션 활성화 시 direnv allow 실행
            if (settings.isTrust()) {
                DirenvCommandExecutor.allow(workDir);
            }

            // direnv export json으로 환경변수 로드
            Map<String, String> direnvEnv = DirenvCommandExecutor.exportJson(workDir);
            Map<String, String> rcEnv = cmdLine.getEnvironment();
            Map<String, String> merged = mergeEnvironment(direnvEnv, rcEnv);

            // 병합된 환경변수 적용
            cmdLine.getEnvironment().clear();
            cmdLine.getEnvironment().putAll(merged);

        } catch (DirenvBlockedException e) {
            DirenvNotifier.notifyBlocked(project);
            throw new ExecutionException(e.getMessage());
        } catch (DirenvException e) {
            DirenvNotifier.notifyError(project, e.getMessage());
            throw new ExecutionException(e.getMessage());
        }
    }

    // 환경변수 병합: direnv 값을 기본으로, RC 설정값이 우선
    static Map<String, String> mergeEnvironment(Map<String, String> direnvEnv,
                                                Map<String, String> rcEnv) {
        Map<String, String> merged = new LinkedHashMap<>(direnvEnv);
        merged.putAll(rcEnv);
        return merged;
    }

    // 프로젝트 basePath에서 작업 디렉토리 결정
    private File resolveWorkDir(Project project) {
        String basePath = project.getBasePath();
        if (basePath != null) {
            return new File(basePath);
        }
        return new File(System.getProperty("user.dir"));
    }
}
