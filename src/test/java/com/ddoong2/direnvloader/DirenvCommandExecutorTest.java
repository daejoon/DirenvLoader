package com.ddoong2.direnvloader;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DirenvCommandExecutorTest {

    // 유효한 JSON 출력을 Map으로 파싱하는지 확인
    @Test
    void parseExportJson_validJson_returnsMap() {
        String json = "{\"FOO\":\"bar\",\"BAZ\":\"qux\"}";

        Map<String, String> result = DirenvCommandExecutor.parseExportJson(json);

        assertEquals(2, result.size());
        assertEquals("bar", result.get("FOO"));
        assertEquals("qux", result.get("BAZ"));
    }

    // 빈 JSON 객체는 빈 Map 반환
    @Test
    void parseExportJson_emptyJson_returnsEmptyMap() {
        String json = "{}";

        Map<String, String> result = DirenvCommandExecutor.parseExportJson(json);

        assertTrue(result.isEmpty());
    }

    // null 또는 빈 문자열은 빈 Map 반환
    @Test
    void parseExportJson_nullOrEmpty_returnsEmptyMap() {
        assertTrue(DirenvCommandExecutor.parseExportJson(null).isEmpty());
        assertTrue(DirenvCommandExecutor.parseExportJson("").isEmpty());
        assertTrue(DirenvCommandExecutor.parseExportJson("  ").isEmpty());
    }

    // 잘못된 JSON은 RuntimeException 발생
    @Test
    void parseExportJson_invalidJson_throwsException() {
        assertThrows(RuntimeException.class, () ->
                DirenvCommandExecutor.parseExportJson("not json"));
    }
}
