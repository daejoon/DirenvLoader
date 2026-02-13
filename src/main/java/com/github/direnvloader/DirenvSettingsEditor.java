package com.github.direnvloader;

import com.intellij.execution.configurations.RunConfigurationBase;
import com.intellij.openapi.options.SettingsEditor;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

// Run Configuration 설정 패널 UI (체크박스 2개)
public class DirenvSettingsEditor<T extends RunConfigurationBase<?>> extends SettingsEditor<T> {

    private final JCheckBox enableDirenvCheckBox = new JCheckBox("Enable Direnv");
    private final JCheckBox trustEnvrcCheckBox = new JCheckBox("Trust .envrc (auto direnv allow)");
    private final JPanel panel = new JPanel();

    public DirenvSettingsEditor() {
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(enableDirenvCheckBox);
        panel.add(trustEnvrcCheckBox);
    }

    @Override
    protected void resetEditorFrom(@NotNull T configuration) {
        DirenvSettings settings = configuration.getCopyableUserData(DirenvSettings.KEY);
        if (settings != null) {
            enableDirenvCheckBox.setSelected(settings.isEnabled());
            trustEnvrcCheckBox.setSelected(settings.isTrust());
        } else {
            enableDirenvCheckBox.setSelected(false);
            trustEnvrcCheckBox.setSelected(false);
        }
    }

    @Override
    protected void applyEditorTo(@NotNull T configuration) {
        DirenvSettings settings = new DirenvSettings(
                enableDirenvCheckBox.isSelected(),
                trustEnvrcCheckBox.isSelected()
        );
        configuration.putCopyableUserData(DirenvSettings.KEY, settings);
    }

    @NotNull
    @Override
    protected JComponent createEditor() {
        return panel;
    }
}
