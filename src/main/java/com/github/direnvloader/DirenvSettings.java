package com.github.direnvloader;

import com.intellij.openapi.util.Key;

// Run Configuration별 direnv 설정 데이터
public class DirenvSettings {

    static final Key<DirenvSettings> KEY = Key.create("direnv.settings");

    private boolean enabled;
    private boolean trust;

    public DirenvSettings(boolean enabled, boolean trust) {
        this.enabled = enabled;
        this.trust = trust;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isTrust() {
        return trust;
    }

    public void setTrust(boolean trust) {
        this.trust = trust;
    }
}
