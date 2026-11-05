package com.ddoong2.direnvloader

import com.intellij.execution.ExecutionException
import com.intellij.execution.RunConfigurationExtension
import com.intellij.execution.configurations.JavaParameters
import com.intellij.execution.configurations.ModuleBasedConfiguration
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.util.Computable
import org.jdom.Element
import java.io.File

// Run Configuration에 direnv 환경변수 주입 기능을 추가하는 Extension
class DirenvRunConfigurationExtension : RunConfigurationExtension() {

    override fun getSerializationId(): String = SERIALIZATION_ID

    override fun readExternal(runConfiguration: RunConfigurationBase<*>, element: Element) {
        val enabled = element.getAttributeValue(ATTR_ENABLED, "false").toBoolean()
        val trust = element.getAttributeValue(ATTR_TRUST, "false").toBoolean()
        runConfiguration.putCopyableUserData(DirenvSettings.KEY, DirenvSettings(enabled, trust))
    }

    override fun writeExternal(runConfiguration: RunConfigurationBase<*>, element: Element) {
        val settings = runConfiguration.getCopyableUserData(DirenvSettings.KEY) ?: return
        element.setAttribute(ATTR_ENABLED, settings.enabled.toString())
        element.setAttribute(ATTR_TRUST, settings.trust.toString())
    }

    override fun getEditorTitle(): String = "Direnv"

    override fun isApplicableFor(configuration: RunConfigurationBase<*>): Boolean = true

    override fun isEnabledFor(
        applicableConfiguration: RunConfigurationBase<*>,
        runnerSettings: RunnerSettings?,
    ): Boolean {
        val settings = applicableConfiguration.getCopyableUserData(DirenvSettings.KEY)
        return settings?.enabled == true
    }

    override fun <T : RunConfigurationBase<*>> createEditor(configuration: T): SettingsEditor<T> =
        DirenvSettingsEditor()

    // direnv 환경변수를 Java 실행 파라미터에 주입
    @Throws(ExecutionException::class)
    override fun <T : RunConfigurationBase<*>> updateJavaParameters(
        configuration: T,
        params: JavaParameters,
        runnerSettings: RunnerSettings?,
    ) {
        val settings = configuration.getCopyableUserData(DirenvSettings.KEY)
        if (settings == null || !settings.enabled) {
            return
        }

        val project = configuration.project
        val workDir = resolveWorkDir(params.workingDirectory, project.basePath, moduleContentRoots(configuration))

        // direnv 설치 확인
        if (!DirenvCommandExecutor.isDirenvInstalled()) {
            DirenvNotifier.notifyNotInstalled(project)
            throw ExecutionException("direnv is not installed.")
        }

        try {
            // Trust 옵션 활성화 시 direnv allow 실행
            if (settings.trust) {
                DirenvCommandExecutor.allow(workDir)
            }

            // direnv export json으로 환경변수 로드
            val direnvEnv = DirenvCommandExecutor.exportJson(workDir)
            val merged = mergeEnvironment(direnvEnv, params.env)

            // 병합된 환경변수 적용
            params.env = merged
            DirenvNotifier.notifyLoaded(project, direnvEnv.size)
        } catch (e: DirenvBlockedException) {
            DirenvNotifier.notifyBlocked(project)
            throw ExecutionException(e.message)
        } catch (e: DirenvException) {
            DirenvNotifier.notifyError(project, e.message ?: "")
            throw ExecutionException(e.message)
        }
    }

    // RC에 지정된 모듈의 content root 경로 (모듈 미지정·dispose 시 빈 목록)
    private fun moduleContentRoots(configuration: RunConfigurationBase<*>): List<String> {
        val module = (configuration as? ModuleBasedConfiguration<*, *>)?.configurationModule?.module ?: return emptyList()
        // ReadAction.compute는 2026.1부터 deprecated이고 대체 API(computeBlocking)는 251에 없어, 두 버전 모두 있는 API를 쓴다
        return ApplicationManager.getApplication().runReadAction(Computable {
            if (module.isDisposed) emptyList() else ModuleRootManager.getInstance(module).contentRoots.map { it.path }
        })
    }

    companion object {
        private const val SERIALIZATION_ID = "com.ddoong2.direnvloader"
        private const val ATTR_ENABLED = "direnv-enabled"
        private const val ATTR_TRUST = "direnv-trust"

        // 환경변수 병합: direnv 값을 기본으로, RC 설정값이 우선
        @JvmStatic
        internal fun mergeEnvironment(
            direnvEnv: Map<String, String>,
            rcEnv: Map<String, String>,
        ): Map<String, String> = LinkedHashMap(direnvEnv).apply { putAll(rcEnv) }

        // direnv 작업 디렉토리 결정: RC working directory → project basePath → user.dir
        // Multi-Project Workspace에서는 basePath가 workspace 폴더라 하위 프로젝트 .envrc를 찾지 못하므로 RC 값을 우선함
        @JvmStatic
        internal fun resolveWorkDir(
            workingDirectory: String?,
            basePath: String?,
            moduleContentRoots: List<String> = emptyList(),
        ): File {
            val baseDir = basePath?.let(::File)
            var resolved = baseDir
            if (workingDirectory != null) {
                // 상대 경로는 IDE 프로세스 cwd가 아닌 프로젝트 루트 기준으로 해석
                val raw = File(workingDirectory)
                val dir = if (!raw.isAbsolute && basePath != null) File(basePath, workingDirectory) else raw
                if (dir.isDirectory) {
                    resolved = dir
                }
            }
            // workspace 플러그인은 Gradle source set 모듈(xxx.main)의 하위 프로젝트를 찾지 못해 working directory를
            // workspace 폴더(basePath)로 채우므로, 이때는 RC 모듈 위치에서 실행해 direnv가 상위 .envrc를 찾게 함
            if (baseDir != null && resolved != null && resolved.absoluteFile.normalize() == baseDir.absoluteFile.normalize()) {
                moduleContentRoots.map(::File).firstOrNull { it.isDirectory }?.let { return it }
            }
            return resolved ?: File(System.getProperty("user.dir"))
        }
    }
}
