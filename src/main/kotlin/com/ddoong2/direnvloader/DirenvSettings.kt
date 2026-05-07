package com.ddoong2.direnvloader

import com.intellij.openapi.util.Key

// Run Configuration별 direnv 설정 데이터
data class DirenvSettings(
    val enabled: Boolean,
    val trust: Boolean,
) {
    companion object {
        // CopyableUserData 식별용 정적 키 (Java 측 호환을 위해 @JvmField로 노출)
        @JvmField
        val KEY: Key<DirenvSettings> = Key.create("direnv.settings")
    }
}
