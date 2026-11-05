package com.ddoong2.direnvloader

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

class DirenvRunConfigurationExtensionTest {

    // RC working directory가 존재하는 디렉토리면 basePath 대신 그 경로 사용 (workspace 하위 프로젝트)
    @Test
    fun resolveWorkDir_existingWorkingDirectory_returnsWorkingDirectory(@TempDir tempDir: Path) {
        val basePath = Files.createDirectories(tempDir.resolve("workspace"))
        val subProject = Files.createDirectories(tempDir.resolve("sub-project"))

        val result = DirenvRunConfigurationExtension.resolveWorkDir(subProject.toString(), basePath.toString())

        assertEquals(subProject.toFile(), result)
    }

    // working directory가 basePath(workspace 폴더)와 같으면 RC 모듈의 존재하는 첫 content root 사용
    @Test
    fun resolveWorkDir_workingDirectoryEqualsBasePath_returnsFirstExistingContentRoot(@TempDir tempDir: Path) {
        val workspace = Files.createDirectories(tempDir.resolve("workspace"))
        val missingRoot = tempDir.resolve("ice-core/build/generated").toString()
        val sourceRoot = Files.createDirectories(tempDir.resolve("ice-core/src/main"))

        val result = DirenvRunConfigurationExtension.resolveWorkDir(
            workspace.toString(),
            workspace.toString(),
            listOf(missingRoot, sourceRoot.toString()),
        )

        assertEquals(sourceRoot.toFile(), result)
    }

    // RC working directory가 상대 경로면 basePath 기준으로 해석
    @Test
    fun resolveWorkDir_relativeWorkingDirectory_resolvesAgainstBasePath(@TempDir tempDir: Path) {
        val subModule = Files.createDirectories(tempDir.resolve("direnv-relative-module"))

        val result = DirenvRunConfigurationExtension.resolveWorkDir("direnv-relative-module", tempDir.toString())

        assertEquals(subModule.toFile(), result)
    }

    // RC working directory가 null이면 basePath 사용
    @Test
    fun resolveWorkDir_nullWorkingDirectory_returnsBasePath(@TempDir tempDir: Path) {
        val result = DirenvRunConfigurationExtension.resolveWorkDir(null, tempDir.toString())

        assertEquals(tempDir.toFile(), result)
    }

    // RC working directory가 공백이면 basePath 사용
    @Test
    fun resolveWorkDir_blankWorkingDirectory_returnsBasePath(@TempDir tempDir: Path) {
        val result = DirenvRunConfigurationExtension.resolveWorkDir("  ", tempDir.toString())

        assertEquals(tempDir.toFile(), result)
    }

    // RC working directory가 존재하지 않는 경로면 basePath 사용
    @Test
    fun resolveWorkDir_missingWorkingDirectory_returnsBasePath(@TempDir tempDir: Path) {
        val missing = tempDir.resolve("missing").toString()

        val result = DirenvRunConfigurationExtension.resolveWorkDir(missing, tempDir.toString())

        assertEquals(tempDir.toFile(), result)
    }

    // RC working directory가 디렉토리가 아닌 파일이면 basePath 사용
    @Test
    fun resolveWorkDir_fileWorkingDirectory_returnsBasePath(@TempDir tempDir: Path) {
        val file = Files.createFile(tempDir.resolve("file.txt")).toString()

        val result = DirenvRunConfigurationExtension.resolveWorkDir(file, tempDir.toString())

        assertEquals(tempDir.toFile(), result)
    }

    // RC working directory와 basePath가 모두 없으면 user.dir 사용
    @Test
    fun resolveWorkDir_noWorkingDirectoryAndNoBasePath_returnsUserDir() {
        val result = DirenvRunConfigurationExtension.resolveWorkDir(null, null)

        assertEquals(File(System.getProperty("user.dir")), result)
    }

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
