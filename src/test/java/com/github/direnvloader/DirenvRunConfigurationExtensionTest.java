package com.github.direnvloader;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DirenvRunConfigurationExtensionTest {

    // direnv 값 위에 RC 설정값이 덮어쓰는지 확인
    @Test
    void mergeEnvironment_rcOverridesDirenv() {
        Map<String, String> direnvEnv = new HashMap<>();
        direnvEnv.put("FOO", "from_direnv");
        direnvEnv.put("BAR", "from_direnv");

        Map<String, String> rcEnv = new HashMap<>();
        rcEnv.put("FOO", "from_rc");

        Map<String, String> result = DirenvRunConfigurationExtension.mergeEnvironment(direnvEnv, rcEnv);

        assertEquals("from_rc", result.get("FOO"));
        assertEquals("from_direnv", result.get("BAR"));
    }

    // direnv 환경변수가 빈 경우 RC 값만 유지
    @Test
    void mergeEnvironment_emptyDirenv_returnsRcOnly() {
        Map<String, String> direnvEnv = new HashMap<>();
        Map<String, String> rcEnv = new HashMap<>();
        rcEnv.put("KEY", "value");

        Map<String, String> result = DirenvRunConfigurationExtension.mergeEnvironment(direnvEnv, rcEnv);

        assertEquals(1, result.size());
        assertEquals("value", result.get("KEY"));
    }

    // RC 환경변수가 빈 경우 direnv 값만 유지
    @Test
    void mergeEnvironment_emptyRc_returnsDirenvOnly() {
        Map<String, String> direnvEnv = new HashMap<>();
        direnvEnv.put("KEY", "value");
        Map<String, String> rcEnv = new HashMap<>();

        Map<String, String> result = DirenvRunConfigurationExtension.mergeEnvironment(direnvEnv, rcEnv);

        assertEquals(1, result.size());
        assertEquals("value", result.get("KEY"));
    }
}
