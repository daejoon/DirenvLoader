package com.ddoong2.direnvloader

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DirenvRunConfigurationExtensionTest {

    // direnv 값 위에 RC 설정값이 덮어쓰는지 확인
    @Test
    fun mergeEnvironment_rcOverridesDirenv() {
        val direnvEnv = mapOf("FOO" to "from_direnv", "BAR" to "from_direnv")
        val rcEnv = mapOf("FOO" to "from_rc")

        val result = DirenvRunConfigurationExtension.mergeEnvironment(direnvEnv, rcEnv)

        assertEquals("from_rc", result["FOO"])
        assertEquals("from_direnv", result["BAR"])
    }

    // direnv 환경변수가 빈 경우 RC 값만 유지
    @Test
    fun mergeEnvironment_emptyDirenv_returnsRcOnly() {
        val direnvEnv = emptyMap<String, String>()
        val rcEnv = mapOf("KEY" to "value")

        val result = DirenvRunConfigurationExtension.mergeEnvironment(direnvEnv, rcEnv)

        assertEquals(1, result.size)
        assertEquals("value", result["KEY"])
    }

    // RC 환경변수가 빈 경우 direnv 값만 유지
    @Test
    fun mergeEnvironment_emptyRc_returnsDirenvOnly() {
        val direnvEnv = mapOf("KEY" to "value")
        val rcEnv = emptyMap<String, String>()

        val result = DirenvRunConfigurationExtension.mergeEnvironment(direnvEnv, rcEnv)

        assertEquals(1, result.size)
        assertEquals("value", result["KEY"])
    }
}
