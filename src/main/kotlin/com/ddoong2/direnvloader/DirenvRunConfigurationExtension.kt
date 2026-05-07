package com.ddoong2.direnvloader

import com.intellij.execution.ExecutionException
import com.intellij.execution.RunConfigurationExtension
import com.intellij.execution.configurations.JavaParameters
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
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
        val workDir = resolveWorkDir(project)

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

    // 프로젝트 basePath에서 작업 디렉토리 결정
    private fun resolveWorkDir(project: Project): File {
        val basePath = project.basePath
        return if (basePath != null) File(basePath) else File(System.getProperty("user.dir"))
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
    }
}
