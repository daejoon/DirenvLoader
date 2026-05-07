package com.ddoong2.direnvloader

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.execution.process.ProcessOutput
import com.intellij.openapi.application.ApplicationManager
import java.io.File
import java.lang.reflect.Type
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

// direnv CLI 실행기
object DirenvCommandExecutor {

    private const val TIMEOUT_MS = 5_000
    private val GSON = Gson()
    private val MAP_TYPE: Type = object : TypeToken<Map<String, String>>() {}.type

    // direnv export json 결과 JSON을 Map으로 파싱
    @JvmStatic
    internal fun parseExportJson(json: String?): Map<String, String> {
        if (json.isNullOrBlank()) {
            return emptyMap()
        }
        return try {
            GSON.fromJson<Map<String, String>?>(json, MAP_TYPE) ?: emptyMap()
        } catch (e: JsonSyntaxException) {
            throw RuntimeException("Failed to parse direnv JSON: ${e.message}", e)
        }
    }

    // direnv export json 실행하여 환경변수 Map 반환
    @JvmStatic
    @Throws(DirenvException::class)
    fun exportJson(workDir: File?): Map<String, String> {
        val output = execute("export", "json", workDir)
        if (output.exitCode != 0) {
            val stderr = output.stderr.trim()
            if (stderr.contains("is blocked")) {
                throw DirenvBlockedException(stderr)
            }
            throw DirenvException("direnv export failed (exit code ${output.exitCode}): $stderr")
        }
        return parseExportJson(output.stdout.trim())
    }

    // direnv allow 실행
    @JvmStatic
    @Throws(DirenvException::class)
    fun allow(workDir: File?) {
        val output = execute("allow", null, workDir)
        if (output.exitCode != 0) {
            throw DirenvException("direnv allow failed: ${output.stderr.trim()}")
        }
    }

    // direnv 설치 여부 확인
    @JvmStatic
    fun isDirenvInstalled(): Boolean = try {
        execute("version", null, null).exitCode == 0
    } catch (_: DirenvException) {
        false
    }

    // direnv 명령 실행 공통 메서드 (EDT 또는 ReadAction 내부에서 호출되면 자동으로 백그라운드 스레드에서 실행)
    @Throws(DirenvException::class)
    private fun execute(command: String, subCommand: String?, workDir: File?): ProcessOutput {
        try {
            val cmd = if (subCommand != null) {
                GeneralCommandLine("direnv", command, subCommand)
            } else {
                GeneralCommandLine("direnv", command)
            }
            cmd.charset = StandardCharsets.UTF_8
            if (workDir != null) {
                cmd.setWorkDirectory(workDir)
            }
            val handler = CapturingProcessHandler(cmd)

            // EDT 또는 ReadAction 내부에서는 프로세스 실행이 금지되므로 별도 스레드에서 실행
            val app = ApplicationManager.getApplication()
            val output: ProcessOutput? = if (app.isDispatchThread || app.isReadAccessAllowed) {
                val future = app.executeOnPooledThread<ProcessOutput> { handler.runProcess(TIMEOUT_MS) }
                future.get(TIMEOUT_MS + 1_000L, TimeUnit.MILLISECONDS)
            } else {
                handler.runProcess(TIMEOUT_MS)
            }

            if (output == null) {
                throw DirenvException("direnv command was cancelled")
            }
            if (output.isTimeout) {
                throw DirenvException("direnv command timed out (${TIMEOUT_MS}ms)")
            }
            return output
        } catch (e: DirenvException) {
            throw e
        } catch (e: Exception) {
            if (e is InterruptedException) {
                Thread.currentThread().interrupt()
            }
            throw DirenvException("Failed to execute direnv: ${e.message}", e)
        }
    }
}
