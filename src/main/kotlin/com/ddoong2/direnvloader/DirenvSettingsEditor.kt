package com.ddoong2.direnvloader

import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.openapi.options.SettingsEditor
import javax.swing.BoxLayout
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JPanel

// Run Configuration 설정 패널 UI (체크박스 2개)
class DirenvSettingsEditor<T : RunConfigurationBase<*>> : SettingsEditor<T>() {

    private val enableDirenvCheckBox = JCheckBox("Enable Direnv")
    private val trustEnvrcCheckBox = JCheckBox("Trust .envrc (auto direnv allow)")
    private val panel = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        add(enableDirenvCheckBox)
        add(trustEnvrcCheckBox)
    }

    override fun resetEditorFrom(configuration: T) {
        val settings = configuration.getCopyableUserData(DirenvSettings.KEY)
        enableDirenvCheckBox.isSelected = settings?.enabled ?: false
        trustEnvrcCheckBox.isSelected = settings?.trust ?: false
    }

    override fun applyEditorTo(configuration: T) {
        val settings = DirenvSettings(
            enabled = enableDirenvCheckBox.isSelected,
            trust = trustEnvrcCheckBox.isSelected,
        )
        configuration.putCopyableUserData(DirenvSettings.KEY, settings)
    }

    override fun createEditor(): JComponent = panel
}
