package com.github.direnvloader;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.CapturingProcessHandler;
import com.intellij.execution.process.ProcessOutput;

import java.io.File;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

// direnv CLI 실행기
public final class DirenvCommandExecutor {

    private static final int TIMEOUT_MS = 5_000;
    private static final Gson GSON = new Gson();
    private static final Type MAP_TYPE = new TypeToken<Map<String, String>>() {}.getType();

    private DirenvCommandExecutor() {
    }

    // direnv export json 결과 JSON을 Map으로 파싱
    static Map<String, String> parseExportJson(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            Map<String, String> result = GSON.fromJson(json, MAP_TYPE);
            return result != null ? result : Collections.emptyMap();
        } catch (JsonSyntaxException e) {
            throw new RuntimeException("direnv JSON 파싱 실패: " + e.getMessage(), e);
        }
    }

    // direnv export json 실행하여 환경변수 Map 반환
    public static Map<String, String> exportJson(File workDir) throws DirenvException {
        ProcessOutput output = execute("export", "json", workDir);
        if (output.getExitCode() != 0) {
            String stderr = output.getStderr().trim();
            if (stderr.contains("is blocked")) {
                throw new DirenvBlockedException(stderr);
            }
            throw new DirenvException("direnv export 실패 (exit code " + output.getExitCode() + "): " + stderr);
        }
        String stdout = output.getStdout().trim();
        return parseExportJson(stdout);
    }

    // direnv allow 실행
    public static void allow(File workDir) throws DirenvException {
        ProcessOutput output = execute("allow", null, workDir);
        if (output.getExitCode() != 0) {
            throw new DirenvException("direnv allow 실패: " + output.getStderr().trim());
        }
    }

    // direnv 설치 여부 확인
    public static boolean isDirenvInstalled() {
        try {
            GeneralCommandLine cmd = new GeneralCommandLine("direnv", "version");
            cmd.setCharset(StandardCharsets.UTF_8);
            CapturingProcessHandler handler = new CapturingProcessHandler(cmd);
            ProcessOutput output = handler.runProcess(TIMEOUT_MS);
            return output.getExitCode() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    // direnv 명령 실행 공통 메서드
    private static ProcessOutput execute(String command, String subCommand, File workDir)
            throws DirenvException {
        try {
            GeneralCommandLine cmd = subCommand != null
                    ? new GeneralCommandLine("direnv", command, subCommand)
                    : new GeneralCommandLine("direnv", command);
            cmd.setCharset(StandardCharsets.UTF_8);
            cmd.setWorkDirectory(workDir);
            CapturingProcessHandler handler = new CapturingProcessHandler(cmd);
            ProcessOutput output = handler.runProcess(TIMEOUT_MS);
            if (output.isTimeout()) {
                throw new DirenvException("direnv 명령이 타임아웃되었습니다 (" + TIMEOUT_MS + "ms)");
            }
            return output;
        } catch (DirenvException e) {
            throw e;
        } catch (Exception e) {
            throw new DirenvException("direnv 실행 실패: " + e.getMessage(), e);
        }
    }
}
