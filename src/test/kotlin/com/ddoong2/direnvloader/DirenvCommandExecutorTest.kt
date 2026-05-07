package com.ddoong2.direnvloader

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DirenvCommandExecutorTest {

    // 유효한 JSON 출력을 Map으로 파싱하는지 확인
    @Test
    fun `parseExportJson_validJson_returnsMap`() {
        val json = """{"FOO":"bar","BAZ":"qux"}"""

        val result = DirenvCommandExecutor.parseExportJson(json)

        assertEquals(2, result.size)
        assertEquals("bar", result["FOO"])
        assertEquals("qux", result["BAZ"])
    }

    // 빈 JSON 객체는 빈 Map 반환
    @Test
    fun `parseExportJson_emptyJson_returnsEmptyMap`() {
        val result = DirenvCommandExecutor.parseExportJson("{}")

        assertTrue(result.isEmpty())
    }

    // null 또는 빈 문자열은 빈 Map 반환
    @Test
    fun `parseExportJson_nullOrEmpty_returnsEmptyMap`() {
        assertTrue(DirenvCommandExecutor.parseExportJson(null).isEmpty())
        assertTrue(DirenvCommandExecutor.parseExportJson("").isEmpty())
        assertTrue(DirenvCommandExecutor.parseExportJson("  ").isEmpty())
    }

    // 잘못된 JSON은 RuntimeException 발생
    @Test
    fun `parseExportJson_invalidJson_throwsException`() {
        assertThrows(RuntimeException::class.java) {
            DirenvCommandExecutor.parseExportJson("not json")
        }
    }
}
